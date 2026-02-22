package com.getontrack.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.getontrack.core.SessionState

/**
 * Encrypted local storage for session state and override counts.
 * 
 * RESPONSIBILITIES:
 * - Save and restore overridesUsed count
 * - Save and restore last SessionState
 * - Use EncryptedSharedPreferences for security
 * 
 * CONSTRAINTS:
 * - NO analytics SDK integration
 * - NO network calls
 * - Local storage only
 * - Encrypted at rest
 */
class SessionStore(context: Context) {
    
    companion object {
        private const val PREFS_FILE_NAME = "getontrack_session_prefs"
        private const val KEY_OVERRIDES_USED = "overrides_used"
        private const val KEY_LAST_STATE = "last_state"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
    }
    
    private val encryptedPrefs: SharedPreferences
    
    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    /**
     * Save the number of overrides used.
     * Should be reset daily.
     * 
     * @param count Number of overrides used.
     */
    fun saveOverridesUsed(count: Int) {
        encryptedPrefs.edit().putInt(KEY_OVERRIDES_USED, count).apply()
    }
    
    /**
     * Get the number of overrides used.
     * Returns 0 if not set or if daily reset needed.
     * 
     * @return Number of overrides used today.
     */
    fun getOverridesUsed(): Int {
        checkDailyReset()
        return encryptedPrefs.getInt(KEY_OVERRIDES_USED, 0)
    }
    
    /**
     * Save the last known session state.
     * Used for restoration after app restart.
     * 
     * @param state The session state to save.
     */
    fun saveLastState(state: SessionState) {
        val stateString = when (state) {
            is SessionState.Idle -> "idle"
            is SessionState.Monitoring -> "monitoring:${state.overridesUsed}"
            is SessionState.BlockTriggered -> "block:${state.confidence}"
            is SessionState.Cooldown -> "cooldown:${state.remainingMs}:${state.overridesUsed}"
            is SessionState.Override -> "override:${state.remainingMs}:${state.overridesUsed}"
        }
        
        encryptedPrefs.edit().putString(KEY_LAST_STATE, stateString).apply()
    }
    
    /**
     * Restore the last known session state.
     * Returns Idle if no state was saved or if restoration fails.
     * 
     * @return The restored session state or Idle.
     */
    fun restoreLastState(): SessionState {
        val stateString = encryptedPrefs.getString(KEY_LAST_STATE, null) ?: return SessionState.Idle
        
        return try {
            parseStateString(stateString)
        } catch (e: Exception) {
            SessionState.Idle
        }
    }
    
    /**
     * Parse a state string back into SessionState.
     */
    private fun parseStateString(stateString: String): SessionState {
        val parts = stateString.split(":")
        
        return when (parts[0]) {
            "idle" -> SessionState.Idle
            "monitoring" -> SessionState.Monitoring(parts.getOrNull(1)?.toIntOrNull() ?: 0)
            "block" -> SessionState.BlockTriggered(parts.getOrNull(1)?.toFloatOrNull() ?: 0f)
            "cooldown" -> SessionState.Cooldown(
                remainingMs = parts.getOrNull(1)?.toLongOrNull() ?: 0L,
                overridesUsed = parts.getOrNull(2)?.toIntOrNull() ?: 0
            )
            "override" -> SessionState.Override(
                remainingMs = parts.getOrNull(1)?.toLongOrNull() ?: 0L,
                overridesUsed = parts.getOrNull(2)?.toIntOrNull() ?: 0
            )
            else -> SessionState.Idle
        }
    }
    
    /**
     * Check if daily reset is needed and reset if so.
     * Resets override count at midnight.
     */
    private fun checkDailyReset() {
        val today = getCurrentDate()
        val lastResetDate = encryptedPrefs.getString(KEY_LAST_RESET_DATE, null)
        
        if (lastResetDate != today) {
            // New day, reset overrides
            encryptedPrefs.edit()
                .putInt(KEY_OVERRIDES_USED, 0)
                .putString(KEY_LAST_RESET_DATE, today)
                .apply()
        }
    }
    
    /**
     * Get current date as a string (YYYY-MM-DD).
     */
    private fun getCurrentDate(): String {
        val calendar = java.util.Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
    
    /**
     * Clear all stored data.
     * Use with caution - this is irreversible.
     */
    fun clear() {
        encryptedPrefs.edit().clear().apply()
    }
}
