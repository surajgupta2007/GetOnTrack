package com.getontrack.engine

import com.getontrack.core.BlockingPolicy
import com.getontrack.core.SessionEvent
import com.getontrack.core.SessionState
import com.getontrack.core.SessionStateMachine
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe runtime wrapper around SessionStateMachine.
 * 
 * CRITICAL CONSTRAINTS:
 * - NO Android imports
 * - Manages coroutines for timing
 * - Thread-safe event processing
 * - Automatic cooldown timer management
 * - Prevents duplicate timer jobs
 * 
 * Responsibilities:
 * - Wrap pure state machine with coroutine-based timing
 * - Emit state changes to observers
 * - Manage cooldown/override timers
 * - Cancel previous timers safely
 * - Never block main thread
 */
class SessionEngine(
    policy: BlockingPolicy,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val stateMachine = SessionStateMachine(policy)
    private val observers = CopyOnWriteArrayList<SessionObserver>()
    private val timerJob = AtomicReference<Job?>(null)

    /**
     * Current session state.
     */
    val currentState: SessionState
        get() = stateMachine.currentState

    /**
     * Register an observer for state changes.
     */
    fun addObserver(observer: SessionObserver) {
        observers.add(observer)
    }

    /**
     * Unregister an observer.
     */
    fun removeObserver(observer: SessionObserver) {
        observers.remove(observer)
    }

    /**
     * Process an event and update state.
     * Thread-safe, non-blocking.
     * 
     * @param event The event to process
     */
    fun handleEvent(event: SessionEvent) {
        scope.launch {
            synchronized(stateMachine) {
                val newState = stateMachine.handle(event)
                handleStateTransition(newState)
            }
        }
    }

    /**
     * Handle side effects of state transition.
     * Must be called within synchronized block.
     */
    private fun handleStateTransition(newState: SessionState) {
        // Cancel any previous timer
        cancelTimer()

        // Notify observers on main dispatcher would be done in Android layer
        // Here we notify on current dispatcher since no Android
        notifyObservers(newState)

        // Start new timer if needed
        when (newState) {
            is SessionState.Cooldown -> startCooldownTimer(newState.remainingMs)
            is SessionState.Override -> startOverrideTimer(newState.remainingMs)
            else -> { /* No timer needed */ }
        }
    }

    /**
     * Start cooldown timer that automatically triggers CooldownFinished.
     */
    private fun startCooldownTimer(durationMs: Long) {
        val job = scope.launch {
            delay(durationMs)
            synchronized(stateMachine) {
                val newState = stateMachine.handle(SessionEvent.CooldownFinished)
                notifyObservers(newState)
            }
        }
        timerJob.set(job)
    }

    /**
     * Start override timer that automatically transitions back to monitoring.
     */
    private fun startOverrideTimer(durationMs: Long) {
        val job = scope.launch {
            delay(durationMs)
            synchronized(stateMachine) {
                val newState = stateMachine.handle(SessionEvent.CooldownFinished)
                notifyObservers(newState)
            }
        }
        timerJob.set(job)
    }

    /**
     * Cancel any active timer job.
     */
    private fun cancelTimer() {
        timerJob.getAndSet(null)?.cancel()
    }

    /**
     * Notify all observers of state change.
     */
    private fun notifyObservers(state: SessionState) {
        observers.forEach { observer ->
            try {
                observer.onStateChanged(state)
            } catch (e: Exception) {
                // Prevent observer exceptions from affecting engine
                e.printStackTrace()
            }
        }
    }

    /**
     * Cleanup resources.
     */
    fun shutdown() {
        cancelTimer()
        scope.cancel()
        observers.clear()
    }
}
