package com.germanleraningwidget.data.model

import androidx.compose.ui.graphics.Color

/**
 * German Learning Level System
 * 
 * Implements CEFR (Common European Framework of Reference for Languages) levels
 * with comprehensive customization and multi-level selection support.
 */

/**
 * German proficiency levels following CEFR standards
 */
enum class GermanLevel(
    val key: String,
    val displayName: String,
    val fullName: String,
    val description: String,
    val color: Color,
    val difficultyOrder: Int,
    val estimatedHours: IntRange,
    val keyFeatures: List<String>
) {
    A1(
        key = "a1",
        displayName = "A1",
        fullName = "Beginner (A1)",
        description = "Basic everyday expressions and very simple phrases",
        color = Color(0xFF4CAF50), // Green for beginner
        difficultyOrder = 1,
        estimatedHours = 60..150,
        keyFeatures = listOf(
            "Basic greetings and introductions",
            "Simple present tense",
            "Numbers, time, and dates",
            "Family and personal information",
            "Basic shopping and ordering"
        )
    ),
    
    A2(
        key = "a2",
        displayName = "A2",
        fullName = "Elementary (A2)",
        description = "Simple communication in routine tasks and familiar topics",
        color = Color(0xFF8BC34A), // Light green for elementary
        difficultyOrder = 2,
        estimatedHours = 150..300,
        keyFeatures = listOf(
            "Past and future tenses",
            "Describing experiences and events",
            "Making appointments and plans",
            "Expressing opinions simply",
            "Travel and transportation"
        )
    ),
    
    B1(
        key = "b1",
        displayName = "B1",
        fullName = "Intermediate (B1)",
        description = "Clear communication on familiar matters and personal interests",
        color = Color(0xFFFF9800), // Orange for intermediate
        difficultyOrder = 3,
        estimatedHours = 300..500,
        keyFeatures = listOf(
            "Complex sentence structures",
            "Expressing wishes and hypotheticals",
            "Professional communication",
            "Cultural topics and traditions",
            "Problem-solving discussions"
        )
    ),
    
    B2(
        key = "b2",
        displayName = "B2",
        fullName = "Upper Intermediate (B2)",
        description = "Fluent communication on complex topics and abstract concepts",
        color = Color(0xFFFFB74D), // Light orange for upper intermediate
        difficultyOrder = 4,
        estimatedHours = 500..750,
        keyFeatures = listOf(
            "Advanced grammar structures",
            "Nuanced expression of ideas",
            "Academic and professional topics",
            "Literature and media analysis",
            "Debate and argumentation"
        )
    ),
    
    C1(
        key = "c1",
        displayName = "C1",
        fullName = "Advanced (C1)",
        description = "Effective communication in demanding situations",
        color = Color(0xFF9C27B0), // Purple for advanced
        difficultyOrder = 5,
        estimatedHours = 750..1000,
        keyFeatures = listOf(
            "Sophisticated language use",
            "Implicit meaning understanding",
            "Professional presentations",
            "Complex text comprehension",
            "Cultural nuances and idioms"
        )
    ),
    
    C2(
        key = "c2",
        displayName = "C2",
        fullName = "Mastery (C2)",
        description = "Near-native proficiency with subtle language nuances",
        color = Color(0xFFBA68C8), // Light purple for mastery
        difficultyOrder = 6,
        estimatedHours = 1000..1200,
        keyFeatures = listOf(
            "Native-like fluency",
            "Literary and academic mastery",
            "Subtle distinctions in meaning",
            "Regional dialects and variations",
            "Professional translation skills"
        )
    );
    
    companion object {
        fun fromKey(key: String): GermanLevel {
            return values().find { it.key == key } ?: A1
        }
        
        fun getAllLevels(): List<GermanLevel> = values().toList()
        
        fun getBeginnerLevels(): List<GermanLevel> = listOf(A1, A2)
        fun getIntermediateLevels(): List<GermanLevel> = listOf(B1, B2)
        fun getAdvancedLevels(): List<GermanLevel> = listOf(C1, C2)
        
        fun getLevelsByDifficulty(): List<GermanLevel> = values().sortedBy { it.difficultyOrder }
        
        fun getRecommendedProgression(currentLevel: GermanLevel): List<GermanLevel> {
            return values().filter { it.difficultyOrder >= currentLevel.difficultyOrder }
        }
    }
}

