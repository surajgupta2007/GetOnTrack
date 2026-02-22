package com.getontrack.core

/**
 * Pure Kotlin state machine for session lifecycle management.
 * 
 * CRITICAL CONSTRAINTS:
 * - NO Android imports
 * - NO coroutines (those belong in engine layer)
 * - NO timers or async operations
 * - NO side effects
 * - Deterministic state transitions only
 * - Thread-safe (immutable state)
 * 
 * @property policy Blocking policy that defines behavior rules (never modified).
 */
class SessionStateMachine(private val policy: BlockingPolicy) {
    
    /**
     * Handle an incoming event and compute the next state.
     * 
     * This is a pure function with no side effects.
     * All state transitions are deterministic and synchronous.
     * 
     * @param currentState The current state of the session.
     * @param event The event to process.
     * @return The new state after processing the event.
     */
    fun handle(currentState: SessionState, event: SessionEvent): SessionState {
        return when (event) {
            is SessionEvent.SessionStarted -> handleSessionStarted(currentState)
            is SessionEvent.SessionStopped -> handleSessionStopped(currentState)
            is SessionEvent.Detection -> handleDetection(currentState, event)
            is SessionEvent.OverrideRequested -> handleOverrideRequested(currentState)
            is SessionEvent.CooldownFinished -> handleCooldownFinished(currentState)
        }
    }

    private fun handleSessionStarted(currentState: SessionState): SessionState {
        return when (currentState) {
            is SessionState.Idle -> SessionState.Monitoring(overridesUsed = 0)
            else -> currentState // Already in session, ignore
        }
    }

    private fun handleSessionStopped(currentState: SessionState): SessionState {
        return when (currentState) {
            is SessionState.Idle -> currentState // Already idle
            else -> SessionState.Idle
        }
    }

    private fun handleDetection(
        currentState: SessionState,
        event: SessionEvent.Detection
    ): SessionState {
        return when (currentState) {
            is SessionState.Monitoring -> {
                if (event.confidence >= policy.threshold()) {
                    SessionState.BlockTriggered(
                        confidence = event.confidence,
                        overridesUsed = currentState.overridesUsed
                    )
                } else {
                    currentState // Below threshold, keep monitoring
                }
            }
            is SessionState.Override -> {
                // Still in override grace period, allow activity
                currentState
            }
            else -> currentState // Not in monitoring state, ignore detection
        }
    }

    private fun handleOverrideRequested(currentState: SessionState): SessionState {
        return when (currentState) {
            is SessionState.BlockTriggered -> {
                // Check if user has overrides remaining
                if (currentState.overridesUsed < policy.maxOverridesPerDay()) {
                    SessionState.Override(
                        remainingMs = policy.cooldownDurationMs(),
                        overridesUsed = currentState.overridesUsed + 1
                    )
                } else {
                    // No overrides left, force cooldown
                    SessionState.Cooldown(
                        remainingMs = policy.cooldownDurationMs(),
                        overridesUsed = currentState.overridesUsed
                    )
                }
            }
            is SessionState.Cooldown -> {
                // Check if user has overrides remaining
                if (currentState.overridesUsed < policy.maxOverridesPerDay()) {
                    SessionState.Override(
                        remainingMs = policy.cooldownDurationMs(),
                        overridesUsed = currentState.overridesUsed + 1
                    )
                } else {
                    // No overrides left, stay in cooldown
                    currentState
                }
            }
            else -> currentState // Override only valid during block or cooldown
        }
    }

    private fun handleCooldownFinished(currentState: SessionState): SessionState {
        return when (currentState) {
            is SessionState.Cooldown -> {
                SessionState.Monitoring(overridesUsed = currentState.overridesUsed)
            }
            is SessionState.Override -> {
                SessionState.Monitoring(overridesUsed = currentState.overridesUsed)
            }
            else -> currentState // Cooldown finished event only valid during cooldown/override
        }
    }
}
