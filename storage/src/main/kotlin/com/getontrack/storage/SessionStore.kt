package com.getontrack.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.getontrack.core.SessionState
import java.text.SimpleDateFormat
import java.util.*

/**
 * Encrypted local storage for session state.
 * 
 * CRITICAL CONSTRAINTS:
 * - No analytics SDK
 * - No network calls
 * - Local only
 * - Encrypted storage
 * 
 * Responsibilities:
 * - Save/load override count
 * - Save/load last session state
 * - Persist data across app restarts
 */
class SessionStore(context: Context) {
    
    private val sharedPreferences: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    companion object {
        private const val PREFS_FILE_NAME = "session_store_encrypted"
        private const val KEY_OVERRIDES_USED = "overrides_used"
        private const val KEY_LAST_STATE_TYPE = "last_state_type"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
    }
    
    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Save current override count.
     */
    fun saveOverridesUsed(count: Int) {
        sharedPreferences.edit()
            .putInt(KEY_OVERRIDES_USED, count)
            .apply()
    }
    
    /**
     * Get saved override count.
     * Returns 0 if new day or not found.
     */
    fun getOverridesUsed(): Int {
        val today = getCurrentDate()
        val lastResetDate = sharedPreferences.getString(KEY_LAST_RESET_DATE, null)
        
        // Reset count if new day
        if (lastResetDate != today) {
            resetDailyCounters()
            return 0
        }
        
        return sharedPreferences.getInt(KEY_OVERRIDES_USED, 0)
    }
    
    /**
     * Save last session state type.
     */
    fun saveLastState(state: SessionState) {
        val stateType = when (state) {
            is SessionState.Idle -> "idle"
            is SessionState.Monitoring -> "monitoring"
            is SessionState.BlockTriggered -> "block_triggered"
            is SessionState.Cooldown -> "cooldown"
            is SessionState.Override -> "override"
        }
        
        sharedPreferences.edit()
            .putString(KEY_LAST_STATE_TYPE, stateType)
            .apply()
        
        // Also save override count if in relevant state
        when (state) {
            is SessionState.Monitoring -> saveOverridesUsed(state.overridesUsed)
            is SessionState.Cooldown -> saveOverridesUsed(state.overridesUsed)
            is SessionState.Override -> saveOverridesUsed(state.overridesUsed)
            else -> {}
        }
    }
    
    /**
     * Get last saved state type.
     */
    fun getLastStateType(): String {
        return sharedPreferences.getString(KEY_LAST_STATE_TYPE, "idle") ?: "idle"
    }
    
    /**
     * Reset daily counters.
     */
    private fun resetDailyCounters() {
        val today = getCurrentDate()
        sharedPreferences.edit()
            .putInt(KEY_OVERRIDES_USED, 0)
            .putString(KEY_LAST_RESET_DATE, today)
            .apply()
    }
    
    /**
     * Get current date string for comparison.
     */
    private fun getCurrentDate(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * Clear all stored data.
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
