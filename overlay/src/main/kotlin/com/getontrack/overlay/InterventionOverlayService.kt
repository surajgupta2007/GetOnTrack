package com.getontrack.overlay

import android.app.Service
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
 * Overlay service that displays blocking UI.
 * 
 * CRITICAL CONSTRAINTS:
 * - Does NOT own cooldown logic
 * - Only reacts to state updates
 * - Displays countdown timer
 * - Sends OverrideRequested event to engine
 * - Full-screen, non-dismissible during cooldown
 * 
 * Responsibilities:
 * - Show full-screen overlay when block triggered
 * - Display countdown timer
 * - Handle override button click
 * - Update UI based on state changes
 */
class InterventionOverlayService : Service(), SessionObserver {
    
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    @Volatile
    private var sessionEngine: SessionEngine? = null
    private var maxOverridesPerDay: Int = 5 // Default, updated when engine is set
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    
    private var countdownText: TextView? = null
    private var overrideButton: Button? = null
    private var messageText: TextView? = null
    
    /**
     * Set SessionEngine dependency.
     * Must be called before service receives any state updates.
     */
    fun setSessionEngine(engine: SessionEngine, maxOverrides: Int) {
        sessionEngine = engine
        maxOverridesPerDay = maxOverrides
        engine.addObserver(this)
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStateChanged(state: SessionState) {
        when (state) {
            is SessionState.BlockTriggered -> {
                // Block triggered but not yet in cooldown
                // This is a brief transition state
            }
            is SessionState.Cooldown -> showOverlay(state)
            is SessionState.Override -> showOverrideMode(state)
            else -> hideOverlay()
        }
    }
    
    /**
     * Show blocking overlay with countdown.
     */
    private fun showOverlay(state: SessionState.Cooldown) {
        if (overlayView == null) {
            createOverlayView()
        }
        
        messageText?.text = "Take a break from distractions"
        
        // Update override button based on remaining overrides
        val canOverride = state.overridesUsed < maxOverridesPerDay
        overrideButton?.isEnabled = canOverride
        overrideButton?.text = if (canOverride) {
            "Override (${state.overridesUsed} used today)"
        } else {
            "No overrides left"
        }
        
        startCountdownTimer(state.remainingMs)
    }
    
    /**
     * Show override mode UI.
     */
    private fun showOverrideMode(state: SessionState.Override) {
        if (overlayView == null) {
            createOverlayView()
        }
        
        messageText?.text = "Override active - Use wisely"
        overrideButton?.isEnabled = false
        overrideButton?.text = "Override used (${state.overridesUsed} total today)"
        
        startCountdownTimer(state.remainingMs)
    }
    
    /**
     * Create and add overlay view to window.
     */
    private fun createOverlayView() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_intervention, null)
        
        countdownText = overlayView?.findViewById(R.id.countdown_text)
        overrideButton = overlayView?.findViewById(R.id.override_button)
        messageText = overlayView?.findViewById(R.id.message_text)
        
        overrideButton?.setOnClickListener {
            sessionEngine?.handleEvent(SessionEvent.OverrideRequested)
        }
        
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        windowManager?.addView(overlayView, params)
    }
    
    /**
     * Start countdown timer display.
     */
    private fun startCountdownTimer(durationMs: Long) {
        timerJob?.cancel()
        
        timerJob = scope.launch {
            val endTime = System.currentTimeMillis() + durationMs
            while (isActive) {
                val remaining = endTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    break
                }
                
                val seconds = (remaining / 1000).toInt()
                countdownText?.text = formatTime(seconds)
                
                delay(100) // Update every 100ms for smooth display
            }
        }
    }
    
    /**
     * Format seconds to MM:SS.
     */
    private fun formatTime(totalSeconds: Int): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Hide and remove overlay.
     */
    private fun hideOverlay() {
        timerJob?.cancel()
        overlayView?.let { view ->
            windowManager?.removeView(view)
            overlayView = null
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        sessionEngine?.removeObserver(this)
        scope.cancel()
    }
}
