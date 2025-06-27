package com.germanleraningwidget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.germanleraningwidget.data.model.DeliveryFrequency
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {
    
    private object PreferencesKeys {
        val GERMAN_LEVEL = stringPreferencesKey("german_level")
        val NATIVE_LANGUAGE = stringPreferencesKey("native_language")
        val SELECTED_TOPICS = stringSetPreferencesKey("selected_topics")
        val DELIVERY_FREQUENCY = stringPreferencesKey("delivery_frequency")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    }
    
    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                germanLevel = GermanLevel.fromString(
                    preferences[PreferencesKeys.GERMAN_LEVEL]
                ),
                nativeLanguage = preferences[PreferencesKeys.NATIVE_LANGUAGE] ?: "English",
                selectedTopics = preferences[PreferencesKeys.SELECTED_TOPICS] ?: emptySet(),
                deliveryFrequency = DeliveryFrequency.fromString(
                    preferences[PreferencesKeys.DELIVERY_FREQUENCY]
                ),
                isOnboardingCompleted = preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] ?: false
            ).withSafeDefaults()
        }
        .catch { exception ->
            // If there's an error reading preferences, return default values
            emit(UserPreferences().withSafeDefaults())
        }
    
    suspend fun updateGermanLevel(level: GermanLevel) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GERMAN_LEVEL] = level.name
            }
        } catch (e: Exception) {
            // Handle error gracefully
        }
    }
    
    suspend fun updateNativeLanguage(language: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.NATIVE_LANGUAGE] = language.ifBlank { "English" }
            }
        } catch (e: Exception) {
            // Handle error gracefully
        }
    }
    
    suspend fun updateSelectedTopics(topics: Set<String>) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SELECTED_TOPICS] = topics.filter { it.isNotBlank() }.toSet()
            }
        } catch (e: Exception) {
            // Handle error gracefully
        }
    }
    
    suspend fun updateDeliveryFrequency(frequency: DeliveryFrequency) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DELIVERY_FREQUENCY] = frequency.name
            }
        } catch (e: Exception) {
            // Handle error gracefully
        }
    }
    
    suspend fun setOnboardingCompleted(completed: Boolean) {
        try {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_ONBOARDING_COMPLETED] = completed
            }
        } catch (e: Exception) {
            // Handle error gracefully
        }
    }
    
    suspend fun updateUserPreferences(preferences: UserPreferences) {
        try {
            val safePreferences = preferences.withSafeDefaults()
            android.util.Log.d("UserPreferencesRepo", "Attempting to save preferences: $safePreferences")
            
            // Validate the data before saving
            if (safePreferences.nativeLanguage.isBlank()) {
                throw IllegalArgumentException("Native language cannot be blank")
            }
            if (safePreferences.selectedTopics.isEmpty()) {
                throw IllegalArgumentException("At least one topic must be selected")
            }
            
            context.dataStore.edit { prefs ->
                prefs[PreferencesKeys.GERMAN_LEVEL] = safePreferences.germanLevel.name
                prefs[PreferencesKeys.NATIVE_LANGUAGE] = safePreferences.nativeLanguage
                prefs[PreferencesKeys.SELECTED_TOPICS] = safePreferences.selectedTopics.filter { it.isNotBlank() }.toSet()
                prefs[PreferencesKeys.DELIVERY_FREQUENCY] = safePreferences.deliveryFrequency.name
                prefs[PreferencesKeys.IS_ONBOARDING_COMPLETED] = safePreferences.isOnboardingCompleted
            }
            
            android.util.Log.d("UserPreferencesRepo", "Preferences saved successfully")
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("UserPreferencesRepo", "Validation error: ${e.message}")
            throw Exception("Validation error: ${e.message}", e)
        } catch (e: Exception) {
            // Log the specific error for debugging
            android.util.Log.e("UserPreferencesRepo", "DataStore error: ${e.message}", e)
            throw Exception("Failed to save preferences: ${e.message}", e)
        }
    }
    
    /**
     * Clears all user preferences and resets to defaults.
     */
    suspend fun clearPreferences() {
        try {
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
        } catch (e: Exception) {
            // Handle error gracefully
        }
    }
    
    /**
     * Test DataStore access by performing a simple write/read operation.
     */
    suspend fun testDataStoreAccess(): Boolean {
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
            android.util.Log.d("UserPreferencesRepo", "DataStore test ${if (success) "PASSED" else "FAILED"}")
            success
        } catch (e: Exception) {
            android.util.Log.e("UserPreferencesRepo", "DataStore test FAILED", e)
            false
        }
    }
} 