package com.getontrack.core

/**
 * Represents all possible states in a session lifecycle.
 * Pure Kotlin sealed class hierarchy with no side effects.
 */
sealed class SessionState {
    /**
     * Initial state - no active monitoring session.
     */
    data object Idle : SessionState()

    /**
     * Active monitoring state - tracking user activity.
     *
     * @property overridesUsed Number of manual overrides used in current session period.
     */
    data class Monitoring(val overridesUsed: Int = 0) : SessionState()

    /**
     * Block is triggered when distraction detected above threshold.
     *
     * @property confidence Detection confidence level (0.0 to 1.0).
     * @property overridesUsed Number of manual overrides used in current session period.
     */
    data class BlockTriggered(val confidence: Float, val overridesUsed: Int = 0) : SessionState()

    /**
     * Cooldown period after block - user must wait before continuing.
     *
     * @property remainingMs Milliseconds remaining in cooldown period.
     * @property overridesUsed Number of manual overrides used in current session period.
     */
    data class Cooldown(val remainingMs: Long, val overridesUsed: Int) : SessionState()

    /**
     * Override state - user manually bypassed the block.
     *
     * @property remainingMs Milliseconds remaining in override grace period.
     * @property overridesUsed Number of manual overrides used in current session period.
     */
    data class Override(val remainingMs: Long, val overridesUsed: Int) : SessionState()
}
