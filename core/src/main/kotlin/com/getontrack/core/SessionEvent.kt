package com.getontrack.core

/**
 * Events that can trigger state transitions in the session state machine.
 * Pure Kotlin sealed class - no side effects, no Android dependencies.
 */
sealed class SessionEvent {
    /**
     * Session monitoring has started.
     */
    data object SessionStarted : SessionEvent()

    /**
     * Session monitoring has stopped.
     */
    data object SessionStopped : SessionEvent()

    /**
     * Content detection result received.
     *
     * @property confidence Detection confidence level (0.0 to 1.0).
     *                      Higher values indicate stronger match to distraction pattern.
     */
    data class Detection(val confidence: Float) : SessionEvent()

    /**
     * User requested manual override to bypass current block.
     */
    data object OverrideRequested : SessionEvent()

    /**
     * Cooldown period has finished naturally.
     */
    data object CooldownFinished : SessionEvent()
}