/**
 * User's German level preferences and progress
 */
data class GermanLevelPreferences(
    val selectedLevels: Set<GermanLevel> = setOf(GermanLevel.A1),
    val primaryLevel: GermanLevel = GermanLevel.A1,
    val learningGoals: Set<LearningGoal> = emptySet(),
    val dailyTargetSentences: Int = 5,
    val preferredDifficulty: DifficultyPreference = DifficultyPreference.MIXED,
    val enableProgressiveMode: Boolean = true,
    val customLevelWeights: Map<GermanLevel, Float> = emptyMap()
) {
    
    /**
     * Validate the level preferences
     */
    fun validate(): ValidationResult {
        return try {
            if (selectedLevels.isEmpty()) {
                return ValidationResult.Error("At least one German level must be selected")
            }
            
            if (primaryLevel !in selectedLevels) {
                return ValidationResult.Error("Primary level must be one of the selected levels")
            }
            
            if (dailyTargetSentences < 1 || dailyTargetSentences > 50) {
                return ValidationResult.Error("Daily target must be between 1 and 50 sentences")
            }
            
            // Validate custom weights
            customLevelWeights.values.forEach { weight ->
                if (weight < 0.1f || weight > 2.0f) {
                    return ValidationResult.Error("Level weights must be between 0.1 and 2.0")
                }
            }
            
            ValidationResult.Success
        } catch (e: Exception) {
            ValidationResult.Error("Invalid level preferences: ${e.message}")
        }
    }
    
    /**
     * Get effective weight for a level (custom or default)
     */
    fun getEffectiveWeight(level: GermanLevel): Float {
        return customLevelWeights[level] ?: when {
            level == primaryLevel -> 1.5f // Primary level gets more weight
            level in selectedLevels -> 1.0f // Selected levels get normal weight
            else -> 0.0f // Unselected levels get no weight
        }
    }
    
    /**
     * Get sentence distribution across selected levels
     */
    fun getSentenceDistribution(): Map<GermanLevel, Int> {
        val totalWeight = selectedLevels.sumOf { getEffectiveWeight(it).toDouble() }
        
        return selectedLevels.associateWith { level ->
            val weight = getEffectiveWeight(level)
            ((dailyTargetSentences * weight / totalWeight).toInt()).coerceAtLeast(1)
        }
    }
    
    /**
     * Check if progressive mode should show next level suggestions
     */
    fun shouldSuggestNextLevel(): Boolean {
        return enableProgressiveMode && 
               selectedLevels.maxByOrNull { it.difficultyOrder }?.let { maxLevel ->
                   maxLevel.difficultyOrder < GermanLevel.C2.difficultyOrder
               } ?: false
    }
    
    /**
     * Get next recommended level for progression
     */
    fun getNextRecommendedLevel(): GermanLevel? {
        val maxLevel = selectedLevels.maxByOrNull { it.difficultyOrder }
        return GermanLevel.values().find { it.difficultyOrder == (maxLevel?.difficultyOrder ?: 0) + 1 }
    }
    
    companion object {
        /**
         * Create default preferences for beginners
         */
        fun createDefault(): GermanLevelPreferences {
            return GermanLevelPreferences(
                selectedLevels = setOf(GermanLevel.A1),
                primaryLevel = GermanLevel.A1,
                learningGoals = setOf(LearningGoal.DAILY_CONVERSATION),
                dailyTargetSentences = 5
            )
        }
        
        /**
         * Create preferences for intermediate learners
         */
        fun createIntermediate(): GermanLevelPreferences {
            return GermanLevelPreferences(
                selectedLevels = setOf(GermanLevel.A2, GermanLevel.B1),
                primaryLevel = GermanLevel.B1,
                learningGoals = setOf(LearningGoal.BUSINESS_GERMAN, LearningGoal.TRAVEL),
                dailyTargetSentences = 8
            )
        }
        
        /**
         * Create preferences for advanced learners
         */
        fun createAdvanced(): GermanLevelPreferences {
            return GermanLevelPreferences(
                selectedLevels = setOf(GermanLevel.B2, GermanLevel.C1),
                primaryLevel = GermanLevel.C1,
                learningGoals = setOf(LearningGoal.ACADEMIC_GERMAN, LearningGoal.CULTURAL_FLUENCY),
                dailyTargetSentences = 12,
                preferredDifficulty = DifficultyPreference.CHALLENGING
            )
        }
    }
    
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val errorText: String) : ValidationResult()
        
        val isSuccess: Boolean get() = this is Success
        val errorMessage: String? get() = (this as? Error)?.errorText
    }
}

