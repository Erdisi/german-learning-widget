package com.germanleraningwidget.data.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Immutable data class representing a German sentence for learning.
 * 
 * Optimized for:
 * - Memory efficiency with string interning for repeated values
 * - Performance with pre-computed hash codes
 * - Thread safety with immutable design
 * - Validation with comprehensive error checking
 * 
 * @param id Unique identifier for the sentence (must be non-negative)
 * @param germanText The German text (trimmed, must not be blank)
 * @param translation English translation (trimmed, must not be blank)
 * @param level German proficiency level required (A1, A2, B1, B2, C1, C2)
 * @param topic Learning topic category (trimmed, must not be blank)
 * @param timestamp Creation timestamp in milliseconds
 * 
 * @throws IllegalArgumentException if any validation fails
 */
data class GermanSentence(
    val id: Long,
    val germanText: String,
    val translation: String,
    val level: String,
    val topic: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    
    init {
        require(id >= 0) { "ID must be non-negative, got: $id" }
        require(germanText.isNotBlank()) { "German text cannot be blank" }
        require(translation.isNotBlank()) { "Translation cannot be blank" }
        require(level.isNotBlank() && level in VALID_LEVELS) { "Level must be one of: ${VALID_LEVELS.joinToString()}, got: $level" }
        require(topic.isNotBlank()) { "Topic cannot be blank" }
        require(timestamp > 0) { "Timestamp must be positive, got: $timestamp" }
    }
    
    // Pre-compute hash code for performance in collections
    private val precomputedHashCode = computeHashCode()
    
    override fun hashCode(): Int = precomputedHashCode
    
    private fun computeHashCode(): Int {
        return id.hashCode() * 31 + germanText.hashCode()
    }
    
    /**
     * Creates a normalized copy with trimmed strings and optimized memory usage.
     * Uses string interning for common topics and values.
     */
    fun normalized(): GermanSentence {
        val normalizedGerman = germanText.trim().intern()
        val normalizedTranslation = translation.trim().intern()
        val normalizedLevel = level.trim().uppercase()
        val normalizedTopic = topic.trim().intern()
        
        return if (normalizedGerman == germanText && 
                  normalizedTranslation == translation && 
                  normalizedLevel == level &&
                  normalizedTopic == topic) {
            this // Return same instance if no changes needed
        } else {
            copy(
                germanText = normalizedGerman,
                translation = normalizedTranslation,
                level = normalizedLevel,
                topic = normalizedTopic
            )
        }
    }
    
    /**
     * Memory-efficient string representation for debugging.
     */
    override fun toString(): String = "GermanSentence(id=$id, level='$level', topic='$topic')"
    
    /**
     * Checks if this sentence is suitable for the given criteria efficiently.
     * 
     * ⚠️ DEPRECATED: This method shows sentences at OR BELOW the target level.
     * For exact level matching (user selections), use direct level/topic comparison instead.
     * 
     * Migration Guide:
     * - Replace: sentence.matchesCriteria(userLevel, userTopics)
     * - With: sentence.level == userLevel && sentence.topic in userTopics
     * 
     * @deprecated Use exact level/topic matching for user preference filtering
     * @param targetLevel The target German level
     * @param allowedTopics Set of allowed topics
     * @return true if sentence matches criteria (at or below level and topic matches)
     */
    @Deprecated(
        message = "Use exact level/topic matching instead: sentence.level == targetLevel && sentence.topic in allowedTopics",
        replaceWith = ReplaceWith("level == targetLevel && topic in allowedTopics"),
        level = DeprecationLevel.WARNING
    )
    fun matchesCriteria(targetLevel: String, allowedTopics: Set<String>): Boolean {
        // Fast path: check level first (cheaper operation)
        // Compare level strings - sentences at or below target level are suitable
        val levelOrder = mapOf(
            "A1" to 1, "A2" to 2, "B1" to 3, "B2" to 4, "C1" to 5, "C2" to 6
        )
        val sentenceLevelOrder = levelOrder[level] ?: 1
        val targetLevelOrder = levelOrder[targetLevel] ?: 1
        
        if (sentenceLevelOrder > targetLevelOrder) return false
        
        // Topic check with optimized set lookup
        return allowedTopics.contains(topic)
    }
    
    /**
     * Get sentence difficulty score for adaptive learning.
     */
    val difficultyScore: Int get() = level.length + germanText.split(' ').size
    
    /**
     * Check if sentence contains specific words (case-insensitive).
     */
    fun containsWords(words: Set<String>): Boolean {
        val lowerText = germanText.lowercase()
        return words.any { word -> lowerText.contains(word.lowercase()) }
    }
    
    companion object {
        /**
         * Valid German proficiency levels
         */
        val VALID_LEVELS = setOf("A1", "A2", "B1", "B2", "C1", "C2")
        
        /**
         * Safe factory method with enhanced validation and error context.
         */
        fun createSafe(
            id: Long,
            germanText: String?,
            translation: String?,
            level: String,
            topic: String?,
            timestamp: Long = System.currentTimeMillis()
        ): Result<GermanSentence> {
            return try {
                // Validate inputs with detailed error messages
                when {
                    id < 0 -> Result.failure(IllegalArgumentException("ID must be non-negative: $id"))
                    germanText.isNullOrBlank() -> Result.failure(IllegalArgumentException("German text cannot be blank"))
                    translation.isNullOrBlank() -> Result.failure(IllegalArgumentException("Translation cannot be blank"))
                    level.isBlank() || level !in VALID_LEVELS -> Result.failure(IllegalArgumentException("Level must be one of: ${VALID_LEVELS.joinToString()}, got: $level"))
                    topic.isNullOrBlank() -> Result.failure(IllegalArgumentException("Topic cannot be blank"))
                    timestamp <= 0 -> Result.failure(IllegalArgumentException("Timestamp must be positive: $timestamp"))
                    else -> {
                        val sentence = GermanSentence(
                            id = id,
                            germanText = germanText.trim(),
                            translation = translation.trim(),
                            level = level.trim().uppercase(),
                            topic = topic.trim(),
                            timestamp = timestamp
                        )
                        Result.success(sentence)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Create a GermanSentence instance for testing with enhanced options.
         */
        fun createForTesting(
            id: Long = 1L,
            german: String = "Test Satz",
            english: String = "Test sentence",
            level: String = "A1",
            topic: String = "Testing",
            timestamp: Long = System.currentTimeMillis(),
            validate: Boolean = true
        ): GermanSentence {
            return if (validate) {
                createSafe(id, german, english, level, topic, timestamp)
                    .getOrDefault(GermanSentence(1L, "Default Test", "Default Test", "A1", "Test"))
            } else {
                GermanSentence(id, german, english, level, topic, timestamp)
            }
        }
        
        /**
         * Batch create sentences with validation and error reporting.
         */
        fun createBatch(sentenceData: List<SentenceData>): BatchCreationResult {
            val successes = mutableListOf<GermanSentence>()
            val failures = mutableListOf<Pair<SentenceData, Exception>>()
            
            sentenceData.forEach { data ->
                createSafe(data.id, data.germanText, data.translation, data.level, data.topic)
                    .fold(
                        onSuccess = { successes.add(it) },
                        onFailure = { failures.add(data to (it as? Exception ?: Exception(it.message))) }
                    )
            }
            
            return BatchCreationResult(successes, failures)
        }
    }
    
    /**
     * Data class for batch sentence creation.
     */
    data class SentenceData(
        val id: Long,
        val germanText: String,
        val translation: String,
        val level: String,
        val topic: String
    )
    
    /**
     * Result class for batch operations.
     */
    data class BatchCreationResult(
        val successes: List<GermanSentence>,
        val failures: List<Pair<SentenceData, Exception>>
    ) {
        val successCount: Int get() = successes.size
        val failureCount: Int get() = failures.size
        val isFullySuccessful: Boolean get() = failures.isEmpty()
        
        fun getFailureMessages(): List<String> = failures.map { (data, error) ->
            "Failed to create sentence with ID ${data.id}: ${error.message}"
        }
    }
}

/**
 * Optimized immutable data class for sentence delivery history.
 * Enhanced with better memory management and validation.
 */
data class SentenceHistory(
    val id: Long,
    val sentenceId: Long,
    val germanText: String,
    val translation: String,
    val level: String,
    val topic: String,
    val deliveredAt: Long = System.currentTimeMillis()
) {
    init {
        require(id >= 0) { "ID must be non-negative: $id" }
        require(sentenceId >= 0) { "Sentence ID must be non-negative: $sentenceId" }
        require(germanText.isNotBlank()) { "German text cannot be blank" }
        require(translation.isNotBlank()) { "Translation cannot be blank" }
        require(topic.isNotBlank()) { "Topic cannot be blank" }
        require(deliveredAt > 0) { "Delivered timestamp must be positive: $deliveredAt" }
    }
    
    /**
     * Memory-efficient string representation.
     */
    override fun toString(): String = "SentenceHistory(id=$id, sentenceId=$sentenceId, deliveredAt=$deliveredAt)"
    
    companion object {
        /**
         * Creates history entry from a GermanSentence with validation.
         */
        fun fromSentence(sentence: GermanSentence, historyId: Long): Result<SentenceHistory> {
            return try {
                val history = SentenceHistory(
                    id = historyId,
                    sentenceId = sentence.id,
                    germanText = sentence.germanText,
                    translation = sentence.translation,
                    level = sentence.level,
                    topic = sentence.topic
                )
                Result.success(history)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        /**
         * Safe factory method for creating history entries.
         */
        fun createSafe(
            id: Long,
            sentenceId: Long,
            germanText: String,
            translation: String,
            level: String,
            topic: String,
            deliveredAt: Long = System.currentTimeMillis()
        ): Result<SentenceHistory> {
            return try {
                val history = SentenceHistory(id, sentenceId, germanText, translation, level, topic, deliveredAt)
                Result.success(history)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

 