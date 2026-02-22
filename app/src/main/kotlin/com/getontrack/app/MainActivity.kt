package com.getontrack.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.getontrack.core.SessionEvent
import com.getontrack.overlay.InterventionOverlayService

/**
 * Main activity for GetOnTrack.
 * 
 * RESPONSIBILITIES:
 * - Request necessary permissions
 * - Start/stop monitoring services
 * - Display basic status
 * - NO business logic
 * 
 * All business logic is delegated to appropriate modules.
 */
class MainActivity : AppCompatActivity() {
    
    private val application: GetOnTrackApplication
        get() = getApplication() as GetOnTrackApplication
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create a simple layout programmatically
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        
        val titleText = android.widget.TextView(this).apply {
            text = "GetOnTrack"
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }
        
        val startButton = android.widget.Button(this).apply {
            text = "Start Monitoring"
            setOnClickListener {
                startMonitoring()
            }
        }
        
        val stopButton = android.widget.Button(this).apply {
            text = "Stop Monitoring"
            setOnClickListener {
                stopMonitoring()
            }
        }
        
        val permissionsButton = android.widget.Button(this).apply {
            text = "Grant Overlay Permission"
            setOnClickListener {
                requestOverlayPermission()
            }
        }
        
        layout.addView(titleText)
        layout.addView(permissionsButton)
        layout.addView(startButton)
        layout.addView(stopButton)
        
        setContentView(layout)
    }
    
    /**
     * Start monitoring session.
     */
    private fun startMonitoring() {
        val engine = application.getSessionEngine()
        engine.handleEvent(SessionEvent.SessionStarted)
        
        // Start overlay service
        InterventionOverlayService.start(this)
    }
    
    /**
     * Stop monitoring session.
     */
    private fun stopMonitoring() {
        val engine = application.getSessionEngine()
        engine.handleEvent(SessionEvent.SessionStopped)
        
        // Stop overlay service
        InterventionOverlayService.stop(this)
    }
    
    /**
     * Request overlay permission (SYSTEM_ALERT_WINDOW).
     */
    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
