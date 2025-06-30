package com.germanleraningwidget.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.germanleraningwidget.data.model.*
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private val Context.widgetCustomizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_customizations")

/**
 * Repository for managing widget customization settings with DataStore.
 * 
 * Provides thread-safe operations for:
 * - Reading and writing widget customizations
 * - Real-time widget updates when settings change
 * - Validation and defaults for customization settings
 * 
 * Thread Safety: All operations are thread-safe using Mutex
 * Error Handling: Comprehensive error handling with fallback to defaults
 */
class WidgetCustomizationRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    companion object {
        private const val TAG = "WidgetCustomizationRepo"
        
        // Singleton instance
        @Suppress("StaticFieldLeak") // Safe: Uses application context only, not activity context
        @Volatile
        private var INSTANCE: WidgetCustomizationRepository? = null
        
        fun getInstance(context: Context): WidgetCustomizationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WidgetCustomizationRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Mutex for protecting write operations
    private val writeMutex = Mutex()
    
    /**
     * Preference keys for widget customizations
     * Note: Text size keys removed as auto-sizing is now implemented
     */
    private object PreferencesKeys {
        // Main Widget
        val MAIN_BACKGROUND_COLOR = stringPreferencesKey("main_background_color")
        val MAIN_TEXT_CONTRAST = stringPreferencesKey("main_text_contrast")
        val MAIN_SENTENCES_PER_DAY = intPreferencesKey("main_sentences_per_day")
        
        // Bookmarks Widget
        val BOOKMARKS_BACKGROUND_COLOR = stringPreferencesKey("bookmarks_background_color")
        val BOOKMARKS_TEXT_CONTRAST = stringPreferencesKey("bookmarks_text_contrast")
        val BOOKMARKS_SENTENCES_PER_DAY = intPreferencesKey("bookmarks_sentences_per_day")
        
        // Hero Widget
        val HERO_BACKGROUND_COLOR = stringPreferencesKey("hero_background_color")
        val HERO_TEXT_CONTRAST = stringPreferencesKey("hero_text_contrast")
        val HERO_SENTENCES_PER_DAY = intPreferencesKey("hero_sentences_per_day")
    }
    
