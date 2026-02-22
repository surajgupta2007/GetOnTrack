package com.getontrack.engine

import com.getontrack.core.SessionState

/**
 * Observer interface for session state changes.
 * Implementations receive state updates from SessionEngine.
 */
fun interface SessionObserver {
    /**
     * Called when session state changes.
     * @param state The new state
     */
    fun onStateChanged(state: SessionState)
}
