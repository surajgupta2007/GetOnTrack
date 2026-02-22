package com.getontrack.core

/**
 * Defines blocking policy parameters.
 * Pure Kotlin interface with no side effects.
 *
 * Implementations determine strictness level of the blocking behavior.
 */
interface BlockingPolicy {
    /**
     * Confidence threshold for triggering a block.
     *
     * @return Threshold value between 0.0 and 1.0.
     *         Detection confidence must exceed this to trigger block.
     */
    fun threshold(): Float

    /**
     * Duration of enforced cooldown period after block is triggered.
     *
     * @return Cooldown duration in milliseconds.
     */
    fun cooldownDurationMs(): Long

    /**
     * Maximum number of manual overrides allowed per day.
     *
     * @return Maximum override count. Once reached, overrides are rejected.
     */
    fun maxOverridesPerDay(): Int
}
