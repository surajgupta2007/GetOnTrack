package com.getontrack.engine

import com.getontrack.core.SessionState

/**
 * Observer interface for receiving session state updates.
 * 
 * Observers are notified whenever the session state changes.
 * Implementations should handle state changes on appropriate thread.
 */
interface SessionObserver {
    /**
     * Called when session state changes.
     * 
     * @param state The new session state.
     */
    fun onStateChanged(state: SessionState)
}
