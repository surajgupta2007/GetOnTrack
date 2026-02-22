package com.getontrack.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.getontrack.core.SessionEvent
import com.getontrack.engine.SessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Accessibility service that monitors app switches.
 * 
 * CRITICAL CONSTRAINTS:
 * - NO business logic in this layer
 * - Delegate all detection to ContentDetector
 * - Never block main thread
 * - Never run heavy computation in callbacks
 * 
 * Responsibilities:
 * - Listen for window state changes
 * - Detect app switches
 * - Forward events to SessionEngine
 */
class AccessibilityController : AccessibilityService() {
    
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @Volatile
    private var sessionEngine: SessionEngine? = null
    
    /**
     * Inject SessionEngine dependency.
     * Must be called before service starts receiving events.
     */
    fun setSessionEngine(engine: SessionEngine) {
        sessionEngine = engine
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }
        
        // Process in background to avoid blocking accessibility thread
        scope.launch {
            handleWindowStateChanged(event)
        }
    }
    
    /**
     * Handle window state change event.
     * Runs in background coroutine.
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // Log app switch for monitoring
        // In production, could check against blacklist of distracting apps
        // For now, just notify engine that user is active
        
        // Note: Actual detection happens in ScreenCaptureService
        // This service just tracks app switches for context
    }
    
    override fun onInterrupt() {
        // Accessibility service interrupted
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup would happen here if needed
    }
}
