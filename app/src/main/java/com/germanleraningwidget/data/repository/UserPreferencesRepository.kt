package com.germanleraningwidget.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.germanleraningwidget.data.model.DeliveryFrequency
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.UserPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repository for managing user preferences with DataStore.
 * 
 * Provides thread-safe operations for:
 * - Reading and writing user preferences
 * - Validating preference data
 * - Error handling with graceful degradation
 * - Testing capabilities
 * 
 * Thread Safety: All operations are thread-safe using Mutex
 * Error Handling: Comprehensive error handling with fallback to defaults
 * Validation: Input validation before storage
 */
class UserPreferencesRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    companion object {
        private const val TAG = "UserPreferencesRepo"
        
        // For testing - allows injection of custom repository
        internal fun createForTesting(
            context: Context,
            dispatcher: CoroutineDispatcher
        ): UserPreferencesRepository {
            return UserPreferencesRepository(context, dispatcher)
        }
    }
    
    // Mutex for protecting write operations
    private val writeMutex = Mutex()
    
    /**
     * Preference keys with type safety
     */
    private object PreferencesKeys {
        val GERMAN_LEVEL = stringPreferencesKey("german_level")
        val NATIVE_LANGUAGE = stringPreferencesKey("native_language")
        val SELECTED_TOPICS = stringSetPreferencesKey("selected_topics")
        val DELIVERY_FREQUENCY = stringPreferencesKey("delivery_frequency")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    }
    
    /**
     * Reactive flow of user preferences with error handling and safe defaults.
     */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            Log.e(TAG, "Error reading preferences", exception)
            // Emit empty preferences on error, will be handled by map
            emit(emptyPreferences())
        }
        .map { preferences ->
            try {
                mapPreferencesToUserPreferences(preferences)
            } catch (e: Exception) {
                Log.e(TAG, "Error mapping preferences", e)
                UserPreferences().withSafeDefaults()
            }
        }
    
    /**
     * Map DataStore preferences to UserPreferences with validation.
     */
    private fun mapPreferencesToUserPreferences(preferences: Preferences): UserPreferences {
        return UserPreferences(
            germanLevel = GermanLevel.fromString(preferences[PreferencesKeys.GERMAN_LEVEL]),
            nativeLanguage = preferences[PreferencesKeys.NATIVE_LANGUAGE]?.takeIf { it.isNotBlank() } ?: "English",
            selectedTopics = preferences[PreferencesKeys.SELECTED_TOPICS]?.filter { it.isNotBlank() }?.toSet() ?: emptySet(),
            deliveryFrequency = DeliveryFrequency.fromString(preferences[PreferencesKeys.DELIVERY_FREQUENCY]),
            isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
        ).withSafeDefaults()
    }
    
    /**
     * Update German level with validation.
     */
    suspend fun updateGermanLevel(level: GermanLevel): Result<Unit> {
        return safeWrite("update German level") { preferences ->
            preferences[PreferencesKeys.GERMAN_LEVEL] = level.name
        }
    }
    
    /**
     * Update native language with validation.
     */
    suspend fun updateNativeLanguage(language: String): Result<Unit> {
        val trimmedLanguage = language.trim()
        if (trimmedLanguage.isBlank()) {
            return Result.failure(IllegalArgumentException("Native language cannot be blank"))
        }
        
        return safeWrite("update native language") { preferences ->
            preferences[PreferencesKeys.NATIVE_LANGUAGE] = trimmedLanguage
        }
    }
    
    /**
     * Update selected topics with validation.
     */
    suspend fun updateSelectedTopics(topics: Set<String>): Result<Unit> {
        val validTopics = topics.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (validTopics.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one topic must be selected"))
        }
        
        return safeWrite("update selected topics") { preferences ->
            preferences[PreferencesKeys.SELECTED_TOPICS] = validTopics
        }
    }
    
    /**
     * Update delivery frequency with validation.
     */
    suspend fun updateDeliveryFrequency(frequency: DeliveryFrequency): Result<Unit> {
        return safeWrite("update delivery frequency") { preferences ->
            preferences[PreferencesKeys.DELIVERY_FREQUENCY] = frequency.name
        }
    }
    
    /**
     * Set onboarding completion status.
     */
    suspend fun setOnboardingCompleted(completed: Boolean): Result<Unit> {
        return safeWrite("set onboarding completed") { preferences ->
            preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed
        }
    }
    
    /**
     * Atomic update of all user preferences with comprehensive validation.
     */
    suspend fun updateUserPreferences(preferences: UserPreferences): Result<Unit> {
        return writeMutex.withLock {
            try {
                // Validate preferences first
                val validationResult = preferences.validate()
                if (!validationResult.isSuccess) {
                    return Result.failure(
                        IllegalArgumentException(validationResult.errorMessage ?: "Invalid preferences")
                    )
                }
                
                val safePreferences = preferences.withSafeDefaults()
                Log.d(TAG, "Updating preferences: level=${safePreferences.germanLevel}, topics=${safePreferences.selectedTopics.size}")
                
                context.dataStore.edit { prefs ->
                    prefs[PreferencesKeys.GERMAN_LEVEL] = safePreferences.germanLevel.name
                    prefs[PreferencesKeys.NATIVE_LANGUAGE] = safePreferences.nativeLanguage
                    prefs[PreferencesKeys.SELECTED_TOPICS] = safePreferences.selectedTopics
                    prefs[PreferencesKeys.DELIVERY_FREQUENCY] = safePreferences.deliveryFrequency.name
                    prefs[PreferencesKeys.IS_ONBOARDING_COMPLETED] = safePreferences.isOnboardingCompleted
                }
                
                Log.d(TAG, "Preferences updated successfully")
                Result.success(Unit)
                
            } catch (e: IOException) {
                Log.e(TAG, "IO error updating preferences", e)
                Result.failure(PreferencesException("Failed to save preferences due to IO error", e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error updating preferences", e)
                Result.failure(PreferencesException("Failed to save preferences: ${e.message}", e))
            }
        }
    }
    
    /**
     * Clear all user preferences and reset to defaults.
     */
    suspend fun clearPreferences(): Result<Unit> {
        return safeWrite("clear preferences") { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Get current preferences synchronously (for testing or one-time reads).
     * Use the Flow-based userPreferences for reactive updates.
     */
    suspend fun getCurrentPreferences(): Result<UserPreferences> {
        return try {
            val preferences = userPreferences.first()
            Result.success(preferences)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current preferences", e)
            Result.failure(PreferencesException("Failed to read preferences", e))
        }
    }
    
    /**
     * Test DataStore access by performing a simple write/read operation.
     * Useful for diagnosing DataStore issues.
     */
    suspend fun testDataStoreAccess(): Result<Boolean> {
        return try {
            val testKey = stringPreferencesKey("test_key")
            val testValue = "test_value_${System.currentTimeMillis()}"
            
            // Write test value
            context.dataStore.edit { prefs ->
                prefs[testKey] = testValue
            }
            
            // Read test value
            val readValue = context.dataStore.data.map { prefs ->
                prefs[testKey]
            }.first()
            
            // Clean up test value
            context.dataStore.edit { prefs ->
                prefs.remove(testKey)
            }
            
            val success = readValue == testValue
            Log.d(TAG, "DataStore test ${if (success) "PASSED" else "FAILED"}")
            Result.success(success)
            
        } catch (e: Exception) {
            Log.e(TAG, "DataStore test FAILED", e)
            Result.failure(PreferencesException("DataStore test failed", e))
        }
    }
    
    /**
     * Validate DataStore integrity and fix any corrupted data.
     */
    suspend fun validateAndRepairDataStore(): Result<ValidationReport> {
        return try {
            val currentPrefs = getCurrentPreferences().getOrThrow()
            val validationResult = currentPrefs.validate()
            
            if (!validationResult.isSuccess) {
                Log.w(TAG, "Found invalid preferences, repairing: ${validationResult.errorMessage}")
                val repairedPrefs = currentPrefs.withSafeDefaults()
                updateUserPreferences(repairedPrefs).getOrThrow()
                
                Result.success(
                    ValidationReport(
                        isValid = false,
                        wasRepaired = true,
                        issues = listOf(validationResult.errorMessage ?: "Unknown validation error")
                    )
                )
            } else {
                Result.success(
                    ValidationReport(
                        isValid = true,
                        wasRepaired = false,
                        issues = emptyList()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate DataStore", e)
            Result.failure(PreferencesException("Validation failed", e))
        }
    }
    
    /**
     * Safe write operation with proper error handling and logging.
     */
    private suspend fun safeWrite(
        operation: String,
        writeBlock: suspend (MutablePreferences) -> Unit
    ): Result<Unit> {
        return writeMutex.withLock {
            try {
                context.dataStore.edit(writeBlock)
                Log.d(TAG, "Successfully completed: $operation")
                Result.success(Unit)
            } catch (e: IOException) {
                Log.e(TAG, "IO error during $operation", e)
                Result.failure(PreferencesException("IO error during $operation", e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during $operation", e)
                Result.failure(PreferencesException("Failed to $operation", e))
            }
        }
    }
    
    /**
     * Data class for validation reports
     */
    data class ValidationReport(
        val isValid: Boolean,
        val wasRepaired: Boolean,
        val issues: List<String>
    )
    
    /**
     * Custom exception for preferences operations
     */
    class PreferencesException(message: String, cause: Throwable? = null) : Exception(message, cause)
} 