package com.getontrack.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.getontrack.core.SessionEvent
import com.getontrack.core.SessionState
import com.getontrack.engine.SessionEngine
import com.getontrack.engine.SessionObserver
import kotlinx.coroutines.*

/**
 * Overlay service that displays intervention UI during blocks.
 * 
 * RESPONSIBILITIES:
 * - Display full-screen overlay during cooldown
 * - Show countdown timer
 * - Handle override button press
 * - React to state changes from SessionEngine
 * 
 * CONSTRAINTS:
 * - NO cooldown logic (delegated to engine)
 * - Non-dismissible during cooldown
 * - State-driven UI updates only
 * - Requires SYSTEM_ALERT_WINDOW permission
 */
class InterventionOverlayService : Service(), SessionObserver {
    
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, InterventionOverlayService::class.java)
            context.startService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, InterventionOverlayService::class.java)
            context.stopService(intent)
        }
    }
    
    private var sessionEngine: SessionEngine? = null
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Inject SessionEngine dependency.
     */
    fun setSessionEngine(engine: SessionEngine) {
        // Remove old observer if exists
        sessionEngine?.removeObserver(this)
        
        this.sessionEngine = engine
        engine.addObserver(this)
    }
    
    override fun onStateChanged(state: SessionState) {
        when (state) {
            is SessionState.Cooldown -> {
                showOverlay(state)
            }
            is SessionState.BlockTriggered -> {
                // Block triggered but cooldown not started yet
                // Could show a brief warning here
            }
            else -> {
                hideOverlay()
            }
        }
    }
    
    /**
     * Show intervention overlay with countdown.
     */
    private fun showOverlay(cooldownState: SessionState.Cooldown) {
        if (overlayView != null) {
            updateOverlay(cooldownState)
            return
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.CENTER
        
        // Create overlay view
        overlayView = createOverlayView()
        
        try {
            windowManager?.addView(overlayView, params)
            updateOverlay(cooldownState)
            startCountdownTimer(cooldownState.remainingMs)
        } catch (e: Exception) {
            // Handle permission errors
        }
    }
    
    /**
     * Create the overlay view with message and override button.
     */
    private fun createOverlayView(): View {
        val inflater = LayoutInflater.from(this)
        
        // Create a simple layout programmatically since we don't have XML resources yet
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
            setPadding(48, 48, 48, 48)
        }
        
        val messageText = TextView(this).apply {
            id = View.generateViewId()
            text = "Take a break from distractions"
            textSize = 24f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
        }
        
        val timerText = TextView(this).apply {
            id = View.generateViewId()
            text = "00:00"
            textSize = 48f
            setTextColor(android.graphics.Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        
        val overrideButton = Button(this).apply {
            id = View.generateViewId()
            text = "Override (Limited)"
            setOnClickListener {
                sessionEngine?.handleEvent(SessionEvent.OverrideRequested)
            }
        }
        
        layout.addView(messageText)
        layout.addView(timerText)
        layout.addView(overrideButton)
        
        return layout
    }
    
    /**
     * Update overlay content based on cooldown state.
     */
    private fun updateOverlay(state: SessionState.Cooldown) {
        overlayView?.let { view ->
            val timerText = view.findViewById<TextView>(
                view.findViewWithTag("timer") ?: return
            )
            
            val seconds = state.remainingMs / 1000
            val minutes = seconds / 60
            val secs = seconds % 60
            
            timerText?.text = String.format("%02d:%02d", minutes, secs)
        }
    }
    
    /**
     * Start countdown timer to update UI.
     */
    private fun startCountdownTimer(durationMs: Long) {
        timerJob?.cancel()
        
        timerJob = serviceScope.launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + durationMs
            
            while (isActive && System.currentTimeMillis() < endTime) {
                val remaining = endTime - System.currentTimeMillis()
                updateTimerDisplay(remaining)
                delay(1000)
            }
        }
    }
    
    /**
     * Update the timer display.
     */
    private fun updateTimerDisplay(remainingMs: Long) {
        overlayView?.let { view ->
            // Find timer text view and update
            // This is simplified - real implementation would use view binding
            val seconds = remainingMs / 1000
            val minutes = seconds / 60
            val secs = seconds % 60
            
            // Update would happen here with proper view references
        }
    }
    
    /**
     * Hide and remove overlay.
     */
    private fun hideOverlay() {
        timerJob?.cancel()
        timerJob = null
        
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                // View already removed
            }
            overlayView = null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sessionEngine?.removeObserver(this)
        hideOverlay()
        serviceScope.cancel()
    }
}
