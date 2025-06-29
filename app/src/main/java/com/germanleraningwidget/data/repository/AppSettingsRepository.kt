package com.germanleraningwidget.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.germanleraningwidget.data.model.AppSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private val Context.appSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Repository for managing app-wide settings with DataStore.
 * 
 * Provides thread-safe operations for:
 * - Reading and writing app settings
 * - System integration (notifications, haptic feedback)
 * - Settings validation and defaults
 * 
 * Thread Safety: All operations are thread-safe using Mutex
 * Error Handling: Comprehensive error handling with fallback to defaults
 */
class AppSettingsRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    companion object {
        private const val TAG = "AppSettingsRepo"
        
        // Singleton instance
        @Volatile
        private var INSTANCE: AppSettingsRepository? = null
        
        fun getInstance(context: Context): AppSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppSettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // Mutex for protecting write operations
    private val writeMutex = Mutex()
    
    /**
     * Preference keys for app settings
     */
    private object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LEARNING_REMINDERS_ENABLED = booleanPreferencesKey("learning_reminders_enabled")
        val IS_DARK_MODE_ENABLED = booleanPreferencesKey("is_dark_mode_enabled")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val LAST_VERSION_CODE = intPreferencesKey("last_version_code")
    }
    
    /**
     * Reactive flow of app settings with error handling.
     */
    val appSettings: Flow<AppSettings> = context.appSettingsDataStore.data
        .catch { exception ->
            Log.e(TAG, "Error reading app settings", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            try {
                mapPreferencesToAppSettings(preferences)
            } catch (e: Exception) {
                Log.e(TAG, "Error mapping app settings", e)
                AppSettings.createDefault().withSafeDefaults()
            }
        }
    
    /**
     * Map DataStore preferences to AppSettings.
     */
    private fun mapPreferencesToAppSettings(preferences: Preferences): AppSettings {
        return AppSettings(
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            learningRemindersEnabled = preferences[PreferencesKeys.LEARNING_REMINDERS_ENABLED] ?: true,
            isDarkModeEnabled = preferences[PreferencesKeys.IS_DARK_MODE_ENABLED],
            isFirstLaunch = preferences[PreferencesKeys.IS_FIRST_LAUNCH] ?: true,
            lastVersionCode = preferences[PreferencesKeys.LAST_VERSION_CODE] ?: 0
        ).withSafeDefaults()
    }
    
    /**
     * Update notifications enabled setting.
     */
    suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return safeWrite("update notifications enabled") { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
            preferences[PreferencesKeys.LEARNING_REMINDERS_ENABLED] = enabled
        }
    }
    
    /**
     * Update learning reminders setting.
     */
    suspend fun updateLearningRemindersEnabled(enabled: Boolean): Result<Unit> {
        return safeWrite("update learning reminders") { preferences ->
            preferences[PreferencesKeys.LEARNING_REMINDERS_ENABLED] = enabled
        }
    }
    
    /**
     * Update dark mode setting.
     */
    suspend fun updateDarkModeEnabled(enabled: Boolean?): Result<Unit> {
        return safeWrite("update dark mode") { preferences ->
            if (enabled != null) {
                preferences[PreferencesKeys.IS_DARK_MODE_ENABLED] = enabled
            } else {
                preferences.remove(PreferencesKeys.IS_DARK_MODE_ENABLED)
            }
        }
    }
    
    /**
     * Mark first launch as completed.
     */
    suspend fun markFirstLaunchCompleted(): Result<Unit> {
        return safeWrite("mark first launch completed") { preferences ->
            preferences[PreferencesKeys.IS_FIRST_LAUNCH] = false
        }
    }
    
    /**
     * Update last version code.
     */
    suspend fun updateLastVersionCode(versionCode: Int): Result<Unit> {
        return safeWrite("update last version code") { preferences ->
            preferences[PreferencesKeys.LAST_VERSION_CODE] = versionCode
        }
    }
    
    /**
     * Update all app settings atomically.
     */
    suspend fun updateAppSettings(settings: AppSettings): Result<Unit> {
        return writeMutex.withLock {
            try {
                val validationResult = settings.validate()
                if (!validationResult.isSuccess) {
                    return Result.failure(
                        IllegalArgumentException(validationResult.errorMessage ?: "Invalid settings")
                    )
                }
                
                val safeSettings = settings.withSafeDefaults()
                
                context.appSettingsDataStore.edit { prefs ->
                    prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = safeSettings.notificationsEnabled
                    prefs[PreferencesKeys.LEARNING_REMINDERS_ENABLED] = safeSettings.learningRemindersEnabled
                    
                    if (safeSettings.isDarkModeEnabled != null) {
                        prefs[PreferencesKeys.IS_DARK_MODE_ENABLED] = safeSettings.isDarkModeEnabled
                    } else {
                        prefs.remove(PreferencesKeys.IS_DARK_MODE_ENABLED)
                    }
                    
                    prefs[PreferencesKeys.IS_FIRST_LAUNCH] = safeSettings.isFirstLaunch
                    prefs[PreferencesKeys.LAST_VERSION_CODE] = safeSettings.lastVersionCode
                }
                
                Log.d(TAG, "App settings updated successfully")
                Result.success(Unit)
                
            } catch (e: IOException) {
                Log.e(TAG, "IO error updating app settings", e)
                Result.failure(Exception("Failed to save settings due to IO error", e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error updating app settings", e)
                Result.failure(Exception("Failed to save settings: ${e.message}", e))
            }
        }
    }
    
    /**
     * Get current settings synchronously.
     */
    suspend fun getCurrentSettings(): Result<AppSettings> {
        return try {
            val settings = appSettings.first()
            Result.success(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current settings", e)
            Result.failure(Exception("Failed to read settings", e))
        }
    }
    
    /**
     * Check if notifications are enabled at system level.
     */
    fun areNotificationsEnabledInSystem(): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    /**
     * Open system notification settings for the app.
     */
    fun openSystemNotificationSettings(): Intent {
        return Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    /**
     * Get app version information.
     */
    fun getAppVersion(): AppVersionInfo {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            AppVersionInfo(
                versionName = packageInfo.versionName ?: "Unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toInt()
                } else {
                    // Legacy API for versions below Android 9 (API 28)
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode
                },
                packageName = context.packageName
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app version", e)
            AppVersionInfo(
                versionName = "Unknown",
                versionCode = 0,
                packageName = context.packageName
            )
        }
    }
    
    /**
     * Reset all settings to defaults.
     */
    suspend fun resetToDefaults(): Result<Unit> {
        return safeWrite("reset to defaults") { preferences ->
            preferences.clear()
        }
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
                context.appSettingsDataStore.edit(writeBlock)
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
    
    /**
     * App version information data class.
     */
    data class AppVersionInfo(
        val versionName: String,
        val versionCode: Int,
        val packageName: String
    )
}