package com.germanleraningwidget.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.model.matchesCriteria
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

private val Context.bookmarkDataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks")

/**
 * Repository for managing German sentences and user bookmarks.
 * 
 * This class provides thread-safe operations for:
 * - Retrieving random sentences based on user preferences
 * - Managing bookmarked sentences with persistent storage
 * - Caching sentences for improved performance
 * - Coordinating with widgets for real-time updates
 * 
 * Thread Safety: All public methods are thread-safe
 * Performance: Uses caching and efficient data structures
 * Error Handling: Comprehensive error handling with graceful degradation
 */
class SentenceRepository private constructor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    companion object {
        private const val TAG = "SentenceRepository"
        private const val BOOKMARKED_IDS_KEY_NAME = "bookmarked_sentence_ids"
        
        @Volatile
        private var INSTANCE: SentenceRepository? = null
        
        /**
         * Thread-safe singleton instance getter.
         */
        fun getInstance(context: Context): SentenceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SentenceRepository(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
        
        /**
         * For testing - allows injection of custom dispatcher
         */
        internal fun createForTesting(
            context: Context,
            dispatcher: CoroutineDispatcher
        ): SentenceRepository {
            return SentenceRepository(context, dispatcher)
        }
        
        /**
         * Sample sentences database - In a real app, this would come from a database or API
         */
        /**
         * Sample sentences database with lazy initialization for better performance.
         * In a real app, this would come from a database or API.
         */
        val SAMPLE_SENTENCES: List<GermanSentence> by lazy {
            createSampleSentences()
        }
        
        /**
         * Create sample sentences for demonstration and testing.
         * Enhanced with more diverse content and proper categorization.
         */
        private fun createSampleSentences(): List<GermanSentence> = listOf(
            // A1 Level - Greetings
            GermanSentence(1, "Guten Morgen!", "Good morning!", "A1", "Greetings"),
            GermanSentence(2, "Wie geht es dir?", "How are you?", "A1", "Greetings"),
            GermanSentence(3, "Guten Abend!", "Good evening!", "A1", "Greetings"),
            
            // A1 Level - Introductions
            GermanSentence(4, "Ich heiße Maria.", "My name is Maria.", "A1", "Introductions"),
            GermanSentence(5, "Woher kommst du?", "Where are you from?", "A1", "Introductions"),
            GermanSentence(6, "Ich komme aus Deutschland.", "I come from Germany.", "A1", "Introductions"),
            
            // A1 Level - Daily Life
            GermanSentence(7, "Ich stehe früh auf.", "I get up early.", "A1", "Daily Life"),
            GermanSentence(8, "Mir geht es nicht gut.", "I don't feel well.", "A1", "Health"),
            GermanSentence(9, "Ich lerne Deutsch.", "I am learning German.", "A1", "Education"),
            
            // A2 Level
            GermanSentence(10, "Ich spreche ein bisschen Deutsch.", "I speak a little German.", "A2", "Language"),
            GermanSentence(11, "Das Wetter ist schön heute.", "The weather is nice today.", "A2", "Weather"),
            GermanSentence(12, "Ich möchte einen Kaffee bestellen.", "I would like to order a coffee.", "A2", "Food"),
            GermanSentence(13, "Kannst du mir helfen?", "Can you help me?", "A2", "Daily Life"),
            GermanSentence(14, "Entschuldigung, wo ist die Toilette?", "Excuse me, where is the bathroom?", "A2", "Travel"),
            
            // B1 Level
            GermanSentence(15, "Das ist sehr interessant.", "That is very interesting.", "B1", "Daily Life"),
            GermanSentence(16, "Ich arbeite als Lehrer.", "I work as a teacher.", "B1", "Work"),
            GermanSentence(17, "Hast du Zeit für ein Gespräch?", "Do you have time for a conversation?", "B1", "Daily Life"),
            GermanSentence(18, "Das Essen schmeckt sehr gut.", "The food tastes very good.", "B1", "Food"),
            GermanSentence(19, "Ich reise gerne.", "I like to travel.", "B1", "Travel"),
            
            // B2 Level
            GermanSentence(20, "Die Reise war anstrengend, aber lohnenswert.", "The journey was exhausting but worthwhile.", "B2", "Travel"),
            GermanSentence(21, "Die Arbeitsatmosphäre in unserem Büro ist sehr angenehm.", "The work atmosphere in our office is very pleasant.", "B2", "Work"),
            GermanSentence(22, "Eine ausgewogene Ernährung trägt zum Wohlbefinden bei.", "A balanced diet contributes to well-being.", "B2", "Health"),
            
            // C1 Level
            GermanSentence(23, "Das Hotel liegt in unmittelbarer Nähe zum Stadtzentrum.", "The hotel is located in close proximity to the city center.", "C1", "Travel"),
            GermanSentence(24, "Die kulinarische Vielfalt Deutschlands ist beeindruckend.", "The culinary diversity of Germany is impressive.", "C1", "Food"),
            GermanSentence(25, "Die Work-Life-Balance ist heutzutage sehr wichtig.", "Work-life balance is very important nowadays.", "C1", "Daily Life"),
            
            // C2 Level
            GermanSentence(26, "Die Digitalisierung hat die Arbeitswelt grundlegend verändert.", "Digitalization has fundamentally changed the working world.", "C2", "Work"),
            GermanSentence(27, "Der gesellschaftliche Wandel beeinflusst unseren Alltag erheblich.", "Social change significantly influences our daily lives.", "C2", "Daily Life"),
            GermanSentence(28, "Lebenslanges Lernen ist in der heutigen Wissensgesellschaft unerlässlich.", "Lifelong learning is essential in today's knowledge society.", "C2", "Education"),
            
            // Additional sentences for variety
            GermanSentence(29, "Ich verstehe nicht.", "I don't understand.", "A1", "Language"),
            GermanSentence(30, "Wo ist der Bahnhof?", "Where is the train station?", "A1", "Travel"),
            GermanSentence(31, "Ich bin hungrig.", "I am hungry.", "A1", "Food"),
            GermanSentence(32, "Mein Handy ist kaputt.", "My phone is broken.", "A1", "Technology"),
            GermanSentence(33, "Ich sehe gern Filme.", "I like to watch movies.", "A1", "Entertainment"),
            GermanSentence(34, "Ich spiele Fußball.", "I play football.", "A1", "Sports"),
            GermanSentence(35, "Es regnet heute.", "It's raining today.", "A1", "Weather")
        )
    }
    
