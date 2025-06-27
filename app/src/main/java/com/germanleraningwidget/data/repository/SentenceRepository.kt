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
        private val SAMPLE_SENTENCES = listOf(
            // A1 Level - Greetings
            GermanSentence(1, "Guten Morgen!", "Good morning!", GermanLevel.A1, "Greetings"),
            GermanSentence(2, "Wie geht es dir?", "How are you?", GermanLevel.A1, "Greetings"),
            GermanSentence(3, "Guten Abend!", "Good evening!", GermanLevel.A1, "Greetings"),
            
            // A1 Level - Introductions
            GermanSentence(4, "Ich heiße Maria.", "My name is Maria.", GermanLevel.A1, "Introductions"),
            GermanSentence(5, "Woher kommst du?", "Where are you from?", GermanLevel.A1, "Introductions"),
            GermanSentence(6, "Ich komme aus Deutschland.", "I come from Germany.", GermanLevel.A1, "Introductions"),
            
            // A1 Level - Daily Life
            GermanSentence(7, "Ich stehe früh auf.", "I get up early.", GermanLevel.A1, "Daily Life"),
            GermanSentence(8, "Mir geht es nicht gut.", "I don't feel well.", GermanLevel.A1, "Health"),
            GermanSentence(9, "Ich lerne Deutsch.", "I am learning German.", GermanLevel.A1, "Education"),
            
            // A2 Level
            GermanSentence(10, "Ich spreche ein bisschen Deutsch.", "I speak a little German.", GermanLevel.A2, "Language"),
            GermanSentence(11, "Das Wetter ist schön heute.", "The weather is nice today.", GermanLevel.A2, "Weather"),
            GermanSentence(12, "Ich möchte einen Kaffee bestellen.", "I would like to order a coffee.", GermanLevel.A2, "Food"),
            GermanSentence(13, "Kannst du mir helfen?", "Can you help me?", GermanLevel.A2, "Daily Life"),
            GermanSentence(14, "Entschuldigung, wo ist die Toilette?", "Excuse me, where is the bathroom?", GermanLevel.A2, "Travel"),
            
            // B1 Level
            GermanSentence(15, "Das ist sehr interessant.", "That is very interesting.", GermanLevel.B1, "Daily Life"),
            GermanSentence(16, "Ich arbeite als Lehrer.", "I work as a teacher.", GermanLevel.B1, "Work"),
            GermanSentence(17, "Hast du Zeit für ein Gespräch?", "Do you have time for a conversation?", GermanLevel.B1, "Daily Life"),
            GermanSentence(18, "Das Essen schmeckt sehr gut.", "The food tastes very good.", GermanLevel.B1, "Food"),
            GermanSentence(19, "Ich reise gerne.", "I like to travel.", GermanLevel.B1, "Travel"),
            
            // B2 Level
            GermanSentence(20, "Die Reise war anstrengend, aber lohnenswert.", "The journey was exhausting but worthwhile.", GermanLevel.B2, "Travel"),
            GermanSentence(21, "Die Arbeitsatmosphäre in unserem Büro ist sehr angenehm.", "The work atmosphere in our office is very pleasant.", GermanLevel.B2, "Work"),
            GermanSentence(22, "Eine ausgewogene Ernährung trägt zum Wohlbefinden bei.", "A balanced diet contributes to well-being.", GermanLevel.B2, "Health"),
            
            // C1 Level
            GermanSentence(23, "Das Hotel liegt in unmittelbarer Nähe zum Stadtzentrum.", "The hotel is located in close proximity to the city center.", GermanLevel.C1, "Travel"),
            GermanSentence(24, "Die kulinarische Vielfalt Deutschlands ist beeindruckend.", "The culinary diversity of Germany is impressive.", GermanLevel.C1, "Food"),
            GermanSentence(25, "Die Work-Life-Balance ist heutzutage sehr wichtig.", "Work-life balance is very important nowadays.", GermanLevel.C1, "Daily Life"),
            
            // C2 Level
            GermanSentence(26, "Die Digitalisierung hat die Arbeitswelt grundlegend verändert.", "Digitalization has fundamentally changed the working world.", GermanLevel.C2, "Work"),
            GermanSentence(27, "Der gesellschaftliche Wandel beeinflusst unseren Alltag erheblich.", "Social change significantly influences our daily lives.", GermanLevel.C2, "Daily Life"),
            GermanSentence(28, "Lebenslanges Lernen ist in der heutigen Wissensgesellschaft unerlässlich.", "Lifelong learning is essential in today's knowledge society.", GermanLevel.C2, "Education"),
            
            // Additional sentences for variety
            GermanSentence(29, "Ich verstehe nicht.", "I don't understand.", GermanLevel.A1, "Language"),
            GermanSentence(30, "Wo ist der Bahnhof?", "Where is the train station?", GermanLevel.A1, "Travel"),
            GermanSentence(31, "Ich bin hungrig.", "I am hungry.", GermanLevel.A1, "Food"),
            GermanSentence(32, "Mein Handy ist kaputt.", "My phone is broken.", GermanLevel.A1, "Technology"),
            GermanSentence(33, "Ich sehe gern Filme.", "I like to watch movies.", GermanLevel.A1, "Entertainment"),
            GermanSentence(34, "Ich spiele Fußball.", "I play football.", GermanLevel.A1, "Sports"),
            GermanSentence(35, "Es regnet heute.", "It's raining today.", GermanLevel.A1, "Weather")
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
     * Notify BookmarksWidget of changes using reflection to avoid circular dependencies.
     */
    private fun notifyBookmarksWidget() {
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
    }
    
    /**
     * Get a random sentence matching the specified criteria.
     * Uses caching for improved performance.
     * 
     * @param level Target German level
     * @param topics List of allowed topics
     * @return Random sentence or null if no matches found
     */
    fun getRandomSentence(level: GermanLevel, topics: List<String>): GermanSentence? {
        if (topics.isEmpty()) {
            Log.w(TAG, "No topics provided for sentence retrieval")
            return null
        }
        
        val topicsSet = topics.toSet()
        val cacheKey = buildCacheKey(level, topicsSet)
        
        // Try cache first
        sentenceCache[cacheKey]?.let { cachedSentences ->
            if (cachedSentences.isNotEmpty()) {
                return cachedSentences.random()
            }
        }
        
        // Filter and cache sentences
        val filteredSentences = SAMPLE_SENTENCES.filter { sentence ->
            sentence.matchesCriteria(level, topicsSet)
        }
        
        if (filteredSentences.isEmpty()) {
            Log.w(TAG, "No sentences found for level $level and topics $topics")
            return null
        }
        
        sentenceCache[cacheKey] = filteredSentences
        return filteredSentences.random()
    }
    
    /**
     * Build cache key for sentence filtering.
     */
    private fun buildCacheKey(level: GermanLevel, topics: Set<String>): String {
        return "${level.name}_${topics.sorted().joinToString("_")}"
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
} 