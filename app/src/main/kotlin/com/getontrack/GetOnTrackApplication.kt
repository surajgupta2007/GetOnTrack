package com.getontrack

import android.app.Application
import com.getontrack.detector.StubContentDetector
import com.getontrack.engine.SessionEngine
import com.getontrack.policy.ModeratePolicy
import com.getontrack.storage.SessionStore

/**
 * Application class responsible for dependency wiring only.
 * 
 * NO business logic here.
 * Only initialization and dependency injection.
 */
class GetOnTrackApplication : Application() {
    
    // Global dependencies - wired at app startup
    lateinit var sessionEngine: SessionEngine
        private set
    
    lateinit var sessionStore: SessionStore
        private set
    
    lateinit var contentDetector: StubContentDetector
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dependencies
        val policy = ModeratePolicy()
        
        sessionEngine = SessionEngine(policy)
        sessionStore = SessionStore(this)
        contentDetector = StubContentDetector()
        
        // Note: Service dependencies are wired when services start
    }
}
