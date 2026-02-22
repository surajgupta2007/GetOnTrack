package com.getontrack.core

/**
 * Pure state machine implementing session blocking logic.
 * 
 * CRITICAL CONSTRAINTS:
 * - NO Android imports
 * - NO coroutines
 * - NO timers
 * - NO side effects
 * - Deterministic transitions only
 * - Never modifies policy
 * 
 * All timing logic must be handled by engine layer.
 */
class SessionStateMachine(private val policy: BlockingPolicy) {
    
    /**
     * Current state of the session.
     */
    var currentState: SessionState = SessionState.Idle
        private set

    /**
     * Process an event and transition to new state.
     * 
     * @param event The event to process
     * @return The new state after transition
     */
    fun handle(event: SessionEvent): SessionState {
        currentState = when (currentState) {
            is SessionState.Idle -> handleIdle(event)
            is SessionState.Monitoring -> handleMonitoring(event, currentState as SessionState.Monitoring)
            is SessionState.BlockTriggered -> handleBlockTriggered(event, currentState as SessionState.BlockTriggered)
            is SessionState.Cooldown -> handleCooldown(event, currentState as SessionState.Cooldown)
            is SessionState.Override -> handleOverride(event, currentState as SessionState.Override)
        }
        return currentState
    }

    private fun handleIdle(event: SessionEvent): SessionState {
        return when (event) {
            is SessionEvent.SessionStarted -> SessionState.Monitoring(overridesUsed = 0)
            else -> SessionState.Idle // Ignore invalid transitions
        }
    }

    private fun handleMonitoring(event: SessionEvent, state: SessionState.Monitoring): SessionState {
        return when (event) {
            is SessionEvent.SessionStopped -> SessionState.Idle
            is SessionEvent.Detection -> {
                if (event.confidence >= policy.threshold()) {
                    SessionState.BlockTriggered(confidence = event.confidence)
                } else {
                    state // Stay in monitoring
                }
            }
            else -> state // Ignore invalid transitions
        }
    }

    private fun handleBlockTriggered(event: SessionEvent, state: SessionState.BlockTriggered): SessionState {
        return when (event) {
            is SessionEvent.SessionStopped -> SessionState.Idle
            is SessionEvent.OverrideRequested -> {
                val currentOverrides = getCurrentOverrides()
                if (currentOverrides < policy.maxOverridesPerDay()) {
                    SessionState.Override(
                        remainingMs = policy.cooldownDurationMs(),
                        overridesUsed = currentOverrides + 1
                    )
                } else {
                    // Max overrides reached, move to cooldown
                    SessionState.Cooldown(
                        remainingMs = policy.cooldownDurationMs(),
                        overridesUsed = currentOverrides
                    )
                }
            }
            else -> {
                // Auto-transition to cooldown if no override requested
                val currentOverrides = getCurrentOverrides()
                SessionState.Cooldown(
                    remainingMs = policy.cooldownDurationMs(),
                    overridesUsed = currentOverrides
                )
            }
        }
    }

    private fun handleCooldown(event: SessionEvent, state: SessionState.Cooldown): SessionState {
        return when (event) {
            is SessionEvent.SessionStopped -> SessionState.Idle
            is SessionEvent.CooldownFinished -> SessionState.Monitoring(overridesUsed = state.overridesUsed)
            else -> state // Ignore invalid transitions, cooldown cannot be interrupted
        }
    }

    private fun handleOverride(event: SessionEvent, state: SessionState.Override): SessionState {
        return when (event) {
            is SessionEvent.SessionStopped -> SessionState.Idle
            is SessionEvent.CooldownFinished -> SessionState.Monitoring(overridesUsed = state.overridesUsed)
            is SessionEvent.Detection -> {
                // During override, new detection triggers immediate cooldown
                if (event.confidence >= policy.threshold()) {
                    SessionState.Cooldown(
                        remainingMs = policy.cooldownDurationMs(),
                        overridesUsed = state.overridesUsed
                    )
                } else {
                    state
                }
            }
            else -> state // Ignore invalid transitions
        }
    }

    /**
     * Extract current override count from state.
     */
    private fun getCurrentOverrides(): Int {
        return when (val state = currentState) {
            is SessionState.Monitoring -> state.overridesUsed
            is SessionState.Cooldown -> state.overridesUsed
            is SessionState.Override -> state.overridesUsed
            else -> 0
        }
    }
}
