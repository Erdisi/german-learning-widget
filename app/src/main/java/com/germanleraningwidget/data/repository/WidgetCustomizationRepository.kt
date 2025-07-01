package com.germanleraningwidget.data.repository

import android.content.Context
import com.germanleraningwidget.util.DebugUtils
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.germanleraningwidget.data.model.*
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
 * Simplified for fixed schedule approach:
 * - No sentences per day configuration (fixed at 10/day)
 * - No dynamic worker scheduling (fixed 90-minute intervals)
 * - Only color and contrast customization
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
     * Preference keys for widget customizations.
     * Simplified to only include color and contrast settings.
     */
    private object PreferencesKeys {
        // Main Widget
        val MAIN_BACKGROUND_COLOR = stringPreferencesKey("main_background_color")
        val MAIN_TEXT_CONTRAST = stringPreferencesKey("main_text_contrast")
        
        // Bookmarks Widget
        val BOOKMARKS_BACKGROUND_COLOR = stringPreferencesKey("bookmarks_background_color")
        val BOOKMARKS_TEXT_CONTRAST = stringPreferencesKey("bookmarks_text_contrast")
        
        // Hero Widget
        val HERO_BACKGROUND_COLOR = stringPreferencesKey("hero_background_color")
        val HERO_TEXT_CONTRAST = stringPreferencesKey("hero_text_contrast")
    }
    
    /**
     * Reactive flow of all widget customizations with error handling.
     */
    val allWidgetCustomizations: Flow<AllWidgetCustomizations> = context.widgetCustomizationDataStore.data
        .catch { exception ->
            DebugUtils.logError(TAG, "Error reading widget customizations", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            try {
                mapPreferencesToCustomizations(preferences)
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Error mapping widget customizations", e)
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
     * Simplified to only handle color and contrast settings.
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
                )
            ),
            bookmarksWidget = WidgetCustomization(
                widgetType = WidgetType.BOOKMARKS,
                backgroundColor = WidgetBackgroundColor.fromKey(
                    preferences[PreferencesKeys.BOOKMARKS_BACKGROUND_COLOR] ?: WidgetBackgroundColor.ORANGE.key
                ),
                textContrast = WidgetTextContrast.fromKey(
                    preferences[PreferencesKeys.BOOKMARKS_TEXT_CONTRAST] ?: WidgetTextContrast.NORMAL.key
                )
            ),
            heroWidget = WidgetCustomization(
                widgetType = WidgetType.HERO,
                backgroundColor = WidgetBackgroundColor.fromKey(
                    preferences[PreferencesKeys.HERO_BACKGROUND_COLOR] ?: WidgetBackgroundColor.NAVY.key
                ),
                textContrast = WidgetTextContrast.fromKey(
                    preferences[PreferencesKeys.HERO_TEXT_CONTRAST] ?: WidgetTextContrast.NORMAL.key
                )
            )
        )
    }
    
    /**
     * Update customization for a specific widget.
     * Simplified to only handle color and contrast updates.
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
                        }
                        WidgetType.BOOKMARKS -> {
                            prefs[PreferencesKeys.BOOKMARKS_BACKGROUND_COLOR] = customization.backgroundColor.key
                            prefs[PreferencesKeys.BOOKMARKS_TEXT_CONTRAST] = customization.textContrast.key
                        }
                        WidgetType.HERO -> {
                            prefs[PreferencesKeys.HERO_BACKGROUND_COLOR] = customization.backgroundColor.key
                            prefs[PreferencesKeys.HERO_TEXT_CONTRAST] = customization.textContrast.key
                        }
                    }
                }
                
                // CRITICAL FIX: Use immediate update with data to ensure widgets get fresh customization
                com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerImmediateWidgetUpdateWithData(context, customization.widgetType, customization)
                
                DebugUtils.logInfo(TAG, "Widget customization updated successfully for ${customization.widgetType.displayName}")
                Result.success(Unit)
                
            } catch (e: IOException) {
                DebugUtils.logError(TAG, "IO error updating widget customization", e)
                Result.failure(Exception("Failed to save customization due to IO error", e))
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Unexpected error updating widget customization", e)
                Result.failure(Exception("Failed to save customization: ${e.message}", e))
            }
        }
    }
    
    /**
     * Update all widget customizations atomically.
     * Simplified to only handle color and contrast updates.
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
                    
                    // Bookmarks widget
                    prefs[PreferencesKeys.BOOKMARKS_BACKGROUND_COLOR] = customizations.bookmarksWidget.backgroundColor.key
                    prefs[PreferencesKeys.BOOKMARKS_TEXT_CONTRAST] = customizations.bookmarksWidget.textContrast.key
                    
                    // Hero widget
                    prefs[PreferencesKeys.HERO_BACKGROUND_COLOR] = customizations.heroWidget.backgroundColor.key
                    prefs[PreferencesKeys.HERO_TEXT_CONTRAST] = customizations.heroWidget.textContrast.key
                }
                
                // CRITICAL FIX: Use immediate updates for all widget types to ensure fresh data
                com.germanleraningwidget.widget.WidgetCustomizationHelper.triggerImmediateAllWidgetUpdates(context)
                
                DebugUtils.logInfo(TAG, "All widget customizations updated successfully")
                Result.success(Unit)
                
            } catch (e: IOException) {
                DebugUtils.logError(TAG, "IO error updating all widget customizations", e)
                Result.failure(Exception("Failed to save customizations due to IO error", e))
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Unexpected error updating all widget customizations", e)
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
            DebugUtils.logError(TAG, "Error getting current customizations", e)
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
                DebugUtils.logInfo(TAG, "Successfully completed: $operation")
                Result.success(Unit)
            } catch (e: IOException) {
                DebugUtils.logError(TAG, "IO error during $operation", e)
                Result.failure(Exception("IO error during $operation", e))
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Unexpected error during $operation", e)
                Result.failure(Exception("Failed to $operation", e))
            }
        }
    }
}