/**
 * Learning goals for German language acquisition
 */
enum class LearningGoal(
    val key: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val recommendedLevels: List<GermanLevel>
) {
    DAILY_CONVERSATION(
        key = "daily_conversation",
        displayName = "Daily Conversation",
        description = "Everyday communication and social interactions",
        icon = "💬",
        recommendedLevels = listOf(GermanLevel.A1, GermanLevel.A2, GermanLevel.B1)
    ),
    
    BUSINESS_GERMAN(
        key = "business_german",
        displayName = "Business German",
        description = "Professional communication and workplace language",
        icon = "💼",
        recommendedLevels = listOf(GermanLevel.B1, GermanLevel.B2, GermanLevel.C1)
    ),
    
    TRAVEL(
        key = "travel",
        displayName = "Travel & Tourism",
        description = "Navigation, booking, and cultural experiences",
        icon = "✈️",
        recommendedLevels = listOf(GermanLevel.A2, GermanLevel.B1)
    ),
    
    ACADEMIC_GERMAN(
        key = "academic_german",
        displayName = "Academic German",
        description = "University studies and research communication",
        icon = "🎓",
        recommendedLevels = listOf(GermanLevel.B2, GermanLevel.C1, GermanLevel.C2)
    ),
    
    CULTURAL_FLUENCY(
        key = "cultural_fluency",
        displayName = "Cultural Fluency",
        description = "Deep cultural understanding and native-like expression",
        icon = "🎭",
        recommendedLevels = listOf(GermanLevel.C1, GermanLevel.C2)
    ),
    
    EXAM_PREPARATION(
        key = "exam_preparation",
        displayName = "Exam Preparation",
        description = "TestDaF, DSH, Goethe Certificate preparation",
        icon = "📝",
        recommendedLevels = GermanLevel.values().toList()
    );
    
    companion object {
        fun fromKey(key: String): LearningGoal {
            return values().find { it.key == key } ?: DAILY_CONVERSATION
        }
        
        fun getGoalsForLevel(level: GermanLevel): List<LearningGoal> {
            return values().filter { level in it.recommendedLevels }
        }
    }
}

/**
 * Difficulty preferences for content delivery
 */
enum class DifficultyPreference(
    val key: String,
    val displayName: String,
    val description: String
) {
    EASY(
        key = "easy",
        displayName = "Easy Focus",
        description = "Prioritize easier content for confidence building"
    ),
    
    MIXED(
        key = "mixed",
        displayName = "Balanced Mix",
        description = "Balanced combination of different difficulty levels"
    ),
    
    CHALLENGING(
        key = "challenging",
        displayName = "Challenge Mode",
        description = "Emphasize challenging content for rapid progress"
    );
    
    companion object {
        fun fromKey(key: String): DifficultyPreference {
            return values().find { it.key == key } ?: MIXED
        }
    }
} 