package com.germanleraningwidget.data.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Immutable data class representing a German sentence for learning.
 * 
 * @param id Unique identifier for the sentence
 * @param germanText The German text (must not be blank)
 * @param translation English translation (must not be blank)
 * @param level German proficiency level required
 * @param topic Learning topic category
 * @param timestamp Creation timestamp in milliseconds
 * 
 * @throws IllegalArgumentException if germanText or translation is blank
 */
data class GermanSentence(
    val id: Long,
    val germanText: String,
    val translation: String,
    val level: GermanLevel,
    val topic: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(germanText.isNotBlank()) { "German text cannot be blank" }
        require(translation.isNotBlank()) { "Translation cannot be blank" }
        require(topic.isNotBlank()) { "Topic cannot be blank" }
        require(id >= 0) { "ID must be non-negative" }
    }
    
    /**
     * Creates a normalized copy with trimmed strings.
     */
    fun normalized(): GermanSentence = copy(
        germanText = germanText.trim(),
        translation = translation.trim(),
        topic = topic.trim()
    )
    

    
    companion object {
        /**
         * Safe factory method that validates input and returns null if invalid.
         */
        fun createSafe(
            id: Long,
            germanText: String?,
            translation: String?,
            level: GermanLevel,
            topic: String?
        ): GermanSentence? {
            return try {
                if (germanText.isNullOrBlank() || translation.isNullOrBlank() || topic.isNullOrBlank()) {
                    return null
                }
                GermanSentence(id, germanText, translation, level, topic)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        /**
         * Create a GermanSentence instance for testing with optional validation.
         */
        fun createForTesting(
            id: Long,
            german: String,
            english: String,
            level: GermanLevel,
            topic: String,
            validate: Boolean = false
        ): GermanSentence {
            return if (validate) {
                createSafe(id, german, english, level, topic) 
                    ?: GermanSentence(1, "Test", "Test", GermanLevel.A1, "Test")
            } else {
                GermanSentence(id, german, english, level, topic)
            }
        }
    }
}

/**
 * Immutable data class representing a sentence delivery history entry.
 * Used for tracking when sentences were delivered to users.
 */
data class SentenceHistory(
    val id: Long,
    val sentenceId: Long,
    val germanText: String,
    val translation: String,
    val level: GermanLevel,
    val topic: String,
    val deliveredAt: Long = System.currentTimeMillis()
) {
    init {
        require(id >= 0) { "ID must be non-negative" }
        require(sentenceId >= 0) { "Sentence ID must be non-negative" }
        require(germanText.isNotBlank()) { "German text cannot be blank" }
        require(translation.isNotBlank()) { "Translation cannot be blank" }
        require(topic.isNotBlank()) { "Topic cannot be blank" }
        require(deliveredAt > 0) { "Delivered timestamp must be positive" }
    }
    
    companion object {
        /**
         * Creates history entry from a GermanSentence.
         */
        fun fromSentence(sentence: GermanSentence, historyId: Long): SentenceHistory {
            return SentenceHistory(
                id = historyId,
                sentenceId = sentence.id,
                germanText = sentence.germanText,
                translation = sentence.translation,
                level = sentence.level,
                topic = sentence.topic
            )
        }
    }
}

/**
 * Extension function to check if a sentence matches given criteria.
 * Used for filtering sentences based on level and topics.
 * 
 * @param targetLevel The target German level
 * @param allowedTopics Set of allowed topics
 * @return true if the sentence matches the criteria
 */
fun GermanSentence.matchesCriteria(targetLevel: GermanLevel, allowedTopics: Set<String>): Boolean {
    // Check if the sentence level matches or is below the target level
    val levelMatches = this.level.ordinal <= targetLevel.ordinal
    
    // Check if the sentence topic is in the allowed topics
    val topicMatches = allowedTopics.contains(this.topic)
    
    return levelMatches && topicMatches
} 