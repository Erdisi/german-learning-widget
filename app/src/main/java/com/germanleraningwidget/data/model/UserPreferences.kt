package com.germanleraningwidget.data.model

import android.util.Log
import java.util.concurrent.ConcurrentHashMap



/**
 * User preferences for the German Learning Widget.
 * 
 * This data class holds all user-configurable settings including German level,
 * selected topics, and UI preferences. Designed for optimal
 * performance with immutable data structures and cached computations.
 * 
 * Thread Safety: Immutable data class is inherently thread-safe
 * Performance: Lazy evaluation and cached computations for expensive operations
 * Validation: Built-in validation methods with comprehensive error handling
 */
data class UserPreferences(
    // Core Learning Settings - UPDATED FOR MULTI-LEVEL SUPPORT
    val selectedGermanLevels: Set<String> = emptySet(), // No default selection for onboarding
    val primaryGermanLevel: String = "", // No default primary level for onboarding
    val selectedTopics: Set<String> = setOf("Daily Life"),
    
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
            
            // Only require levels if onboarding is completed
            if (isOnboardingCompleted && selectedGermanLevels.isEmpty()) {
                return ValidationResult.Error("At least one German level must be selected")
            }
            
            // Only validate primary level if levels are selected
            if (selectedGermanLevels.isNotEmpty() && primaryGermanLevel !in selectedGermanLevels) {
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
            // Only apply A1 fallback if onboarding is completed (for data corruption recovery)
            if (isOnboardingCompleted) setOf("A1") else emptySet()
        } else {
            selectedGermanLevels.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }
        
        val safePrimaryLevel = if (primaryGermanLevel.isBlank() || primaryGermanLevel !in safeLevels) {
            safeLevels.minByOrNull { level ->
                when (level) {
                    "A1" -> 1; "A2" -> 2; "B1" -> 3; "B2" -> 4; "C1" -> 5; "C2" -> 6
                    else -> 1
                }
            } ?: ""
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
        append("onboarded=$isOnboardingCompleted")
        append(")")
    }
    
    /**
     * Check if preferences indicate an advanced user.
     */
    val isAdvancedUser: Boolean get() = selectedGermanLevels.any { it in listOf("B2", "C1", "C2") }
    
    /**
     * Get recommended sentence count per day based on level.
     */
    val recommendedDailySentences: Int get() {
        return when {
            selectedGermanLevels.any { it in listOf("C1", "C2") } -> 8
            selectedGermanLevels.any { it in listOf("B1", "B2") } -> 6
            else -> 4
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
            selectedGermanLevels: Set<String> = emptySet(),
            primaryGermanLevel: String = "",
            selectedTopics: Set<String> = setOf("Daily Life"),
            isOnboardingCompleted: Boolean = false
        ): UserPreferences {
            return UserPreferences(
                selectedGermanLevels = selectedGermanLevels,
                primaryGermanLevel = primaryGermanLevel,
                selectedTopics = selectedTopics,
                isOnboardingCompleted = isOnboardingCompleted
            ).withSafeDefaults()
        }
        
        /**
         * Migration helper: Create from old single-level format
         */
        fun migrateFromSingleLevel(
            germanLevel: String,
            selectedTopics: Set<String> = setOf("Daily Life"),
            isOnboardingCompleted: Boolean = false
        ): UserPreferences {
            return createSafe(
                selectedGermanLevels = setOf(germanLevel),
                primaryGermanLevel = germanLevel,
                selectedTopics = selectedTopics,
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
 * Available topics for German learning.
 * Unified list that works across onboarding, preferences, and sentence filtering.
 */
object AvailableTopics {
    val topics = listOf(
        // Core topics available in sample sentences
        "Daily Life", "Greetings", "Introductions", "Food", "Travel", 
        "Weather", "Health", "Work", "Education", "Technology", 
        "Entertainment", "Sports", "Language",
        // Additional advanced topics for comprehensive learning
        "Family", "Culture", "Business", "Science", "Politics", "Art"
    )
    
    // Pre-computed set for O(1) lookups
    val topicsSet: Set<String> = topics.toSet()
    
    /**
     * Check if a topic is valid.
     */
    fun isValidTopic(topic: String): Boolean = topicsSet.contains(topic)
    
    /**
     * Get topics suitable for a specific level.
     * Basic levels get core topics, advanced levels get all topics.
     */
    fun getTopicsForLevel(level: String): List<String> = when (level) {
        "C1", "C2" -> topics // Advanced users get all topics
        "B1", "B2" -> topics.take(16) // Intermediate users get most topics
        else -> topics.take(13) // Beginners get core topics only
    }
    
    /**
     * Get topics that have sample sentences available.
     * These are the topics that will actually work for sentence filtering.
     */
    fun getAvailableTopicsInSentences(): List<String> = listOf(
        "Daily Life", "Greetings", "Introductions", "Food", "Travel", 
        "Weather", "Health", "Work", "Education", "Technology", 
        "Entertainment", "Sports", "Language"
    )
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