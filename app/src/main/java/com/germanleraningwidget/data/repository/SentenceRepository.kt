package com.germanleraningwidget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.model.UserPreferences
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
import com.germanleraningwidget.util.OptimizationUtils
import com.germanleraningwidget.util.DebugUtils
import com.germanleraningwidget.util.AppConstants
import kotlinx.coroutines.withContext

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
        private const val BOOKMARKED_IDS_KEY_NAME = AppConstants.BOOKMARKED_IDS_KEY_NAME
        
        @Suppress("StaticFieldLeak") // Safe: Uses application context only, not activity context
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
            
            // Additional sentences for variety and missing topics
            GermanSentence(29, "Ich verstehe nicht.", "I don't understand.", "A1", "Language"),
            GermanSentence(30, "Wo ist der Bahnhof?", "Where is the train station?", "A1", "Travel"),
            GermanSentence(31, "Ich bin hungrig.", "I am hungry.", "A1", "Food"),
            GermanSentence(32, "Mein Handy ist kaputt.", "My phone is broken.", "A1", "Technology"),
            GermanSentence(33, "Ich sehe gern Filme.", "I like to watch movies.", "A1", "Entertainment"),
            GermanSentence(34, "Ich spiele Fußball.", "I play football.", "A1", "Sports"),
            GermanSentence(35, "Es regnet heute.", "It's raining today.", "A1", "Weather"),
            
            // Family topic sentences
            GermanSentence(36, "Das ist meine Familie.", "This is my family.", "A1", "Family"),
            GermanSentence(37, "Meine Mutter ist sehr nett.", "My mother is very nice.", "A2", "Family"),
            GermanSentence(38, "Wir verbringen viel Zeit mit der Familie.", "We spend a lot of time with family.", "B1", "Family"),
            GermanSentence(39, "Familienzusammenkünfte sind sehr wichtig.", "Family gatherings are very important.", "B2", "Family"),
            
            // Culture topic sentences
            GermanSentence(40, "Deutsche Kultur ist sehr interessant.", "German culture is very interesting.", "A2", "Culture"),
            GermanSentence(41, "Wir besuchen das Museum.", "We visit the museum.", "A2", "Culture"),
            GermanSentence(42, "Die deutsche Literatur hat eine reiche Tradition.", "German literature has a rich tradition.", "B2", "Culture"),
            GermanSentence(43, "Kulturelle Vielfalt bereichert unser Leben.", "Cultural diversity enriches our lives.", "C1", "Culture"),
            
            // Business topic sentences
            GermanSentence(44, "Das Geschäft läuft gut.", "Business is going well.", "B1", "Business"),
            GermanSentence(45, "Wir haben einen wichtigen Termin.", "We have an important appointment.", "B1", "Business"),
            GermanSentence(46, "Die Geschäftsstrategie muss überarbeitet werden.", "The business strategy needs to be revised.", "C1", "Business"),
            GermanSentence(47, "Internationale Handelsbeziehungen sind komplex.", "International trade relations are complex.", "C2", "Business"),
            
            // Science topic sentences
            GermanSentence(48, "Die Wissenschaft ist faszinierend.", "Science is fascinating.", "B1", "Science"),
            GermanSentence(49, "Forschung ist sehr wichtig.", "Research is very important.", "B1", "Science"),
            GermanSentence(50, "Wissenschaftliche Erkenntnisse verändern unser Weltbild.", "Scientific discoveries change our world view.", "C1", "Science"),
            GermanSentence(51, "Die Quantenphysik revolutioniert unser Verständnis der Realität.", "Quantum physics revolutionizes our understanding of reality.", "C2", "Science"),
            
            // Politics topic sentences
            GermanSentence(52, "Politik ist ein wichtiges Thema.", "Politics is an important topic.", "B1", "Politics"),
            GermanSentence(53, "Demokratie ist ein wertvolles Gut.", "Democracy is a valuable asset.", "B2", "Politics"),
            GermanSentence(54, "Politische Entscheidungen beeinflussen unser tägliches Leben.", "Political decisions influence our daily lives.", "C1", "Politics"),
            GermanSentence(55, "Die geopolitische Lage erfordert diplomatisches Geschick.", "The geopolitical situation requires diplomatic skill.", "C2", "Politics"),
            
            // Art topic sentences  
            GermanSentence(56, "Kunst ist schön.", "Art is beautiful.", "A2", "Art"),
            GermanSentence(57, "Ich male gerne.", "I like to paint.", "A2", "Art"),
            GermanSentence(58, "Moderne Kunst ist oft schwer zu verstehen.", "Modern art is often difficult to understand.", "B2", "Art"),
            GermanSentence(59, "Künstlerischer Ausdruck spiegelt gesellschaftliche Entwicklungen wider.", "Artistic expression reflects social developments.", "C2", "Art")
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
                DebugUtils.logInfo(TAG, "Initialized with ${ids.size} bookmarked sentences")
            } catch (e: Exception) {
                DebugUtils.logError(TAG, "Failed to initialize bookmarks", e)
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
                    DebugUtils.logError(TAG, "Error reading bookmark data", e)
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                }
                .first()
                .let { prefs ->
                    prefs[bookmarkedIdsKey]
                        ?.mapNotNull { idString ->
                            idString.toLongOrNull().also { id ->
                                if (id == null) {
                                    DebugUtils.logWarning(TAG, "Invalid bookmark ID format: '$idString'")
                                }
                            }
                        }
                        ?.toSet()
                        ?: emptySet()
                }
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Critical error loading bookmarks", e)
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
            DebugUtils.logInfo(TAG, "Successfully saved ${ids.size} bookmark IDs")
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Failed to save bookmarks", e)
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
                    DebugUtils.logError(TAG, "Failed to update bookmarks", e)
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
            DebugUtils.logInfo(TAG, "Successfully notified BookmarksWidget")
        } catch (e: ClassNotFoundException) {
            DebugUtils.logInfo(TAG, "BookmarksWidget not available - this is normal during testing")
        } catch (e: Exception) {
            DebugUtils.logWarning(TAG, "Failed to notify BookmarksWidget", e)
        }
        
        // Notify Hero BookmarksWidget
        try {
            val heroWidgetClass = Class.forName("com.germanleraningwidget.widget.BookmarksHeroWidget")
            val updateMethod = heroWidgetClass.getMethod("updateAllWidgets", Context::class.java)
            updateMethod.invoke(null, context)
            DebugUtils.logInfo(TAG, "Successfully notified BookmarksHeroWidget")
        } catch (e: ClassNotFoundException) {
            DebugUtils.logInfo(TAG, "BookmarksHeroWidget not available - this is normal during testing")
        } catch (e: Exception) {
            DebugUtils.logWarning(TAG, "Failed to notify BookmarksHeroWidget", e)
        }
    }
    
    /**
     * Get a random sentence matching the specified criteria with performance monitoring.
     */
    suspend fun getRandomSentence(
        levels: Set<String>,
        topics: Set<String>
    ): GermanSentence? = OptimizationUtils.measureOptimizedOperation(AppConstants.PerformanceOperations.GET_RANDOM_SENTENCE) {
        return@measureOptimizedOperation withContext(Dispatchers.IO) {
            try {
                DebugUtils.logInfo("SentenceRepository", "Getting random sentence for levels: $levels, topics: $topics")
                
                val sentences = SAMPLE_SENTENCES.filter { sentence ->
                    sentence.level in levels && sentence.topic in topics
                }
                
                if (sentences.isEmpty()) {
                    DebugUtils.logWarning("SentenceRepository", "No sentences found for criteria")
                    return@withContext null
                }
                
                val randomSentence = sentences.random()
                DebugUtils.logInfo("SentenceRepository", "Retrieved random sentence: ${randomSentence.id}")
                randomSentence
                
            } catch (e: Exception) {
                DebugUtils.logError("SentenceRepository", "Error getting random sentence", e)
                null
            }
        }
    }
    
    /**
     * Get distributed sentences across levels and topics with performance monitoring.
     */
    suspend fun getDistributedSentences(
        userPreferences: UserPreferences,
        count: Int
    ): Map<String, List<GermanSentence>> = OptimizationUtils.measureOptimizedOperation(AppConstants.PerformanceOperations.GET_DISTRIBUTED_SENTENCES) {
        return@measureOptimizedOperation withContext(Dispatchers.IO) {
            try {
                DebugUtils.logInfo("SentenceRepository", "Getting $count distributed sentences")
                
                val result = mutableMapOf<String, MutableList<GermanSentence>>()
                val selectedLevels = userPreferences.selectedGermanLevels.toList()
                val selectedTopics = userPreferences.selectedTopics.toList()
                
                if (selectedLevels.isEmpty() || selectedTopics.isEmpty()) {
                    return@withContext emptyMap()
                }
                
                val sentencesPerLevel = maxOf(1, count / selectedLevels.size)
                
                for (level in selectedLevels) {
                    val levelSentences = mutableListOf<GermanSentence>()
                    
                    for (topic in selectedTopics) {
                        val topicSentences = SAMPLE_SENTENCES.filter { sentence ->
                            sentence.level == level && sentence.topic == topic
                        }
                        
                        if (topicSentences.isNotEmpty()) {
                            val sentencesToAdd = minOf(
                                sentencesPerLevel / selectedTopics.size + 1,
                                topicSentences.size
                            )
                            levelSentences.addAll(topicSentences.shuffled().take(sentencesToAdd))
                        }
                    }
                    
                    if (levelSentences.isNotEmpty()) {
                        result[level] = levelSentences.shuffled().take(sentencesPerLevel).toMutableList()
                    }
                }
                
                DebugUtils.logInfo("SentenceRepository", "Generated distributed sentences: ${result.values.sumOf { it.size }} total")
                result
                
            } catch (e: Exception) {
                DebugUtils.logError("SentenceRepository", "Error getting distributed sentences", e)
                emptyMap()
            }
        }
    }
    
    /**
     * Save a sentence to bookmarks with performance monitoring.
     */
    suspend fun saveBookmark(sentence: GermanSentence): Boolean = OptimizationUtils.measureOptimizedOperation(AppConstants.PerformanceOperations.SAVE_BOOKMARK) {
        return@measureOptimizedOperation withContext(Dispatchers.IO) {
            try {
                DebugUtils.logInfo("SentenceRepository", "Saving bookmark for sentence: ${sentence.id}")
                
                val bookmarkedSentenceIds = _bookmarkedIds.value.toMutableSet()
                val wasAdded = bookmarkedSentenceIds.add(sentence.id)
                
                if (wasAdded) {
                    _bookmarkedIds.value = bookmarkedSentenceIds
                    saveBookmarkedIdsToStorage(bookmarkedSentenceIds)
                    DebugUtils.logInfo("SentenceRepository", "Bookmark saved successfully")
                } else {
                    DebugUtils.logInfo("SentenceRepository", "Sentence already bookmarked")
                }
                
                wasAdded
                
            } catch (e: Exception) {
                DebugUtils.logError("SentenceRepository", "Error saving bookmark", e)
                false
            }
        }
    }
    
    /**
     * Remove a sentence from bookmarks with performance monitoring.
     */
    suspend fun removeBookmark(sentenceId: Long): Boolean = OptimizationUtils.measureOptimizedOperation("remove_bookmark") {
        return@measureOptimizedOperation withContext(Dispatchers.IO) {
            try {
                DebugUtils.logInfo("SentenceRepository", "Removing bookmark for sentence: $sentenceId")
                
                val bookmarkedSentenceIds = _bookmarkedIds.value.toMutableSet()
                val wasRemoved = bookmarkedSentenceIds.remove(sentenceId)
                
                if (wasRemoved) {
                    _bookmarkedIds.value = bookmarkedSentenceIds
                    saveBookmarkedIdsToStorage(bookmarkedSentenceIds)
                    DebugUtils.logInfo("SentenceRepository", "Bookmark removed successfully")
                } else {
                    DebugUtils.logInfo("SentenceRepository", "Sentence was not bookmarked")
                }
                
                wasRemoved
                
            } catch (e: Exception) {
                DebugUtils.logError("SentenceRepository", "Error removing bookmark", e)
                false
            }
        }
    }
    
    /**
     * Generate daily sentence pool with performance monitoring and optimization.
     */
    suspend fun generateDailyPool(
        levels: Set<String>,
        topics: Set<String>,
        poolSize: Int
    ): List<GermanSentence> = OptimizationUtils.measureOptimizedOperation("generate_daily_pool") {
        return@measureOptimizedOperation withContext(Dispatchers.IO) {
            try {
                DebugUtils.logInfo("SentenceRepository", "Generating daily pool: $poolSize sentences")
                
                // Get eligible sentences
                val eligibleSentences = SAMPLE_SENTENCES.filter { sentence ->
                    sentence.level in levels && sentence.topic in topics
                }
                
                if (eligibleSentences.isEmpty()) {
                    DebugUtils.logWarning("SentenceRepository", "No eligible sentences for daily pool")
                    return@withContext emptyList()
                }
                
                // Create balanced distribution
                val poolSentences = if (eligibleSentences.size <= poolSize) {
                    eligibleSentences.shuffled()
                } else {
                    // Distribute across levels and topics
                    val sentencesPerLevel = poolSize / levels.size
                    val remainingSentences = poolSize % levels.size
                    val result = mutableListOf<GermanSentence>()
                    
                    levels.forEachIndexed { index, level ->
                        val levelSentences = eligibleSentences.filter { it.level == level }
                        val countForLevel = sentencesPerLevel + if (index < remainingSentences) 1 else 0
                        result.addAll(levelSentences.shuffled().take(countForLevel))
                    }
                    
                    result.shuffled()
                }
                
                // Store in preferences
                storeDailyPool(poolSentences)
                
                DebugUtils.logInfo("SentenceRepository", "Generated daily pool with ${poolSentences.size} sentences")
                poolSentences
                
            } catch (e: Exception) {
                DebugUtils.logError("SentenceRepository", "Error generating daily pool", e)
                emptyList()
            }
        }
    }
    
    /**
     * Check if daily pool needs regeneration with performance monitoring.
     */
    suspend fun shouldRegenerateDailyPool(): Boolean = OptimizationUtils.measureOptimizedOperation("check_pool_regeneration") {
        return@measureOptimizedOperation withContext(Dispatchers.IO) {
            try {
                val today = getCurrentDateString()
                val lastPoolDate = getLastPoolDate()
                val shouldRegenerate = lastPoolDate != today
                
                DebugUtils.logInfo("SentenceRepository", "Pool regeneration check: today=$today, lastPool=$lastPoolDate, shouldRegenerate=$shouldRegenerate")
                shouldRegenerate
                
            } catch (e: Exception) {
                DebugUtils.logError("SentenceRepository", "Error checking pool regeneration", e)
                true // Default to regeneration on error
            }
        }
    }
    
    /**
     * Get next sentence from daily pool with performance monitoring.
     */
    suspend fun getNextSentenceFromDailyPool(): GermanSentence? = OptimizationUtils.measureOptimizedOperation("get_next_pool_sentence") {
        return@measureOptimizedOperation withContext(Dispatchers.IO) {
            try {
                val dailyPool = getDailyPool()
                if (dailyPool.isEmpty()) {
                    DebugUtils.logWarning("SentenceRepository", "Daily pool is empty")
                    return@withContext null
                }
                
                val currentIndex = getCurrentPoolIndex()
                val nextIndex = currentIndex % dailyPool.size
                val sentence = dailyPool[nextIndex]
                
                // Update index for next call
                updateCurrentPoolIndex((nextIndex + 1) % dailyPool.size)
                
                DebugUtils.logInfo("SentenceRepository", "Retrieved sentence ${nextIndex + 1}/${dailyPool.size} from daily pool")
                sentence
                
            } catch (e: Exception) {
                DebugUtils.logError("SentenceRepository", "Error getting next pool sentence", e)
                null
            }
        }
    }
    
    /**
     * Clear daily pool for cleanup with performance monitoring.
     */
    suspend fun clearDailyPool() = OptimizationUtils.measureOptimizedOperation("clear_daily_pool") {
        withContext(Dispatchers.IO) {
            try {
                DebugUtils.logInfo("SentenceRepository", "Clearing daily pool")
                
                val prefs = context.getSharedPreferences("daily_sentence_pool", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                
                DebugUtils.logInfo("SentenceRepository", "Daily pool cleared successfully")
                
            } catch (e: Exception) {
                DebugUtils.logError("SentenceRepository", "Error clearing daily pool", e)
            }
        }
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
                    DebugUtils.logWarning(TAG, "Found ${invalidBookmarks.size} invalid bookmarks, cleaning up")
                    val cleanedBookmarks = currentBookmarks - invalidBookmarks
                    _bookmarkedIds.value = cleanedBookmarks
                    saveBookmarkedIdsToStorage(cleanedBookmarks)
                }
                
                ValidationResult.Success(
                    message = "Repository integrity validated. Cleaned ${invalidBookmarks.size} invalid bookmarks."
                )
            }
        } catch (e: Exception) {
            DebugUtils.logError(TAG, "Integrity validation failed", e)
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
    
    // Daily pool management helper methods
    
    /**
     * Store daily pool sentences in shared preferences
     */
    private fun storeDailyPool(sentences: List<GermanSentence>) {
        val prefs = context.getSharedPreferences("daily_sentence_pool", Context.MODE_PRIVATE)
        val today = getCurrentDateString()
        
        prefs.edit().apply {
            putString("daily_pool_date", today)
            putInt("pool_size", sentences.size)
            putInt("current_index", 0)
            
            sentences.forEachIndexed { index, sentence ->
                putLong("sentence_$index", sentence.id)
            }
            
            apply()
        }
    }
    
    /**
     * Get daily pool sentences from shared preferences
     */
    private fun getDailyPool(): List<GermanSentence> {
        val prefs = context.getSharedPreferences("daily_sentence_pool", Context.MODE_PRIVATE)
        val poolSize = prefs.getInt("pool_size", 0)
        
        if (poolSize == 0) return emptyList()
        
        val sentences = mutableListOf<GermanSentence>()
        for (i in 0 until poolSize) {
            val sentenceId = prefs.getLong("sentence_$i", -1L)
            if (sentenceId != -1L) {
                val sentence = getSentenceById(sentenceId)
                if (sentence != null) {
                    sentences.add(sentence)
                }
            }
        }
        
        return sentences
    }
    
    /**
     * Get current pool index
     */
    private fun getCurrentPoolIndex(): Int {
        val prefs = context.getSharedPreferences("daily_sentence_pool", Context.MODE_PRIVATE)
        return prefs.getInt("current_index", 0)
    }
    
    /**
     * Update current pool index
     */
    private fun updateCurrentPoolIndex(newIndex: Int) {
        val prefs = context.getSharedPreferences("daily_sentence_pool", Context.MODE_PRIVATE)
        prefs.edit().putInt("current_index", newIndex).apply()
    }
    
    /**
     * Get last pool generation date
     */
    private fun getLastPoolDate(): String {
        val prefs = context.getSharedPreferences("daily_sentence_pool", Context.MODE_PRIVATE)
        return prefs.getString("daily_pool_date", "") ?: ""
    }
    
    /**
     * Get current date as string for daily pool management
     */
    private fun getCurrentDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        return "${calendar.get(java.util.Calendar.YEAR)}-${calendar.get(java.util.Calendar.MONTH) + 1}-${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
    }
    
    /**
     * Get a sentence by its ID.
     */
    fun getSentenceById(sentenceId: Long): GermanSentence? {
        return SAMPLE_SENTENCES.find { it.id == sentenceId }
    }
    
    /**
     * Get a Flow of bookmarked sentence IDs for reactive UI updates.
     */
    fun getSavedSentenceIds(): Flow<Set<Long>> = bookmarkedIds
    
    /**
     * Get a list of saved sentences synchronously.
     */
    fun getSavedSentences(): List<GermanSentence> {
        val savedIds = _bookmarkedIds.value
        return SAMPLE_SENTENCES.filter { sentence -> sentence.id in savedIds }
    }
    
    /**
     * Toggle the bookmark status of a sentence.
     */
    suspend fun toggleSaveSentence(sentence: GermanSentence): Boolean {
        val isCurrentlySaved = _bookmarkedIds.value.contains(sentence.id)
        
        return if (isCurrentlySaved) {
            removeBookmark(sentence.id)
        } else {
            saveBookmark(sentence)
        }
    }
    
    /**
     * Check if a sentence is currently saved/bookmarked.
     */
    fun isSentenceSaved(sentenceId: Long): Boolean {
        return _bookmarkedIds.value.contains(sentenceId)
    }
    
    /**
     * Get a random sentence from the specified levels and topics.
     */
    suspend fun getRandomSentenceFromLevels(
        levels: Set<String>,
        topics: Set<String>
    ): GermanSentence? {
        return getRandomSentence(levels, topics)
    }
} 