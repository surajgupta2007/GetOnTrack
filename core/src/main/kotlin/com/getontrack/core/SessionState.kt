package com.getontrack.core

/**
 * Represents all possible states in a blocking session lifecycle.
 * Pure data structure with no behavior.
 */
sealed class SessionState {
    /**
     * No active session. Initial state.
     */
    data object Idle : SessionState()

    /**
     * Session is actively monitoring for distracting content.
     * @param overridesUsed Number of overrides consumed today
     */
    data class Monitoring(val overridesUsed: Int = 0) : SessionState()

    /**
     * Distraction detected with sufficient confidence.
     * @param confidence Detection confidence score (0.0-1.0)
     */
    data class BlockTriggered(val confidence: Float) : SessionState()

    /**
     * Enforcing cooldown period after block triggered.
     * @param remainingMs Milliseconds left in cooldown
     * @param overridesUsed Number of overrides consumed today
     */
    data class Cooldown(val remainingMs: Long, val overridesUsed: Int) : SessionState()

    /**
     * User overrode the block, grace period active.
     * @param remainingMs Milliseconds left in override window
     * @param overridesUsed Number of overrides consumed today (includes current)
     */
    data class Override(val remainingMs: Long, val overridesUsed: Int) : SessionState()
}
