package com.getontrack.policy

import com.getontrack.core.BlockingPolicy

/**
 * Lenient blocking policy with relaxed constraints.
 * Suitable for users starting their focus journey.
 */
class LenientPolicy : BlockingPolicy {
    override fun threshold(): Float = 0.75f
    override fun cooldownDurationMs(): Long = 10_000L // 10 seconds
    override fun maxOverridesPerDay(): Int = 10
}

/**
 * Moderate blocking policy with balanced constraints.
 * Suitable for users with some self-control practice.
 */
class ModeratePolicy : BlockingPolicy {
    override fun threshold(): Float = 0.60f
    override fun cooldownDurationMs(): Long = 30_000L // 30 seconds
    override fun maxOverridesPerDay(): Int = 5
}

/**
 * Strict blocking policy with tight constraints.
 * Suitable for users seeking maximum focus enforcement.
 */
class StrictPolicy : BlockingPolicy {
    override fun threshold(): Float = 0.50f
    override fun cooldownDurationMs(): Long = 60_000L // 1 minute
    override fun maxOverridesPerDay(): Int = 2
}

/**
 * Custom blocking policy allowing full configuration.
 *
 * @property customThreshold Detection threshold (0.0 to 1.0).
 * @property customCooldownMs Cooldown duration in milliseconds.
 * @property customMaxOverrides Maximum overrides per day.
 */
class CustomPolicy(
    private val customThreshold: Float,
    private val customCooldownMs: Long,
    private val customMaxOverrides: Int
) : BlockingPolicy {
    
    init {
        require(customThreshold in 0.0f..1.0f) {
            "Threshold must be between 0.0 and 1.0"
        }
        require(customCooldownMs > 0) {
            "Cooldown duration must be positive"
        }
        require(customMaxOverrides >= 0) {
            "Max overrides must be non-negative"
        }
    }
    
    override fun threshold(): Float = customThreshold
    override fun cooldownDurationMs(): Long = customCooldownMs
    override fun maxOverridesPerDay(): Int = customMaxOverrides
}