    // DataStore key for bookmarked sentence IDs
    private val bookmarkedIdsKey = stringSetPreferencesKey(BOOKMARKED_IDS_KEY_NAME)
    
    // Thread-safe cache for filtered sentences
    private val sentenceCache = ConcurrentHashMap<String, List<GermanSentence>>()
    
    // Mutex for protecting bookmark operations
    private val bookmarkMutex = Mutex()
    
    // Repository scope with SupervisorJob for error isolation
    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    
    // StateFlow for bookmarked IDs - thread-safe reactive state
    private val _bookmarkedIds = MutableStateFlow<Set<Long>>(emptySet())
    val bookmarkedIds: StateFlow<Set<Long>> = _bookmarkedIds.asStateFlow()
    
    // Error state for monitoring repository health
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    init {
        initializeBookmarks()
    }
    
    /**
     * Initialize bookmarked IDs from persistent storage.
     */
    private fun initializeBookmarks() {
        repositoryScope.launch {
            try {
                val ids = loadBookmarkedIdsFromStorage()
                _bookmarkedIds.value = ids
                _lastError.value = null
                Log.d(TAG, "Initialized with ${ids.size} bookmarked sentences")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize bookmarks", e)
                _lastError.value = "Failed to load bookmarks: ${e.message}"
                _bookmarkedIds.value = emptySet()
            }
        }
    }
    
