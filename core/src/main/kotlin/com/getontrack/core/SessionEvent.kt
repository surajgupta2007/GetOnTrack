package com.getontrack.core

/**
 * Events that drive state transitions in the session state machine.
 * Pure data structures representing external stimuli.
 */
sealed class SessionEvent {
    /**
     * User initiated a monitoring session.
     */
    data object SessionStarted : SessionEvent()

    /**
     * User terminated the monitoring session.
     */
    data object SessionStopped : SessionEvent()

    /**
     * Content detector analyzed frame and returned result.
     * @param confidence Detection confidence score (0.0-1.0)
     */
    data class Detection(val confidence: Float) : SessionEvent()

    /**
     * User requested to override current block.
     */
    data object OverrideRequested : SessionEvent()

    /**
     * Cooldown timer completed naturally.
     */
    data object CooldownFinished : SessionEvent()
}
