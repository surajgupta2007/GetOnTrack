package com.getontrack.system

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.getontrack.core.SessionEvent
import com.getontrack.engine.SessionEngine

/**
 * AccessibilityService adapter that monitors app switches.
 * 
 * RESPONSIBILITIES:
 * - Listen for window state changes
 * - Detect app switches
 * - Forward events to SessionEngine
 * - Delegate heavy operations to background threads
 * 
 * CONSTRAINTS:
 * - NO business logic in this layer
 * - Never block main thread
 * - Never perform heavy operations in callbacks
 */
class AccessibilityController : AccessibilityService() {
    
    private var sessionEngine: SessionEngine? = null
    
    /**
     * Inject SessionEngine dependency.
     * Should be called by app layer during initialization.
     */
    fun setSessionEngine(engine: SessionEngine) {
        this.sessionEngine = engine
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Only process window state changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handleWindowStateChanged(event)
        }
    }
    
    /**
     * Handle window state change event.
     * Detects app switches and notifies SessionEngine.
     */
    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        
        // Forward to engine - detection will be triggered separately
        // This just notifies that an app switch occurred
        // In a full implementation, this would check if the app is in a distraction list
        
        // Example: Start monitoring when user opens any app
        // Real implementation would have app filtering logic
    }
    
    override fun onInterrupt() {
        // Handle service interruption
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service is connected and ready
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sessionEngine = null
    }
}
