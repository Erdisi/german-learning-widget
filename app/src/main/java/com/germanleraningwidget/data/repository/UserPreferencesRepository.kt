package com.germanleraningwidget.data.repository

import android.content.Context
import com.germanleraningwidget.util.DebugUtils
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
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
     * Preference keys with type safety - UPDATED FOR MULTI-LEVEL SUPPORT
     */
    private object PreferencesKeys {
        // New multi-level keys
        val SELECTED_GERMAN_LEVELS = stringSetPreferencesKey("selected_german_levels")
        val PRIMARY_GERMAN_LEVEL = stringPreferencesKey("primary_german_level")
        
        // Legacy keys for migration
        val GERMAN_LEVEL = stringPreferencesKey("german_level") // Legacy single level
        
        // Other existing keys
        val SELECTED_TOPICS = stringSetPreferencesKey("selected_topics")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    }
    
    /**
     * Reactive flow of user preferences with error handling and safe defaults.
     * Now includes migration from single-level to multi-level format.
     */
    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            DebugUtils.logError(TAG, "Error reading preferences", exception)
            // Emit empty preferences on error, will be handled by map
            emit(emptyPreferences())
        }
        .map { preferences ->
            try {
                mapPreferencesToUserPreferences(preferences)
            } catch (e: Exception) {
                try {
                    DebugUtils.logError(TAG, "Error mapping preferences", e)
                    UserPreferences()
                } catch (fallbackError: Exception) {
                    DebugUtils.logError(TAG, "Error creating fallback preferences", fallbackError)
                    UserPreferences()
                }
            }
        }
    
    /**
     * Map DataStore preferences to UserPreferences with validation and migration support.
     */
    private fun mapPreferencesToUserPreferences(preferences: Preferences): UserPreferences {
        // Check if we need to migrate from single-level to multi-level
        val legacyGermanLevel = preferences[PreferencesKeys.GERMAN_LEVEL]
        val selectedLevels = preferences[PreferencesKeys.SELECTED_GERMAN_LEVELS]
        val primaryLevel = preferences[PreferencesKeys.PRIMARY_GERMAN_LEVEL]
        val isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
        
        // Enhanced logging for debugging widget level issues
        DebugUtils.logInfo(TAG, "Loading preferences: selectedLevels=$selectedLevels, primaryLevel=$primaryLevel, legacyLevel=$legacyGermanLevel, onboarded=$isOnboardingCompleted")
        
        // Migration logic: if we have legacy data but no new data, migrate
        val (finalSelectedLevels, finalPrimaryLevel) = when {
            // New format exists - use it exactly as stored (respect user's choices)
            !selectedLevels.isNullOrEmpty() && !primaryLevel.isNullOrBlank() -> {
                val validLevels = selectedLevels.filter { it.isNotBlank() }.toSet()
                DebugUtils.logInfo(TAG, "Using saved multi-level preferences: levels=$validLevels, primary=$primaryLevel")
                Pair(validLevels, primaryLevel)
            }
            
            // Legacy format exists - migrate it
            !legacyGermanLevel.isNullOrBlank() -> {
                DebugUtils.logInfo(TAG, "Migrating from single-level ($legacyGermanLevel) to multi-level format")
                Pair(setOf(legacyGermanLevel), legacyGermanLevel)
            }
            
            // No data exists - only for first-time users or true data corruption
            else -> {
                if (isOnboardingCompleted) {
                    // This should rarely happen - only if user data was corrupted after onboarding
                    DebugUtils.logWarning(TAG, "Data corruption detected: onboarding completed but no levels found. Using A1 as emergency fallback.")
                    Pair(setOf("A1"), "A1")
                } else {
                    // New user hasn't completed onboarding yet
                    DebugUtils.logInfo(TAG, "New user, no preferences set yet")
                    Pair(emptySet(), "")
                }
            }
        }
        
        return UserPreferences(
            selectedGermanLevels = finalSelectedLevels,
            primaryGermanLevel = finalPrimaryLevel,
            selectedTopics = preferences[PreferencesKeys.SELECTED_TOPICS]?.filter { it.isNotBlank() }?.toSet() ?: setOf("Daily Life"),
            isOnboardingCompleted = isOnboardingCompleted
        )
    }
    
    /**
     * Update selected German levels with validation.
     */
    suspend fun updateSelectedGermanLevels(levels: Set<String>): Result<Unit> {
        val validLevels = levels.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (validLevels.isEmpty()) {
            return Result.failure(IllegalArgumentException("At least one German level must be selected"))
        }
        
        return safeWrite("update selected German levels") { preferences ->
            preferences[PreferencesKeys.SELECTED_GERMAN_LEVELS] = validLevels
            
            // Ensure primary level is still valid
            val currentPrimary = preferences[PreferencesKeys.PRIMARY_GERMAN_LEVEL]
            if (currentPrimary.isNullOrBlank() || currentPrimary !in validLevels) {
                // Set primary to the lowest selected level
                val newPrimary = validLevels.minByOrNull { level ->
                    when (level) {
                        "A1" -> 1; "A2" -> 2; "B1" -> 3; "B2" -> 4; "C1" -> 5; "C2" -> 6
                        else -> 1
                    }
                } ?: "A1"
                preferences[PreferencesKeys.PRIMARY_GERMAN_LEVEL] = newPrimary
                DebugUtils.logInfo(TAG, "Updated primary level to $newPrimary")
            }
        }
    }
    
    /**
     * Update primary German level with validation.
     */
    suspend fun updatePrimaryGermanLevel(level: String): Result<Unit> {
        val trimmedLevel = level.trim()
        if (trimmedLevel.isBlank()) {
            return Result.failure(IllegalArgumentException("Primary German level cannot be blank"))
        }
        
        return safeWrite("update primary German level") { preferences ->
            // Ensure the level is in selected levels
            val selectedLevels = preferences[PreferencesKeys.SELECTED_GERMAN_LEVELS] ?: setOf("A1")
            if (trimmedLevel !in selectedLevels) {
                // Add the level to selected levels
                preferences[PreferencesKeys.SELECTED_GERMAN_LEVELS] = selectedLevels + trimmedLevel
                                    DebugUtils.logInfo(TAG, "Added $trimmedLevel to selected levels when setting as primary")
            }
            preferences[PreferencesKeys.PRIMARY_GERMAN_LEVEL] = trimmedLevel
        }
    }
    
    /**
     * Toggle a German level (add if not present, remove if present)
     * Ensures at least one level remains selected and primary level is valid.
     */
    suspend fun toggleGermanLevel(level: String): Result<Boolean> {
        val trimmedLevel = level.trim()
        if (trimmedLevel.isBlank()) {
            return Result.failure(IllegalArgumentException("German level cannot be blank"))
        }
        
        return writeMutex.withLock {
            try {
                val currentPrefs = getCurrentPreferences().getOrThrow()
                val currentLevels = currentPrefs.selectedGermanLevels
                val isCurrentlySelected = trimmedLevel in currentLevels
                
                if (isCurrentlySelected) {
                    // Removing level
                    if (currentLevels.size <= 1) {
                        return@withLock Result.failure(
                            IllegalArgumentException("Cannot remove the last German level. At least one must be selected.")
                        )
                    }
                    
                    if (trimmedLevel == currentPrefs.primaryGermanLevel) {
                        return@withLock Result.failure(
                            IllegalArgumentException("Cannot remove the primary German level. Change primary level first.")
                        )
                    }
                    
                    updateSelectedGermanLevels(currentLevels - trimmedLevel).getOrThrow()
                    Result.success(false) // Level was removed
                } else {
                    // Adding level
                    updateSelectedGermanLevels(currentLevels + trimmedLevel).getOrThrow()
                    Result.success(true) // Level was added
                }
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error toggling German level", e)
                Result.failure(PreferencesException("Failed to toggle German level", e))
            }
        }
    }
    
    /**
     * Update German level with validation - LEGACY METHOD for backward compatibility.
     * @deprecated Use updateSelectedGermanLevels and updatePrimaryGermanLevel instead
     */
    @Deprecated("Use updateSelectedGermanLevels and updatePrimaryGermanLevel instead")
    suspend fun updateGermanLevel(level: String): Result<Unit> {
        DebugUtils.logWarning(TAG, "Using deprecated updateGermanLevel method. Consider migrating to multi-level methods.")
        return writeMutex.withLock {
            try {
                updateSelectedGermanLevels(setOf(level)).getOrThrow()
                updatePrimaryGermanLevel(level).getOrThrow()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
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
                
                DebugUtils.logInfo(TAG, "Updating preferences: selectedLevels=${preferences.selectedGermanLevels}, primaryLevel=${preferences.primaryGermanLevel}, topics=${preferences.selectedTopics.size}")
                
                context.dataStore.edit { prefs ->
                    prefs[PreferencesKeys.SELECTED_GERMAN_LEVELS] = preferences.selectedGermanLevels
                    prefs[PreferencesKeys.PRIMARY_GERMAN_LEVEL] = preferences.primaryGermanLevel
                    prefs[PreferencesKeys.SELECTED_TOPICS] = preferences.selectedTopics
                    prefs[PreferencesKeys.IS_ONBOARDING_COMPLETED] = preferences.isOnboardingCompleted
                }
                
                // Notify widgets to update with new preferences
                notifyWidgetsOfPreferenceChange()
                
                DebugUtils.logInfo(TAG, "Preferences updated successfully and widgets notified")
                Result.success(Unit)
                
            } catch (e: IOException) {
                DebugUtils.logError(TAG, "IO error updating preferences", e)
                Result.failure(PreferencesException("Failed to save preferences due to IO error", e))
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Unexpected error updating preferences", e)
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
            DebugUtils.logError(TAG, "Error getting current preferences", e)
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
            DebugUtils.logInfo(TAG, "DataStore test ${if (success) "PASSED" else "FAILED"}")
            Result.success(success)
            
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "DataStore test FAILED", e)
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
                DebugUtils.logWarning(TAG, "Found invalid preferences, repairing: ${validationResult.errorMessage}")
                val repairedPrefs = UserPreferences.createSafe()
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
            DebugUtils.logError(TAG, "Failed to validate DataStore", e)
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
                DebugUtils.logInfo(TAG, "Successfully completed: $operation")
                Result.success(Unit)
            } catch (e: IOException) {
                DebugUtils.logError(TAG, "IO error during $operation", e)
                Result.failure(PreferencesException("IO error during $operation", e))
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Unexpected error during $operation", e)
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
     * Notify all widgets that user preferences have changed.
     * This ensures widgets update to use new levels, topics, etc.
     * FIXED: Now invalidates widget customization cache to ensure fresh data.
     */
    private fun notifyWidgetsOfPreferenceChange() {
        try {
            // CRITICAL FIX: Use immediate update method for most reliable widget refresh
            com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerImmediateAllWidgetUpdates(context)
            
            DebugUtils.logInfo(TAG, "Successfully triggered immediate widget updates for preference changes")
        } catch (e: Exception) {
            DebugUtils.logWarning(TAG, "Failed immediate widget update for preference changes, using fallback", e)
            // Fallback: try reflection-based approach
            notifyWidgetsViaReflection()
        }
    }
    
    /**
     * Fallback method to notify widgets via reflection if direct calls fail
     */
    private fun notifyWidgetsViaReflection() {
        try {
            // First try immediate update with reflection approach
            val helperClass = Class.forName("com.germanleraningwidget.widget.WidgetCustomizationHelper")
            val immediateUpdateMethod = helperClass.getMethod("triggerImmediateAllWidgetUpdates", Context::class.java)
            immediateUpdateMethod.invoke(null, context)
            DebugUtils.logInfo(TAG, "Successfully triggered immediate widget updates via reflection")
        } catch (e: Exception) {
            DebugUtils.logWarning(TAG, "Failed immediate update via reflection, trying direct widget calls", e)
            
            // Last resort: direct widget update calls with cache invalidation
            try {
                com.germanleraningwidget.widget.WidgetCustomizationHelper.invalidateAllCache()
            } catch (cacheException: Exception) {
                DebugUtils.logWarning(TAG, "Failed to invalidate cache in fallback method", cacheException)
            }
            
            val widgetClasses = listOf(
                "com.germanleraningwidget.widget.GermanLearningWidget",
                "com.germanleraningwidget.widget.BookmarksWidget", 
                "com.germanleraningwidget.widget.BookmarksHeroWidget"
            )
            
            widgetClasses.forEach { className ->
                try {
                    val widgetClass = Class.forName(className)
                    val updateMethod = widgetClass.getMethod("updateAllWidgets", Context::class.java)
                    updateMethod.invoke(null, context)
                    DebugUtils.logInfo(TAG, "Successfully notified $className via reflection")
                } catch (widgetException: Exception) {
                    DebugUtils.logWarning(TAG, "Failed to notify $className via reflection", widgetException)
                }
            }
        }
    }
    
    /**
     * Custom exception for preferences operations
     */
    class PreferencesException(message: String, cause: Throwable? = null) : Exception(message, cause)
} 