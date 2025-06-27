package com.germanleraningwidget.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.GermanSentence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

private val Context.bookmarkDataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmarks")

class SentenceRepository private constructor(private val context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: SentenceRepository? = null
        fun getInstance(context: Context): SentenceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SentenceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val BOOKMARKED_IDS_KEY = stringSetPreferencesKey("bookmarked_sentence_ids")
    private val sentenceCache = ConcurrentHashMap<String, List<GermanSentence>>()
    private val _bookmarkedIds = MutableStateFlow<Set<Long>>(emptySet())
    val bookmarkedIds: StateFlow<Set<Long>> = _bookmarkedIds.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val ids = loadBookmarkedIds()
            _bookmarkedIds.value = ids
            android.util.Log.d("SentenceRepository", "Loaded bookmarked IDs: $ids")
        }
    }

    private suspend fun loadBookmarkedIds(): Set<Long> {
        return try {
            val prefs = context.bookmarkDataStore.data.first()
            prefs[BOOKMARKED_IDS_KEY]?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        } catch (e: Exception) {
            android.util.Log.e("SentenceRepository", "Error loading bookmarked IDs", e)
            emptySet()
        }
    }

    private suspend fun saveBookmarkedIds(ids: Set<Long>) {
        try {
            context.bookmarkDataStore.edit { prefs ->
                prefs[BOOKMARKED_IDS_KEY] = ids.map { it.toString() }.toSet()
            }
            android.util.Log.d("SentenceRepository", "Saved bookmarked IDs: $ids")
        } catch (e: Exception) {
            android.util.Log.e("SentenceRepository", "Error saving bookmarked IDs", e)
        }
    }

    private fun updateBookmarksAsync(ids: Set<Long>) {
        _bookmarkedIds.value = ids
        CoroutineScope(Dispatchers.IO).launch {
            saveBookmarkedIds(ids)
        }
    }

    fun getRandomSentence(level: GermanLevel, topics: List<String>): GermanSentence? {
        val cacheKey = "${level.name}_${topics.sorted().joinToString("_")}"
        sentenceCache[cacheKey]?.let { cachedSentences ->
            if (cachedSentences.isNotEmpty()) {
                return cachedSentences.random()
            }
        }
        val filteredSentences = sampleSentences.filter { sentence ->
            sentence.level == level && topics.contains(sentence.topic)
        }
        sentenceCache[cacheKey] = filteredSentences
        return filteredSentences.randomOrNull()
    }

    fun saveSentence(sentence: GermanSentence) {
        val newIds = _bookmarkedIds.value + sentence.id
        updateBookmarksAsync(newIds)
        android.util.Log.d("SentenceRepository", "Saved sentence ${sentence.id}: ${sentence.germanText}")
    }

    fun isSentenceSaved(sentenceId: Long): Boolean {
        val isSaved = _bookmarkedIds.value.contains(sentenceId)
        android.util.Log.d("SentenceRepository", "Checking if sentence $sentenceId is saved: $isSaved")
        return isSaved
    }

    fun toggleSaveSentence(sentence: GermanSentence): Boolean {
        android.util.Log.d("SentenceRepository", "Toggling save for sentence ${sentence.id}: ${sentence.germanText}")
        return if (isSentenceSaved(sentence.id)) {
            val newIds = _bookmarkedIds.value - sentence.id
            updateBookmarksAsync(newIds)
            android.util.Log.d("SentenceRepository", "Removed sentence ${sentence.id} from bookmarks")
            false
        } else {
            saveSentence(sentence)
            android.util.Log.d("SentenceRepository", "Added sentence ${sentence.id} to bookmarks")
            true
        }
    }

    fun getSavedSentenceIds(): StateFlow<Set<Long>> {
        return bookmarkedIds
    }

    fun getSavedSentences(): List<GermanSentence> {
        val savedIds = _bookmarkedIds.value
        return sampleSentences.filter { it.id in savedIds }
    }

    fun clearCache() {
        sentenceCache.clear()
    }

    // Sample sentences database
    private val sampleSentences = listOf(
        GermanSentence(
            id = 1,
            germanText = "Guten Morgen!",
            translation = "Good morning!",
            level = GermanLevel.A1,
            topic = "Greetings"
        ),
        GermanSentence(
            id = 2,
            germanText = "Wie geht es dir?",
            translation = "How are you?",
            level = GermanLevel.A1,
            topic = "Greetings"
        ),
        GermanSentence(
            id = 3,
            germanText = "Ich heiße Maria.",
            translation = "My name is Maria.",
            level = GermanLevel.A1,
            topic = "Introductions"
        ),
        GermanSentence(
            id = 4,
            germanText = "Woher kommst du?",
            translation = "Where are you from?",
            level = GermanLevel.A1,
            topic = "Introductions"
        ),
        GermanSentence(
            id = 5,
            germanText = "Ich spreche ein bisschen Deutsch.",
            translation = "I speak a little German.",
            level = GermanLevel.A2,
            topic = "Language"
        ),
        GermanSentence(
            id = 6,
            germanText = "Das Wetter ist schön heute.",
            translation = "The weather is nice today.",
            level = GermanLevel.A2,
            topic = "Weather"
        ),
        GermanSentence(
            id = 7,
            germanText = "Ich möchte einen Kaffee bestellen.",
            translation = "I would like to order a coffee.",
            level = GermanLevel.A2,
            topic = "Food"
        ),
        GermanSentence(
            id = 8,
            germanText = "Kannst du mir helfen?",
            translation = "Can you help me?",
            level = GermanLevel.A2,
            topic = "Daily Life"
        ),
        GermanSentence(
            id = 9,
            germanText = "Ich verstehe nicht.",
            translation = "I don't understand.",
            level = GermanLevel.A1,
            topic = "Language"
        ),
        GermanSentence(
            id = 10,
            germanText = "Entschuldigung, wo ist die Toilette?",
            translation = "Excuse me, where is the bathroom?",
            level = GermanLevel.A2,
            topic = "Travel"
        ),
        GermanSentence(
            id = 11,
            germanText = "Das ist sehr interessant.",
            translation = "That is very interesting.",
            level = GermanLevel.B1,
            topic = "Daily Life"
        ),
        GermanSentence(
            id = 12,
            germanText = "Ich arbeite als Lehrer.",
            translation = "I work as a teacher.",
            level = GermanLevel.B1,
            topic = "Work"
        ),
        GermanSentence(
            id = 13,
            germanText = "Hast du Zeit für ein Gespräch?",
            translation = "Do you have time for a conversation?",
            level = GermanLevel.B1,
            topic = "Daily Life"
        ),
        GermanSentence(
            id = 14,
            germanText = "Das Essen schmeckt sehr gut.",
            translation = "The food tastes very good.",
            level = GermanLevel.B1,
            topic = "Food"
        ),
        GermanSentence(
            id = 15,
            germanText = "Ich reise gerne.",
            translation = "I like to travel.",
            level = GermanLevel.B1,
            topic = "Travel"
        ),
        
        // Travel Topic - 5 additional sentences
        GermanSentence(
            id = 16,
            germanText = "Wo ist der Bahnhof?",
            translation = "Where is the train station?",
            level = GermanLevel.A1,
            topic = "Travel"
        ),
        GermanSentence(
            id = 17,
            germanText = "Ich brauche eine Fahrkarte nach Berlin.",
            translation = "I need a ticket to Berlin.",
            level = GermanLevel.A2,
            topic = "Travel"
        ),
        GermanSentence(
            id = 18,
            germanText = "Wann fährt der nächste Zug ab?",
            translation = "When does the next train depart?",
            level = GermanLevel.B1,
            topic = "Travel"
        ),
        GermanSentence(
            id = 19,
            germanText = "Die Reise war anstrengend, aber lohnenswert.",
            translation = "The journey was exhausting but worthwhile.",
            level = GermanLevel.B2,
            topic = "Travel"
        ),
        GermanSentence(
            id = 20,
            germanText = "Das Hotel liegt in unmittelbarer Nähe zum Stadtzentrum.",
            translation = "The hotel is located in close proximity to the city center.",
            level = GermanLevel.C1,
            topic = "Travel"
        ),
        
        // Food Topic - 5 additional sentences
        GermanSentence(
            id = 21,
            germanText = "Ich bin hungrig.",
            translation = "I am hungry.",
            level = GermanLevel.A1,
            topic = "Food"
        ),
        GermanSentence(
            id = 22,
            germanText = "Was ist das Tagesgericht?",
            translation = "What is the daily special?",
            level = GermanLevel.A2,
            topic = "Food"
        ),
        GermanSentence(
            id = 23,
            germanText = "Könnten Sie mir die Speisekarte bringen?",
            translation = "Could you bring me the menu?",
            level = GermanLevel.B1,
            topic = "Food"
        ),
        GermanSentence(
            id = 24,
            germanText = "Dieses Gericht ist eine Spezialität aus Bayern.",
            translation = "This dish is a specialty from Bavaria.",
            level = GermanLevel.B2,
            topic = "Food"
        ),
        GermanSentence(
            id = 25,
            germanText = "Die kulinarische Vielfalt Deutschlands ist beeindruckend.",
            translation = "The culinary diversity of Germany is impressive.",
            level = GermanLevel.C1,
            topic = "Food"
        ),
        
        // Work Topic - 5 additional sentences
        GermanSentence(
            id = 26,
            germanText = "Ich habe einen Job.",
            translation = "I have a job.",
            level = GermanLevel.A1,
            topic = "Work"
        ),
        GermanSentence(
            id = 27,
            germanText = "Wann beginnt die Arbeit?",
            translation = "When does work start?",
            level = GermanLevel.A2,
            topic = "Work"
        ),
        GermanSentence(
            id = 28,
            germanText = "Ich muss heute Überstunden machen.",
            translation = "I have to work overtime today.",
            level = GermanLevel.B1,
            topic = "Work"
        ),
        GermanSentence(
            id = 29,
            germanText = "Die Arbeitsatmosphäre in unserem Büro ist sehr angenehm.",
            translation = "The work atmosphere in our office is very pleasant.",
            level = GermanLevel.B2,
            topic = "Work"
        ),
        GermanSentence(
            id = 30,
            germanText = "Die Digitalisierung hat die Arbeitswelt grundlegend verändert.",
            translation = "Digitalization has fundamentally changed the working world.",
            level = GermanLevel.C2,
            topic = "Work"
        ),
        
        // Daily Life Topic - 5 additional sentences
        GermanSentence(
            id = 31,
            germanText = "Ich stehe früh auf.",
            translation = "I get up early.",
            level = GermanLevel.A1,
            topic = "Daily Life"
        ),
        GermanSentence(
            id = 32,
            germanText = "Was machst du in deiner Freizeit?",
            translation = "What do you do in your free time?",
            level = GermanLevel.A2,
            topic = "Daily Life"
        ),
        GermanSentence(
            id = 33,
            germanText = "Meine tägliche Routine ist ziemlich strukturiert.",
            translation = "My daily routine is quite structured.",
            level = GermanLevel.B2,
            topic = "Daily Life"
        ),
        GermanSentence(
            id = 34,
            germanText = "Die Work-Life-Balance ist heutzutage sehr wichtig.",
            translation = "Work-life balance is very important nowadays.",
            level = GermanLevel.C1,
            topic = "Daily Life"
        ),
        GermanSentence(
            id = 35,
            germanText = "Der gesellschaftliche Wandel beeinflusst unseren Alltag erheblich.",
            translation = "Social change significantly influences our daily lives.",
            level = GermanLevel.C2,
            topic = "Daily Life"
        ),
        
        // Technology Topic - 5 additional sentences
        GermanSentence(
            id = 36,
            germanText = "Mein Handy ist kaputt.",
            translation = "My phone is broken.",
            level = GermanLevel.A1,
            topic = "Technology"
        ),
        GermanSentence(
            id = 37,
            germanText = "Kannst du mir beim Computer helfen?",
            translation = "Can you help me with the computer?",
            level = GermanLevel.A2,
            topic = "Technology"
        ),
        GermanSentence(
            id = 38,
            germanText = "Ich nutze verschiedene Apps für meine Arbeit.",
            translation = "I use various apps for my work.",
            level = GermanLevel.B1,
            topic = "Technology"
        ),
        GermanSentence(
            id = 39,
            germanText = "Die künstliche Intelligenz entwickelt sich rasant.",
            translation = "Artificial intelligence is developing rapidly.",
            level = GermanLevel.B2,
            topic = "Technology"
        ),
        GermanSentence(
            id = 40,
            germanText = "Die Digitalisierung bringt sowohl Chancen als auch Herausforderungen mit sich.",
            translation = "Digitalization brings both opportunities and challenges.",
            level = GermanLevel.C1,
            topic = "Technology"
        ),
        
        // Health Topic - 5 additional sentences
        GermanSentence(
            id = 41,
            germanText = "Mir geht es nicht gut.",
            translation = "I don't feel well.",
            level = GermanLevel.A1,
            topic = "Health"
        ),
        GermanSentence(
            id = 42,
            germanText = "Ich brauche einen Arzttermin.",
            translation = "I need a doctor's appointment.",
            level = GermanLevel.A2,
            topic = "Health"
        ),
        GermanSentence(
            id = 43,
            germanText = "Sport ist wichtig für die Gesundheit.",
            translation = "Exercise is important for health.",
            level = GermanLevel.B1,
            topic = "Health"
        ),
        GermanSentence(
            id = 44,
            germanText = "Eine ausgewogene Ernährung trägt zum Wohlbefinden bei.",
            translation = "A balanced diet contributes to well-being.",
            level = GermanLevel.B2,
            topic = "Health"
        ),
        GermanSentence(
            id = 45,
            germanText = "Präventivmedizin spielt eine zunehmend wichtige Rolle im Gesundheitswesen.",
            translation = "Preventive medicine plays an increasingly important role in healthcare.",
            level = GermanLevel.C2,
            topic = "Health"
        ),
        
        // Education Topic - 5 additional sentences
        GermanSentence(
            id = 46,
            germanText = "Ich lerne Deutsch.",
            translation = "I am learning German.",
            level = GermanLevel.A1,
            topic = "Education"
        ),
        GermanSentence(
            id = 47,
            germanText = "Welche Schule besuchst du?",
            translation = "Which school do you attend?",
            level = GermanLevel.A2,
            topic = "Education"
        ),
        GermanSentence(
            id = 48,
            germanText = "Ich studiere an der Universität München.",
            translation = "I study at the University of Munich.",
            level = GermanLevel.B1,
            topic = "Education"
        ),
        GermanSentence(
            id = 49,
            germanText = "Das deutsche Bildungssystem ist sehr differenziert.",
            translation = "The German education system is very differentiated.",
            level = GermanLevel.B2,
            topic = "Education"
        ),
        GermanSentence(
            id = 50,
            germanText = "Lebenslanges Lernen ist in der heutigen Wissensgesellschaft unerlässlich.",
            translation = "Lifelong learning is essential in today's knowledge society.",
            level = GermanLevel.C2,
            topic = "Education"
        ),
        
        // Entertainment Topic - 5 additional sentences
        GermanSentence(
            id = 51,
            germanText = "Ich sehe gern Filme.",
            translation = "I like to watch movies.",
            level = GermanLevel.A1,
            topic = "Entertainment"
        ),
        GermanSentence(
            id = 52,
            germanText = "Welche Musik hörst du gerne?",
            translation = "What kind of music do you like to listen to?",
            level = GermanLevel.A2,
            topic = "Entertainment"
        ),
        GermanSentence(
            id = 53,
            germanText = "Am Wochenende gehe ich oft ins Kino.",
            translation = "On weekends I often go to the cinema.",
            level = GermanLevel.B1,
            topic = "Entertainment"
        ),
        GermanSentence(
            id = 54,
            germanText = "Die deutsche Filmindustrie hat viele talentierte Regisseure hervorgebracht.",
            translation = "The German film industry has produced many talented directors.",
            level = GermanLevel.B2,
            topic = "Entertainment"
        ),
        GermanSentence(
            id = 55,
            germanText = "Streaming-Dienste haben die Art, wie wir Unterhaltung konsumieren, revolutioniert.",
            translation = "Streaming services have revolutionized the way we consume entertainment.",
            level = GermanLevel.C1,
            topic = "Entertainment"
        ),
        
        // Sports Topic - 5 additional sentences
        GermanSentence(
            id = 56,
            germanText = "Ich spiele Fußball.",
            translation = "I play football.",
            level = GermanLevel.A1,
            topic = "Sports"
        ),
        GermanSentence(
            id = 57,
            germanText = "Machst du gerne Sport?",
            translation = "Do you like to do sports?",
            level = GermanLevel.A2,
            topic = "Sports"
        ),
        GermanSentence(
            id = 58,
            germanText = "Ich trainiere dreimal pro Woche im Fitnessstudio.",
            translation = "I train three times a week at the gym.",
            level = GermanLevel.B1,
            topic = "Sports"
        ),
        GermanSentence(
            id = 59,
            germanText = "Regelmäßiger Sport verbessert die körperliche und geistige Gesundheit.",
            translation = "Regular exercise improves physical and mental health.",
            level = GermanLevel.B2,
            topic = "Sports"
        ),
        GermanSentence(
            id = 60,
            germanText = "Profisport ist zu einem milliardenschweren Wirtschaftszweig geworden.",
            translation = "Professional sports has become a billion-dollar industry.",
            level = GermanLevel.C1,
            topic = "Sports"
        ),
        
        // Weather Topic - 5 additional sentences
        GermanSentence(
            id = 61,
            germanText = "Es regnet heute.",
            translation = "It's raining today.",
            level = GermanLevel.A1,
            topic = "Weather"
        ),
        GermanSentence(
            id = 62,
            germanText = "Wie ist das Wetter morgen?",
            translation = "How is the weather tomorrow?",
            level = GermanLevel.A2,
            topic = "Weather"
        ),
        GermanSentence(
            id = 63,
            germanText = "Der Winter war dieses Jahr besonders kalt.",
            translation = "The winter was particularly cold this year.",
            level = GermanLevel.B1,
            topic = "Weather"
        ),
        GermanSentence(
            id = 64,
            germanText = "Extreme Wetterereignisse nehmen aufgrund des Klimawandels zu.",
            translation = "Extreme weather events are increasing due to climate change.",
            level = GermanLevel.B2,
            topic = "Weather"
        ),
        GermanSentence(
            id = 65,
            germanText = "Meteorologen verwenden komplexe Modelle zur Wettervorhersage.",
            translation = "Meteorologists use complex models for weather forecasting.",
            level = GermanLevel.C1,
            topic = "Weather"
        )
    )
} 