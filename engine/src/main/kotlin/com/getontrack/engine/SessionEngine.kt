package com.getontrack.engine

import com.getontrack.core.BlockingPolicy
import com.getontrack.core.SessionEvent
import com.getontrack.core.SessionState
import com.getontrack.core.SessionStateMachine
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Runtime wrapper around SessionStateMachine.
 * 
 * RESPONSIBILITIES:
 * - Wraps pure state machine with async capabilities
 * - Manages cooldown/override timers using coroutines
 * - Emits state updates to registered observers
 * - Thread-safe state management
 * 
 * CONSTRAINTS:
 * - NO Android imports
 * - Timing logic lives here (not in core)
 * - Never blocks calling thread
 * - Cancels previous timers safely
 * 
 * @property policy Blocking policy for the session.
 * @property scope CoroutineScope for managing async operations.
 */
class SessionEngine(
    private val policy: BlockingPolicy,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val stateMachine = SessionStateMachine(policy)
    private val observers = CopyOnWriteArrayList<SessionObserver>()
    
    @Volatile
    private var currentState: SessionState = SessionState.Idle
    
    private var cooldownJob: Job? = null
    
    /**
     * Register an observer to receive state change notifications.
     * 
     * @param observer The observer to register.
     */
    fun addObserver(observer: SessionObserver) {
        observers.add(observer)
        // Immediately notify of current state
        observer.onStateChanged(currentState)
    }
    
    /**
     * Unregister an observer.
     * 
     * @param observer The observer to remove.
     */
    fun removeObserver(observer: SessionObserver) {
        observers.remove(observer)
    }
    
    /**
     * Process an event and update state.
     * 
     * This method is thread-safe and can be called from any thread.
     * State updates are emitted to observers on the coroutine context.
     * 
     * @param event The event to process.
     */
    fun handleEvent(event: SessionEvent) {
        scope.launch {
            synchronized(this@SessionEngine) {
                val newState = stateMachine.handle(currentState, event)
                
                if (newState != currentState) {
                    currentState = newState
                    
                    // Manage timers based on new state
                    when (newState) {
                        is SessionState.Cooldown -> {
                            scheduleCooldownFinish(newState.remainingMs)
                        }
                        is SessionState.Override -> {
                            scheduleCooldownFinish(newState.remainingMs)
                        }
                        else -> {
                            // Cancel any existing cooldown timer
                            cancelCooldownTimer()
                        }
                    }
                    
                    // Notify observers
                    notifyObservers(newState)
                }
            }
        }
    }
    
    /**
     * Get the current session state.
     * 
     * @return Current state snapshot.
     */
    fun getCurrentState(): SessionState = currentState
    
    /**
     * Schedule automatic CooldownFinished event after specified delay.
     * Cancels any previous cooldown timer to prevent duplicates.
     * 
     * @param delayMs Delay in milliseconds.
     */
    private fun scheduleCooldownFinish(delayMs: Long) {
        // Cancel previous timer if exists
        cancelCooldownTimer()
        
        // Schedule new timer
        cooldownJob = scope.launch {
            delay(delayMs)
            handleEvent(SessionEvent.CooldownFinished)
        }
    }
    
    /**
     * Cancel the current cooldown timer if active.
     */
    private fun cancelCooldownTimer() {
        cooldownJob?.cancel()
        cooldownJob = null
    }
    
    /**
     * Notify all registered observers of state change.
     * 
     * @param state The new state to notify.
     */
    private fun notifyObservers(state: SessionState) {
        observers.forEach { observer ->
            try {
                observer.onStateChanged(state)
            } catch (e: Exception) {
                // Prevent observer exceptions from breaking the engine
                // In production, this should be logged
            }
        }
    }
    
    /**
     * Clean up resources.
     * Should be called when engine is no longer needed.
     */
    fun shutdown() {
        cancelCooldownTimer()
        observers.clear()
        scope.cancel()
    }
}
