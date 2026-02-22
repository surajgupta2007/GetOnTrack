package com.getontrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.getontrack.core.SessionEvent
import com.getontrack.databinding.ActivityMainBinding

/**
 * Main activity for dependency wiring and user interaction.
 * 
 * NO business logic - only UI and event delegation.
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var app: GetOnTrackApplication
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        app = application as GetOnTrackApplication
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.startButton.setOnClickListener {
            app.sessionEngine.handleEvent(SessionEvent.SessionStarted)
        }
        
        binding.stopButton.setOnClickListener {
            app.sessionEngine.handleEvent(SessionEvent.SessionStopped)
        }
        
        // Observe state changes
        app.sessionEngine.addObserver { state ->
            runOnUiThread {
                binding.stateText.text = "State: ${state::class.simpleName}"
            }
        }
    }
}
