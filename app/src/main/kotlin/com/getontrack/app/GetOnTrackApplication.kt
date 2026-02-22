package com.getontrack.app

import android.app.Application
import com.getontrack.detector.ContentDetector
import com.getontrack.detector.StubContentDetector
import com.getontrack.engine.SessionEngine
import com.getontrack.policy.ModeratePolicy
import com.getontrack.storage.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application class for dependency wiring.
 * 
 * RESPONSIBILITIES:
 * - Initialize all modules
 * - Wire dependencies
 * - Provide singleton instances
 * - NO business logic
 * 
 * This is the only place where modules are connected together.
 */
class GetOnTrackApplication : Application() {
    
    companion object {
        private lateinit var instance: GetOnTrackApplication
        
        fun getInstance(): GetOnTrackApplication = instance
    }
    
    // Singleton instances
    private lateinit var sessionStore: SessionStore
    private lateinit var sessionEngine: SessionEngine
    private lateinit var contentDetector: ContentDetector
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        initializeDependencies()
    }
    
    /**
     * Initialize all dependencies.
     * This is where the dependency graph is wired together.
     */
    private fun initializeDependencies() {
        // Initialize storage
        sessionStore = SessionStore(applicationContext)
        
        // Initialize policy (using moderate by default)
        val policy = ModeratePolicy()
        
        // Initialize engine with application scope
        val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        sessionEngine = SessionEngine(policy, appScope)
        
        // Initialize content detector (stub implementation)
        contentDetector = StubContentDetector()
        
        // Restore previous state if any
        val lastState = sessionStore.restoreLastState()
        // In a full implementation, we would restore the engine state here
    }
    
    /**
     * Provide SessionEngine instance.
     */
    fun getSessionEngine(): SessionEngine = sessionEngine
    
    /**
     * Provide SessionStore instance.
     */
    fun getSessionStore(): SessionStore = sessionStore
    
    /**
     * Provide ContentDetector instance.
     */
    fun getContentDetector(): ContentDetector = contentDetector
}
