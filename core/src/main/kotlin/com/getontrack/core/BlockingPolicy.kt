package com.getontrack.core

/**
 * Defines blocking behavior policy.
 * Immutable configuration with no side effects.
 */
interface BlockingPolicy {
    /**
     * Minimum confidence threshold to trigger a block (0.0-1.0).
     */
    fun threshold(): Float

    /**
     * Duration of enforced cooldown in milliseconds.
     */
    fun cooldownDurationMs(): Long

    /**
     * Maximum number of overrides allowed per day.
     */
    fun maxOverridesPerDay(): Int
}
