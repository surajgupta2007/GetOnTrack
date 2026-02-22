package com.getontrack.policy

import com.getontrack.core.BlockingPolicy

/**
 * Lenient blocking policy with low threshold and generous override allowance.
 * Good for users just starting to build focus habits.
 */
class LenientPolicy : BlockingPolicy {
    override fun threshold(): Float = 0.7f // 70% confidence required
    override fun cooldownDurationMs(): Long = 30_000L // 30 seconds
    override fun maxOverridesPerDay(): Int = 10
}

/**
 * Moderate blocking policy balancing strictness with flexibility.
 * Suitable for users with established focus patterns.
 */
class ModeratePolicy : BlockingPolicy {
    override fun threshold(): Float = 0.8f // 80% confidence required
    override fun cooldownDurationMs(): Long = 60_000L // 1 minute
    override fun maxOverridesPerDay(): Int = 5
}

/**
 * Strict blocking policy with high threshold and limited overrides.
 * For users committed to maximum focus and minimal interruptions.
 */
class StrictPolicy : BlockingPolicy {
    override fun threshold(): Float = 0.9f // 90% confidence required
    override fun cooldownDurationMs(): Long = 180_000L // 3 minutes
    override fun maxOverridesPerDay(): Int = 2
}
