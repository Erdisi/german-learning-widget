package com.germanleraningwidget.data.model

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * User preferences for the German Learning Widget.
 * 
 * This data class holds all user-configurable settings including German level,
 * selected topics, delivery frequency, and UI preferences. Designed for optimal
 * performance with immutable data structures and cached computations.
 * 
 * Thread Safety: Immutable data class is inherently thread-safe
 * Performance: Lazy evaluation and cached computations for expensive operations
 * Validation: Built-in validation methods with comprehensive error handling
 */
data class UserPreferences(
    // Core Learning Settings - UPDATED FOR MULTI-LEVEL SUPPORT
    val selectedGermanLevels: Set<String> = setOf("A1"), // Multi-level selection
    val primaryGermanLevel: String = "A1", // Primary/main level for progression
    val selectedTopics: Set<String> = setOf("Daily Life"),
    val deliveryFrequency: DeliveryFrequency = DeliveryFrequency.DAILY,
    
    // Learning Behavior Settings
    val enableSmartDelivery: Boolean = true,
    val enableProgressTracking: Boolean = true,
    val enableAdaptiveDifficulty: Boolean = true,
    val enableContextualHints: Boolean = true,
    
    // UI and Experience Settings
    val enableAnimations: Boolean = true,
    val enableSoundEffects: Boolean = false,
    val enableHapticFeedback: Boolean = true,
    val preferDarkMode: Boolean = false,
    
    // Widget Appearance Settings - Using simple types for now
    val widgetTextSize: String = "MEDIUM",
    val widgetTheme: String = "LIGHT",
    val showProgressIndicator: Boolean = true,
    val showLevelBadge: Boolean = true,
    
    // Privacy and Data Settings
    val enableAnalytics: Boolean = true,
    val enableCrashReporting: Boolean = true,
    val enableUsageStatistics: Boolean = true,
    
    // Advanced Learning Settings
    val maxDailyWidgets: Int = 10,
    val enableReviewMode: Boolean = true,
    val enableDifficultyProgression: Boolean = true,
    val preferredLearningTime: String = "ANY_TIME",
    
    // Onboarding and Setup
    val isOnboardingCompleted: Boolean = false,
    val onboardingVersion: Int = 1,
    val hasSeenWidgetTutorial: Boolean = false,
    val hasRatedApp: Boolean = false,
    
    // Performance and Caching
    val enablePreloading: Boolean = true,
    val cacheSize: String = "MEDIUM",
    val enableOfflineMode: Boolean = true,
    
    // Experimental Features
    val enableBetaFeatures: Boolean = false,
    val enableAdvancedStatistics: Boolean = false,
    val enableCustomTopics: Boolean = false
) {
    
    // BACKWARD COMPATIBILITY PROPERTIES
    /**
     * Backward compatibility property - returns primary level as single string
     * @deprecated Use selectedGermanLevels instead
     */
    @Deprecated("Use selectedGermanLevels and primaryGermanLevel instead")
    val germanLevel: String get() = primaryGermanLevel
    
    /**
     * Check if user has multiple levels selected
     */
    val hasMultipleLevels: Boolean get() = selectedGermanLevels.size > 1
    
    /**
     * Get level distribution weights for sentence selection
     */
    fun getLevelWeights(): Map<String, Float> {
        return selectedGermanLevels.associateWith { level ->
            when {
                level == primaryGermanLevel -> 1.5f // Primary level gets more weight
                selectedGermanLevels.size == 1 -> 1.0f // Single level gets full weight
                else -> 1.0f // Multiple levels get equal weight
            }
        }
    }
    
    /**
     * Get sentence distribution across selected levels
     */
    fun getSentenceDistribution(dailyTarget: Int = recommendedDailySentences): Map<String, Int> {
        val totalWeight = getLevelWeights().values.sum()
        
        return selectedGermanLevels.associateWith { level ->
            val weight = getLevelWeights()[level] ?: 1.0f
            ((dailyTarget * weight / totalWeight).toInt()).coerceAtLeast(1)
        }
    }
    
    // Note: Caching removed to avoid serialization issues with DataStore
    // Validation is still optimized through efficient algorithms
    
    /**
     * Validates the user preferences with optimized performance.
     * @return ValidationResult containing success status and error message if invalid
     */
    fun validate(): ValidationResult {
        return try {
            if (selectedTopics.isEmpty()) {
                return ValidationResult.Error("At least one topic must be selected")
            }
            
            if (selectedGermanLevels.isEmpty()) {
                return ValidationResult.Error("At least one German level must be selected")
            }
            
            if (primaryGermanLevel !in selectedGermanLevels) {
                return ValidationResult.Error("Primary German level must be one of the selected levels")
            }
            
            ValidationResult.Success
        } catch (e: Exception) {
            ValidationResult.Error("Invalid preferences: ${e.message}")
        }
    }
    
    /**
     * Checks if preferences are valid with optimized performance.
     */
    fun isValid(): Boolean = validate().isSuccess
    
    /**
     * Creates a copy with safe defaults if current values are invalid.
     * This method ensures the returned preferences are always valid.
     * Optimized to avoid unnecessary object creation.
     */
    fun withSafeDefaults(): UserPreferences {
        val safeLevels = if (selectedGermanLevels.isEmpty() || selectedGermanLevels.any { it.isBlank() }) {
            setOf("A1")
        } else {
            selectedGermanLevels.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }
        
        val safePrimaryLevel = if (primaryGermanLevel.isBlank() || primaryGermanLevel !in safeLevels) {
            safeLevels.minByOrNull { level ->
                when (level) {
                    "A1" -> 1; "A2" -> 2; "B1" -> 3; "B2" -> 4; "C1" -> 5; "C2" -> 6
                    else -> 1
                }
            } ?: "A1"
        } else {
            primaryGermanLevel.trim()
        }
        
        val safeTopics = if (selectedTopics.isEmpty() || selectedTopics.any { it.isBlank() }) {
            setOf("Daily Life")
        } else {
            // Use existing set if all topics are valid
            val cleanedTopics = selectedTopics.asSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
            
            if (cleanedTopics.size == selectedTopics.size) {
                selectedTopics // Use existing set to avoid object creation
            } else {
                cleanedTopics
            }
        }
        
        // Return same instance if no changes needed
        return if (safeLevels == selectedGermanLevels && 
                  safePrimaryLevel == primaryGermanLevel && 
                  safeTopics === selectedTopics) {
            this
        } else {
            copy(
                selectedGermanLevels = safeLevels,
                primaryGermanLevel = safePrimaryLevel,
                selectedTopics = safeTopics
            )
        }
    }
    
    /**
     * Creates a normalized copy with trimmed strings and validated data.
     * Optimized for performance by avoiding unnecessary work.
     */
    fun normalized(): UserPreferences {
        val trimmedLevels = selectedGermanLevels.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val trimmedPrimaryLevel = primaryGermanLevel.trim()
        val cleanedTopics = selectedTopics.asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
        
        return if (trimmedLevels.size == selectedGermanLevels.size && 
                  trimmedPrimaryLevel == primaryGermanLevel &&
                  cleanedTopics.size == selectedTopics.size) {
            this // Return same instance if no normalization needed
        } else {
            copy(
                selectedGermanLevels = trimmedLevels,
                primaryGermanLevel = trimmedPrimaryLevel,
                selectedTopics = cleanedTopics
            )
        }
    }
    
    /**
     * Get a summary string for debugging and logging.
     */
    fun getSummary(): String = buildString {
        append("UserPreferences(")
        append("levels=${selectedGermanLevels.joinToString(",")}, ")
        append("primary=$primaryGermanLevel, ")
        append("topics=${selectedTopics.size}, ")
        append("frequency=${deliveryFrequency.name}, ")
        append("onboarded=$isOnboardingCompleted")
        append(")")
    }
    
    /**
     * Check if preferences indicate an advanced user.
     */
    val isAdvancedUser: Boolean get() = selectedGermanLevels.any { it in listOf("B2", "C1", "C2") }
    
    /**
     * Get recommended sentence count per day based on level and frequency.
     */
    val recommendedDailySentences: Int get() {
        val baseCount = when {
            selectedGermanLevels.any { it in listOf("C1", "C2") } -> 8
            selectedGermanLevels.any { it in listOf("B1", "B2") } -> 6
            else -> 4
        }
        
        return baseCount * when (deliveryFrequency) {
            DeliveryFrequency.EVERY_30_MINUTES -> 2
            DeliveryFrequency.EVERY_HOUR -> 1
            else -> 1
        }
    }
    
    companion object {
        private const val DEFAULT_NATIVE_LANGUAGE = "English"
        private val DEFAULT_TOPICS = setOf("Daily Life")
        
        // Cache for expensive validations
        private val validationCache = ConcurrentHashMap<UserPreferences, ValidationResult>()
        
        /**
         * Creates UserPreferences with validation and performance optimization.
         */
        fun createSafe(
            selectedGermanLevels: Set<String> = setOf("A1"),
            primaryGermanLevel: String = "A1",
            selectedTopics: Set<String> = setOf("Daily Life"),
            deliveryFrequency: DeliveryFrequency = DeliveryFrequency.DAILY,
            isOnboardingCompleted: Boolean = false
        ): UserPreferences {
            return UserPreferences(
                selectedGermanLevels = selectedGermanLevels,
                primaryGermanLevel = primaryGermanLevel,
                selectedTopics = selectedTopics,
                deliveryFrequency = deliveryFrequency,
                isOnboardingCompleted = isOnboardingCompleted
            ).withSafeDefaults()
        }
        
        /**
         * Migration helper: Create from old single-level format
         */
        fun migrateFromSingleLevel(
            germanLevel: String,
            selectedTopics: Set<String> = setOf("Daily Life"),
            deliveryFrequency: DeliveryFrequency = DeliveryFrequency.DAILY,
            isOnboardingCompleted: Boolean = false
        ): UserPreferences {
            return createSafe(
                selectedGermanLevels = setOf(germanLevel),
                primaryGermanLevel = germanLevel,
                selectedTopics = selectedTopics,
                deliveryFrequency = deliveryFrequency,
                isOnboardingCompleted = isOnboardingCompleted
            )
        }
    }
    
    /**
     * Validation result sealed class for type-safe error handling.
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val errorText: String) : ValidationResult()
        
        val isSuccess: Boolean get() = this is Success
        val errorMessage: String? get() = (this as? Error)?.errorText
    }
}

/**
 * Enhanced enum representing different delivery frequencies.
 * Optimized with better performance characteristics and utility methods.
 */
enum class DeliveryFrequency(
    val displayName: String, 
    val hours: Long,
    val minutes: Long = 0
) {
    EVERY_30_MINUTES("Every 30 minutes", 0, 30),
    EVERY_HOUR("Every hour", 1),
    EVERY_2_HOURS("Every 2 hours", 2),
    EVERY_4_HOURS("Every 4 hours", 4),
    EVERY_6_HOURS("Every 6 hours", 6),
    EVERY_12_HOURS("Every 12 hours", 12),
    DAILY("Daily", 24);
    
    /**
     * Gets the total duration in minutes.
     * Cached for performance.
     */
    val totalMinutes: Long by lazy { hours * 60 + minutes }
    
    /**
     * Gets the total duration in milliseconds.
     * Cached for performance.
     */
    val totalMilliseconds: Long by lazy { totalMinutes * 60 * 1000 }
    
    /**
     * Checks if this is a high-frequency delivery (less than 2 hours).
     */
    val isHighFrequency: Boolean get() = totalMinutes < 120
    
    /**
     * Checks if this is a low-frequency delivery (12+ hours).
     */
    val isLowFrequency: Boolean get() = totalMinutes >= 720
    
    /**
     * Get frequency category for analytics.
     */
    val category: FrequencyCategory get() = when {
        isHighFrequency -> FrequencyCategory.HIGH
        isLowFrequency -> FrequencyCategory.LOW
        else -> FrequencyCategory.MEDIUM
    }
    
    enum class FrequencyCategory {
        HIGH, MEDIUM, LOW
    }
    
    companion object {
        // Cached map for faster display name lookups
        private val displayNameMap = values().associateBy { it.displayName }
        
        // Cached lists for UI
        private val highFrequencyOptions = values().filter { it.isHighFrequency }
        private val mediumFrequencyOptions = values().filter { !it.isHighFrequency && !it.isLowFrequency }
        private val lowFrequencyOptions = values().filter { it.isLowFrequency }
        
        /**
         * Safely converts display name to DeliveryFrequency with caching.
         */
        fun fromDisplayName(displayName: String?): DeliveryFrequency {
            if (displayName.isNullOrBlank()) return DAILY
            
            return displayNameMap[displayName.trim()] ?: run {
                Log.w("DeliveryFrequency", "Invalid display name: '$displayName', defaulting to DAILY")
                DAILY
            }
        }
        
        /**
         * Safely converts string to DeliveryFrequency.
         */
        fun fromString(value: String?): DeliveryFrequency {
            if (value.isNullOrBlank()) return DAILY
            
            return try {
                valueOf(value.uppercase().trim())
            } catch (e: IllegalArgumentException) {
                Log.w("DeliveryFrequency", "Invalid frequency string: '$value', defaulting to DAILY")
                DAILY
            }
        }
        
        /**
         * Get frequency options categorized by intensity.
         */
        fun getHighFrequencyOptions(): List<DeliveryFrequency> = highFrequencyOptions
        fun getMediumFrequencyOptions(): List<DeliveryFrequency> = mediumFrequencyOptions
        fun getLowFrequencyOptions(): List<DeliveryFrequency> = lowFrequencyOptions
        
        /**
         * Gets all frequencies as a list for UI display.
         */
        fun getAllFrequencies(): List<DeliveryFrequency> = values().toList()
    }
}

/**
 * Available topics for German learning.
 * Optimized with Set-based lookups for performance.
 */
object AvailableTopics {
    val topics = listOf(
        "Daily Life", "Food", "Travel", "Work", "Family", "Health", 
        "Education", "Technology", "Culture", "Sports", "Weather",
        "Entertainment", "Business", "Science", "Politics", "Art"
    )
    
    // Pre-computed set for O(1) lookups
    val topicsSet: Set<String> = topics.toSet()
    
    /**
     * Check if a topic is valid.
     */
    fun isValidTopic(topic: String): Boolean = topicsSet.contains(topic)
    
    /**
     * Get topics suitable for a specific level.
     */
    fun getTopicsForLevel(level: String): List<String> = when (level) {
        "C1", "C2" -> topics
        "B1", "B2" -> topics.take(12)
        else -> topics.take(8)
    }
}

/**
 * Available languages for native language selection.
 * Optimized with Set-based lookups.
 */
object AvailableLanguages {
    val languages = listOf(
        "English", "Spanish", "French", "Italian", "Portuguese", 
        "Dutch", "Russian", "Chinese", "Japanese", "Arabic"
    )
    
    // Pre-computed set for O(1) lookups
    val languagesSet: Set<String> = languages.toSet()
    
    /**
     * Check if a language is valid.
     */
    fun isValidLanguage(language: String): Boolean = languagesSet.contains(language)
} 