    /**
     * Load bookmarked IDs from DataStore with proper error handling.
     */
    private suspend fun loadBookmarkedIdsFromStorage(): Set<Long> {
        return try {
            context.bookmarkDataStore.data
                .catch { e ->
                    Log.e(TAG, "Error reading bookmark data", e)
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                }
                .first()
                .let { prefs ->
                    prefs[bookmarkedIdsKey]
                        ?.mapNotNull { idString ->
                            idString.toLongOrNull().also { id ->
                                if (id == null) {
                                    Log.w(TAG, "Invalid bookmark ID format: '$idString'")
                                }
                            }
                        }
                        ?.toSet()
                        ?: emptySet()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error loading bookmarks", e)
            emptySet()
        }
    }
    
    /**
     * Save bookmarked IDs to DataStore with proper error handling.
     */
    private suspend fun saveBookmarkedIdsToStorage(ids: Set<Long>) {
        try {
            context.bookmarkDataStore.edit { prefs ->
                prefs[bookmarkedIdsKey] = ids.map { it.toString() }.toSet()
            }
            Log.d(TAG, "Successfully saved ${ids.size} bookmark IDs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save bookmarks", e)
            throw RepositoryException("Failed to save bookmarks", e)
        }
    }
    
    /**
     * Thread-safe bookmark update with widget notification.
     */
    private fun updateBookmarksAsync(newIds: Set<Long>) {
        repositoryScope.launch {
            bookmarkMutex.withLock {
                try {
                    _bookmarkedIds.value = newIds
                    saveBookmarkedIdsToStorage(newIds)
                    notifyBookmarksWidget()
                    _lastError.value = null
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update bookmarks", e)
                    _lastError.value = "Failed to update bookmarks: ${e.message}"
                    // Revert state on failure
                    loadBookmarkedIdsFromStorage().let { revertedIds ->
                        _bookmarkedIds.value = revertedIds
                    }
                }
            }
        }
    }
    
    /**
     * Notify BookmarksWidget and BookmarksHeroWidget of changes using reflection to avoid circular dependencies.
     */
    private fun notifyBookmarksWidget() {
        // Notify regular BookmarksWidget
        try {
            val bookmarksWidgetClass = Class.forName("com.germanleraningwidget.widget.BookmarksWidget")
            val updateMethod = bookmarksWidgetClass.getMethod("updateAllWidgets", Context::class.java)
            updateMethod.invoke(null, context)
            Log.d(TAG, "Successfully notified BookmarksWidget")
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "BookmarksWidget not available - this is normal during testing")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to notify BookmarksWidget", e)
        }
        
        // Notify Hero BookmarksWidget
        try {
            val heroWidgetClass = Class.forName("com.germanleraningwidget.widget.BookmarksHeroWidget")
            val updateMethod = heroWidgetClass.getMethod("updateAllWidgets", Context::class.java)
            updateMethod.invoke(null, context)
            Log.d(TAG, "Successfully notified BookmarksHeroWidget")
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "BookmarksHeroWidget not available - this is normal during testing")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to notify BookmarksHeroWidget", e)
        }
    }
    
    /**
     * Get a random sentence matching the specified criteria.
     * Enhanced with improved caching, validation, and performance optimizations.
     * 
     * @param level Target German level as String (e.g., "A1", "B2")
     * @param topics List of allowed topics
     * @return Random sentence or null if no matches found
     */
    fun getRandomSentence(level: String, topics: List<String>): GermanSentence? {
        return getRandomSentenceFromLevels(setOf(level), topics)
    }
    
    /**
     * Get a random sentence from multiple German levels with weighted selection.
     * Enhanced with level weighting and smart distribution.
     * 
     * @param levels Set of target German levels (e.g., setOf("A1", "B1"))
     * @param topics List of allowed topics
     * @param levelWeights Optional weights for each level (default: equal weights)
     * @return Random sentence or null if no matches found
     */
    fun getRandomSentenceFromLevels(
        levels: Set<String>, 
        topics: List<String>,
        levelWeights: Map<String, Float> = emptyMap()
    ): GermanSentence? {
        // Input validation with early returns
        if (levels.isEmpty()) {
            Log.w(TAG, "No levels provided for sentence retrieval")
            return null
        }
        
        if (topics.isEmpty()) {
            Log.w(TAG, "No topics provided for sentence retrieval")
            return null
        }
        
        // Convert to set for O(1) lookups - use existing set if already a set  
        @Suppress("UNCHECKED_CAST")
        val topicsSet = if (topics is Set<*>) topics as Set<String> else topics.toSet()
        val cacheKey = buildMultiLevelCacheKey(levels, topicsSet, levelWeights)
        
        // Try cache first with null safety
        sentenceCache[cacheKey]?.let { cachedSentences ->
            if (cachedSentences.isNotEmpty()) {
                return selectWeightedRandomSentence(cachedSentences, levelWeights)
            }
        }
        
        // Filter sentences using optimized instance method for multiple levels
        val filteredSentences = SAMPLE_SENTENCES.filter { sentence ->
            levels.any { level -> sentence.matchesCriteria(level, topicsSet) }
        }
        
        if (filteredSentences.isEmpty()) {
            Log.w(TAG, "No sentences found for levels $levels and topics $topicsSet")
            return null
        }
        
        // Cache results and return weighted random sentence
        sentenceCache[cacheKey] = filteredSentences
        Log.d(TAG, "Cached ${filteredSentences.size} sentences for multi-level key: $cacheKey")
        return selectWeightedRandomSentence(filteredSentences, levelWeights)
    }
    
    /**
     * Get a random sentence using UserPreferences (supports both single and multi-level).
     * This is the recommended method for getting sentences based on user settings.
     * 
     * @param userPreferences User's learning preferences
     * @return Random sentence or null if no matches found
     */
    fun getRandomSentenceForUser(userPreferences: com.germanleraningwidget.data.model.UserPreferences): GermanSentence? {
        return if (userPreferences.hasMultipleLevels) {
            // Use multi-level selection with user's level weights
            getRandomSentenceFromLevels(
                levels = userPreferences.selectedGermanLevels,
                topics = userPreferences.selectedTopics.toList(),
                levelWeights = userPreferences.getLevelWeights()
            )
        } else {
            // Get sentence using multi-level approach with fallback
            val sentence = getRandomSentenceFromLevels(
                levels = userPreferences.selectedGermanLevels,
                topics = userPreferences.selectedTopics.toList()
            )
            
            if (sentence != null) {
                Log.d(TAG, "Found sentence for multi-level delivery: ${sentence.germanText}")
                return sentence
            }
            
            // Fallback to primary level only if multi-level fails
            val fallbackSentence = getRandomSentence(
                level = userPreferences.primaryGermanLevel,
                topics = userPreferences.selectedTopics.toList()
            )
            
            if (fallbackSentence != null) {
                Log.d(TAG, "Found sentence for primary level delivery: ${fallbackSentence.germanText}")
                return fallbackSentence
            }
            
            null
        }
    }
    
    /**
     * Select a weighted random sentence from filtered results.
     * Gives preference to sentences from levels with higher weights.
     */
    private fun selectWeightedRandomSentence(
        sentences: List<GermanSentence>,
        levelWeights: Map<String, Float>
    ): GermanSentence? {
        if (sentences.isEmpty()) return null
        if (levelWeights.isEmpty()) return sentences.random()
        
        // Create weighted list based on sentence levels
        val weightedSentences = sentences.flatMap { sentence ->
            val weight = levelWeights[sentence.level] ?: 1.0f
            val count = (weight * 10).toInt().coerceAtLeast(1) // Convert weight to count
            List(count) { sentence }
        }
        
        return weightedSentences.randomOrNull() ?: sentences.random()
    }
    
    /**
     * Build cache key for multi-level sentence filtering with optimized string building.
     */
    private fun buildMultiLevelCacheKey(
        levels: Set<String>, 
        topics: Set<String>,
        levelWeights: Map<String, Float>
    ): String {
        return buildString {
            append("multi_")
            levels.sorted().joinTo(this, ",")
            append('_')
            topics.sorted().joinTo(this, ",")
            
            // Include weights in cache key if they're not default
            if (levelWeights.isNotEmpty()) {
                append("_weights_")
                levelWeights.toSortedMap().entries.joinTo(this, ",") { "${it.key}:${it.value}" }
            }
        }
    }
    
    /**
     * Build cache key for sentence filtering with optimized string building.
     * LEGACY METHOD - kept for backward compatibility
     */
    private fun buildCacheKey(level: String, topics: Set<String>): String {
        return buildString {
            append(level)
            append('_')
            topics.sorted().joinTo(this, "_")
        }
    }
    
    /**
     * Get sentences distributed across multiple levels based on user preferences.
     * Returns a map of level -> list of sentences for that level.
     * 
     * @param userPreferences User's learning preferences
     * @param count Total number of sentences to retrieve
     * @return Map of level to sentences for that level
     */
    fun getDistributedSentences(
        userPreferences: com.germanleraningwidget.data.model.UserPreferences,
        count: Int = userPreferences.recommendedDailySentences
    ): Map<String, List<GermanSentence>> {
        val distribution = userPreferences.getSentenceDistribution(count)
        val topicsSet = userPreferences.selectedTopics
        
        return distribution.mapValues { (level, targetCount) ->
            val filteredSentences = SAMPLE_SENTENCES.filter { sentence ->
                sentence.matchesCriteria(level, topicsSet)
            }
            
            if (filteredSentences.isEmpty()) {
                emptyList()
            } else {
                // Get random sentences up to target count, allowing duplicates if needed
                (1..targetCount).map { filteredSentences.random() }
            }
        }
    }
    
    /**
     * Get learning statistics for user's selected levels.
     * Provides insights into available content across levels.
     * 
     * @param userPreferences User's learning preferences
     * @return Statistics about available sentences per level
     */
    fun getLearningStatistics(
        userPreferences: com.germanleraningwidget.data.model.UserPreferences
    ): LearningStatistics {
        val topicsSet = userPreferences.selectedTopics
        
        val levelStats = userPreferences.selectedGermanLevels.associateWith { level ->
            val availableSentences = SAMPLE_SENTENCES.count { sentence ->
                sentence.matchesCriteria(level, topicsSet)
            }
            
            val topicBreakdown = topicsSet.associateWith { topic ->
                SAMPLE_SENTENCES.count { sentence ->
                    sentence.level == level && sentence.topic in topicsSet
                }
            }
            
            LevelStatistics(
                level = level,
                availableSentences = availableSentences,
                topicBreakdown = topicBreakdown
            )
        }
        
        return LearningStatistics(
            totalSentences = SAMPLE_SENTENCES.size,
            selectedLevels = userPreferences.selectedGermanLevels,
            selectedTopics = userPreferences.selectedTopics,
            levelStatistics = levelStats,
            recommendedDaily = userPreferences.recommendedDailySentences
        )
    }
    
    /**
     * Save a sentence to bookmarks.
     * Thread-safe operation with proper error handling.
     */
    fun saveSentence(sentence: GermanSentence) {
        val currentIds = _bookmarkedIds.value
        if (currentIds.contains(sentence.id)) {
            Log.d(TAG, "Sentence ${sentence.id} already bookmarked")
            return
        }
        
        val newIds = currentIds + sentence.id
        updateBookmarksAsync(newIds)
        Log.d(TAG, "Bookmarked sentence: ${sentence.id}")
    }
    
    /**
     * Check if a sentence is bookmarked.
     * Thread-safe read operation.
     */
    fun isSentenceSaved(sentenceId: Long): Boolean {
        return _bookmarkedIds.value.contains(sentenceId)
    }
    
    /**
     * Toggle bookmark status of a sentence.
     * Thread-safe operation with atomic state changes.
     * 
     * @param sentence The sentence to toggle
     * @return true if sentence is now bookmarked, false if unbookmarked
     */
    fun toggleSaveSentence(sentence: GermanSentence): Boolean {
        val currentIds = _bookmarkedIds.value
        val isCurrentlySaved = currentIds.contains(sentence.id)
        
        val newIds = if (isCurrentlySaved) {
            currentIds - sentence.id
        } else {
            currentIds + sentence.id
        }
        
        updateBookmarksAsync(newIds)
        
        val action = if (isCurrentlySaved) "Removed" else "Added"
        Log.d(TAG, "$action sentence ${sentence.id} ${if (isCurrentlySaved) "from" else "to"} bookmarks")
        
        return !isCurrentlySaved
    }
    
    /**
     * Get reactive flow of bookmarked sentence IDs.
     */
    fun getSavedSentenceIds(): StateFlow<Set<Long>> = bookmarkedIds
    
    /**
     * Get all bookmarked sentences.
     * Returns a snapshot of current bookmarked sentences.
     */
    fun getSavedSentences(): List<GermanSentence> {
        val savedIds = _bookmarkedIds.value
        return SAMPLE_SENTENCES.filter { it.id in savedIds }
    }
    
    /**
     * Get bookmarked sentences as a reactive Flow.
     */
    fun getSavedSentencesFlow(): Flow<List<GermanSentence>> {
        return bookmarkedIds.map { savedIds ->
            SAMPLE_SENTENCES.filter { it.id in savedIds }
        }
    }
    
    /**
     * Clear sentence cache.
     * Useful when sentence data is updated or memory needs to be freed.
     */
    fun clearCache() {
        sentenceCache.clear()
        Log.d(TAG, "Sentence cache cleared")
    }
    
    /**
     * Get repository statistics for monitoring.
     */
    fun getStatistics(): RepositoryStatistics {
        return RepositoryStatistics(
            totalSentences = SAMPLE_SENTENCES.size,
            bookmarkedSentences = _bookmarkedIds.value.size,
            cacheEntries = sentenceCache.size,
            lastError = _lastError.value
        )
    }
    
    /**
     * Validate repository integrity.
     * Checks for data consistency and removes invalid bookmarks.
     */
    suspend fun validateIntegrity(): ValidationResult {
        return try {
            bookmarkMutex.withLock {
                val currentBookmarks = _bookmarkedIds.value
                val validSentenceIds = SAMPLE_SENTENCES.map { it.id }.toSet()
                val invalidBookmarks = currentBookmarks - validSentenceIds
                
                if (invalidBookmarks.isNotEmpty()) {
                    Log.w(TAG, "Found ${invalidBookmarks.size} invalid bookmarks, cleaning up")
                    val cleanedBookmarks = currentBookmarks - invalidBookmarks
                    _bookmarkedIds.value = cleanedBookmarks
                    saveBookmarkedIdsToStorage(cleanedBookmarks)
                }
                
                ValidationResult.Success(
                    message = "Repository integrity validated. Cleaned ${invalidBookmarks.size} invalid bookmarks."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Integrity validation failed", e)
            ValidationResult.Error("Validation failed: ${e.message}")
        }
    }
    
    /**
     * Data classes for repository monitoring and validation
     */
    data class RepositoryStatistics(
        val totalSentences: Int,
        val bookmarkedSentences: Int,
        val cacheEntries: Int,
        val lastError: String?
    )
    
    sealed class ValidationResult {
        data class Success(val message: String) : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
    
    /**
     * Custom exception for repository operations
     */
    class RepositoryException(message: String, cause: Throwable? = null) : Exception(message, cause)
    
    /**
     * Data classes for learning statistics and insights
     */
    data class LevelStatistics(
        val level: String,
        val availableSentences: Int,
        val topicBreakdown: Map<String, Int>
    )
    
    data class LearningStatistics(
        val totalSentences: Int,
        val selectedLevels: Set<String>,
        val selectedTopics: Set<String>,
        val levelStatistics: Map<String, LevelStatistics>,
        val recommendedDaily: Int
    ) {
        val totalAvailableForUser: Int = levelStatistics.values.sumOf { it.availableSentences }
        val coveragePercentage: Float = if (totalSentences > 0) {
            (totalAvailableForUser.toFloat() / totalSentences) * 100f
        } else 0f
    }
} 