    /**
     * Reactive flow of all widget customizations with error handling.
     */
    val allWidgetCustomizations: Flow<AllWidgetCustomizations> = context.widgetCustomizationDataStore.data
        .catch { exception ->
            Log.e(TAG, "Error reading widget customizations", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            try {
                mapPreferencesToCustomizations(preferences)
            } catch (e: Exception) {
                Log.e(TAG, "Error mapping widget customizations", e)
                AllWidgetCustomizations.createDefault()
            }
        }
    
    /**
     * Get customization flow for specific widget type.
     */
    fun getWidgetCustomization(widgetType: WidgetType): Flow<WidgetCustomization> {
        return allWidgetCustomizations.map { it.getCustomization(widgetType) }
    }
    
    /**
     * Map DataStore preferences to AllWidgetCustomizations.
     */
    private fun mapPreferencesToCustomizations(preferences: Preferences): AllWidgetCustomizations {
        return AllWidgetCustomizations(
            mainWidget = WidgetCustomization(
                widgetType = WidgetType.MAIN,
                backgroundColor = WidgetBackgroundColor.fromKey(
                    preferences[PreferencesKeys.MAIN_BACKGROUND_COLOR] ?: WidgetBackgroundColor.CREAM.key
                ),
                textContrast = WidgetTextContrast.fromKey(
                    preferences[PreferencesKeys.MAIN_TEXT_CONTRAST] ?: WidgetTextContrast.NORMAL.key
                ),
                sentencesPerDay = preferences[PreferencesKeys.MAIN_SENTENCES_PER_DAY] ?: WidgetCustomization.DEFAULT_SENTENCES_PER_DAY
            ),
            bookmarksWidget = WidgetCustomization(
                widgetType = WidgetType.BOOKMARKS,
                backgroundColor = WidgetBackgroundColor.fromKey(
                    preferences[PreferencesKeys.BOOKMARKS_BACKGROUND_COLOR] ?: WidgetBackgroundColor.ORANGE.key
                ),
                textContrast = WidgetTextContrast.fromKey(
                    preferences[PreferencesKeys.BOOKMARKS_TEXT_CONTRAST] ?: WidgetTextContrast.NORMAL.key
                ),
                sentencesPerDay = preferences[PreferencesKeys.BOOKMARKS_SENTENCES_PER_DAY] ?: WidgetCustomization.DEFAULT_SENTENCES_PER_DAY
            ),
            heroWidget = WidgetCustomization(
                widgetType = WidgetType.HERO,
                backgroundColor = WidgetBackgroundColor.fromKey(
                    preferences[PreferencesKeys.HERO_BACKGROUND_COLOR] ?: WidgetBackgroundColor.NAVY.key
                ),
                textContrast = WidgetTextContrast.fromKey(
                    preferences[PreferencesKeys.HERO_TEXT_CONTRAST] ?: WidgetTextContrast.NORMAL.key
                ),
                sentencesPerDay = preferences[PreferencesKeys.HERO_SENTENCES_PER_DAY] ?: WidgetCustomization.DEFAULT_SENTENCES_PER_DAY
            )
        )
    }
    
    /**
     * Update customization for a specific widget.
     */
    suspend fun updateWidgetCustomization(customization: WidgetCustomization): Result<Unit> {
        return writeMutex.withLock {
            try {
                val validationResult = customization.validate()
                if (!validationResult.isSuccess) {
                    return Result.failure(
                        IllegalArgumentException(validationResult.errorMessage ?: "Invalid customization")
                    )
                }
                
                context.widgetCustomizationDataStore.edit { prefs ->
                    when (customization.widgetType) {
                        WidgetType.MAIN -> {
                            prefs[PreferencesKeys.MAIN_BACKGROUND_COLOR] = customization.backgroundColor.key
                            prefs[PreferencesKeys.MAIN_TEXT_CONTRAST] = customization.textContrast.key
                            prefs[PreferencesKeys.MAIN_SENTENCES_PER_DAY] = customization.sentencesPerDay
                        }
                        WidgetType.BOOKMARKS -> {
                            prefs[PreferencesKeys.BOOKMARKS_BACKGROUND_COLOR] = customization.backgroundColor.key
                            prefs[PreferencesKeys.BOOKMARKS_TEXT_CONTRAST] = customization.textContrast.key
                            prefs[PreferencesKeys.BOOKMARKS_SENTENCES_PER_DAY] = customization.sentencesPerDay
                        }
                        WidgetType.HERO -> {
                            prefs[PreferencesKeys.HERO_BACKGROUND_COLOR] = customization.backgroundColor.key
                            prefs[PreferencesKeys.HERO_TEXT_CONTRAST] = customization.textContrast.key
                            prefs[PreferencesKeys.HERO_SENTENCES_PER_DAY] = customization.sentencesPerDay
                        }
                    }
                }
                
                // Trigger simple widget update using new helper
                com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerWidgetUpdate(context, customization.widgetType)
                
                // Reschedule worker with new frequency settings
                try {
                    SentenceDeliveryWorker.scheduleWork(context)
                    Log.d(TAG, "Worker rescheduled with updated frequency settings")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to reschedule worker after customization update", e)
                    // Don't fail the entire operation if worker scheduling fails
                }
                
                Log.d(TAG, "Widget customization updated successfully for ${customization.widgetType.displayName}")
                Result.success(Unit)
                
            } catch (e: IOException) {
                Log.e(TAG, "IO error updating widget customization", e)
                Result.failure(Exception("Failed to save customization due to IO error", e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error updating widget customization", e)
                Result.failure(Exception("Failed to save customization: ${e.message}", e))
            }
        }
    }
    
    /**
     * Update all widget customizations atomically.
     */
    suspend fun updateAllWidgetCustomizations(customizations: AllWidgetCustomizations): Result<Unit> {
        return writeMutex.withLock {
            try {
                val validationResult = customizations.validate()
                if (!validationResult.isSuccess) {
                    return Result.failure(
                        IllegalArgumentException(validationResult.errorMessage ?: "Invalid customizations")
                    )
                }
                
                context.widgetCustomizationDataStore.edit { prefs ->
                    // Main widget
                    prefs[PreferencesKeys.MAIN_BACKGROUND_COLOR] = customizations.mainWidget.backgroundColor.key
                    prefs[PreferencesKeys.MAIN_TEXT_CONTRAST] = customizations.mainWidget.textContrast.key
                    prefs[PreferencesKeys.MAIN_SENTENCES_PER_DAY] = customizations.mainWidget.sentencesPerDay
                    
                    // Bookmarks widget
                    prefs[PreferencesKeys.BOOKMARKS_BACKGROUND_COLOR] = customizations.bookmarksWidget.backgroundColor.key
                    prefs[PreferencesKeys.BOOKMARKS_TEXT_CONTRAST] = customizations.bookmarksWidget.textContrast.key
                    prefs[PreferencesKeys.BOOKMARKS_SENTENCES_PER_DAY] = customizations.bookmarksWidget.sentencesPerDay
                    
                    // Hero widget
                    prefs[PreferencesKeys.HERO_BACKGROUND_COLOR] = customizations.heroWidget.backgroundColor.key
                    prefs[PreferencesKeys.HERO_TEXT_CONTRAST] = customizations.heroWidget.textContrast.key
                    prefs[PreferencesKeys.HERO_SENTENCES_PER_DAY] = customizations.heroWidget.sentencesPerDay
                }
                
                // Trigger updates for all widget types using new helper
                com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerWidgetUpdate(context, WidgetType.MAIN)
                com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerWidgetUpdate(context, WidgetType.BOOKMARKS)
                com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerWidgetUpdate(context, WidgetType.HERO)
                
                // Reschedule worker with new frequency settings
                try {
                    SentenceDeliveryWorker.scheduleWork(context)
                    Log.d(TAG, "Worker rescheduled with updated frequency settings")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to reschedule worker after all customizations update", e)
                    // Don't fail the entire operation if worker scheduling fails
                }
                
                Log.d(TAG, "All widget customizations updated successfully")
                Result.success(Unit)
                
            } catch (e: IOException) {
                Log.e(TAG, "IO error updating all widget customizations", e)
                Result.failure(Exception("Failed to save customizations due to IO error", e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error updating all widget customizations", e)
                Result.failure(Exception("Failed to save customizations: ${e.message}", e))
            }
        }
    }
    
    /**
     * Get current customizations synchronously.
     */
    suspend fun getCurrentCustomizations(): Result<AllWidgetCustomizations> {
        return try {
            val customizations = allWidgetCustomizations.first()
            Result.success(customizations)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current customizations", e)
            Result.failure(Exception("Failed to read customizations", e))
        }
    }
    
    /**
     * Reset all customizations to defaults.
     */
    suspend fun resetToDefaults(): Result<Unit> {
        return safeWrite("reset to defaults") { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * Reset specific widget customization to default.
     */
    suspend fun resetWidgetToDefault(widgetType: WidgetType): Result<Unit> {
        val defaultCustomization = WidgetCustomization.createDefault(widgetType)
        return updateWidgetCustomization(defaultCustomization)
    }
    
    /**
     * Safe write operation with proper error handling.
     */
    private suspend fun safeWrite(
        operation: String,
        writeBlock: suspend (MutablePreferences) -> Unit
    ): Result<Unit> {
        return writeMutex.withLock {
            try {
                context.widgetCustomizationDataStore.edit(writeBlock)
                Log.d(TAG, "Successfully completed: $operation")
                Result.success(Unit)
            } catch (e: IOException) {
                Log.e(TAG, "IO error during $operation", e)
                Result.failure(Exception("IO error during $operation", e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during $operation", e)
                Result.failure(Exception("Failed to $operation", e))
            }
        }
    }
}