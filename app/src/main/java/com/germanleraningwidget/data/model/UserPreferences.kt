package com.germanleraningwidget.data.model

/**
 * Data class representing user preferences for the German learning app.
 * Contains all user-configurable settings for personalized learning experience.
 */
data class UserPreferences(
    val germanLevel: GermanLevel = GermanLevel.A1,
    val nativeLanguage: String = "English",
    val selectedTopics: Set<String> = emptySet(),
    val deliveryFrequency: DeliveryFrequency = DeliveryFrequency.DAILY,
    val isOnboardingCompleted: Boolean = false
) {
    /**
     * Validates the user preferences to ensure they are valid.
     * @return true if preferences are valid, false otherwise
     */
    fun isValid(): Boolean {
        val isValidLanguage = nativeLanguage.isNotBlank()
        val hasTopics = selectedTopics.isNotEmpty()
        val allTopicsValid = selectedTopics.all { it.isNotBlank() }
        
        android.util.Log.d("UserPreferences", "Validation - Language valid: $isValidLanguage, Has topics: $hasTopics, All topics valid: $allTopicsValid")
        android.util.Log.d("UserPreferences", "Language: '$nativeLanguage', Topics: $selectedTopics")
        
        return isValidLanguage && hasTopics && allTopicsValid
    }
    
    /**
     * Creates a copy with safe defaults if current values are invalid.
     */
    fun withSafeDefaults(): UserPreferences {
        val safeLang = if (nativeLanguage.isBlank()) "English" else nativeLanguage.trim()
        val safeTopics = if (selectedTopics.isEmpty()) {
            setOf("Daily Life")
        } else {
            selectedTopics.filter { it.isNotBlank() }.map { it.trim() }.toSet()
        }
        
        android.util.Log.d("UserPreferences", "WithSafeDefaults - Original: lang='$nativeLanguage', topics=$selectedTopics")
        android.util.Log.d("UserPreferences", "WithSafeDefaults - Safe: lang='$safeLang', topics=$safeTopics")
        
        return copy(
            nativeLanguage = safeLang,
            selectedTopics = safeTopics
        )
    }
}

/**
 * Enum representing different German proficiency levels.
 */
enum class GermanLevel(val displayName: String) {
    A1("A1 - Beginner"),
    A2("A2 - Elementary"),
    B1("B1 - Intermediate"),
    B2("B2 - Upper Intermediate"),
    C1("C1 - Advanced"),
    C2("C2 - Mastery");
    
    companion object {
        /**
         * Safely converts a string to GermanLevel, returns A1 as default if invalid.
         */
        fun fromString(value: String?): GermanLevel {
            return try {
                valueOf(value ?: "A1")
            } catch (e: IllegalArgumentException) {
                A1
            }
        }
    }
}

/**
 * Enum representing different delivery frequencies for German sentences.
 */
enum class DeliveryFrequency(val displayName: String, val hours: Long) {
    EVERY_30_MINUTES("Every 30 minutes", 0), // Special case: 30 minutes
    EVERY_HOUR("Every hour", 1),
    EVERY_2_HOURS("Every 2 hours", 2),
    EVERY_4_HOURS("Every 4 hours", 4),
    EVERY_6_HOURS("Every 6 hours", 6),
    EVERY_12_HOURS("Every 12 hours", 12),
    DAILY("Daily", 24);
    
    companion object {
        fun fromDisplayName(displayName: String): DeliveryFrequency {
            return values().find { it.displayName == displayName } ?: EVERY_HOUR
        }
        
        fun fromString(value: String?): DeliveryFrequency {
            return try {
                valueOf(value ?: "DAILY")
            } catch (e: IllegalArgumentException) {
                DAILY
            }
        }
    }
} 