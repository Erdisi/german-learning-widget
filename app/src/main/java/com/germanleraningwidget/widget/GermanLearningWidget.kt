package com.germanleraningwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.germanleraningwidget.MainActivity
import com.germanleraningwidget.R
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.model.WidgetType
import com.germanleraningwidget.data.model.WidgetCustomization
import com.germanleraningwidget.data.model.WidgetBackgroundColor
import com.germanleraningwidget.data.model.WidgetTextContrast
import com.germanleraningwidget.data.repository.SentenceRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class GermanLearningWidget : AppWidgetProvider() {
    
    companion object {
        private const val ACTION_SAVE_SENTENCE = "com.germanleraningwidget.SAVE_SENTENCE"
        private const val EXTRA_WIDGET_ID = "widget_id"
        private const val EXTRA_SENTENCE_ID = "sentence_id"
        
        // Track current sentence for each widget instance
        private val currentSentences = mutableMapOf<Int, GermanSentence>()
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, GermanLearningWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf() // Handle potential null return
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, GermanLearningWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        android.util.Log.d("GermanLearningWidget", "onUpdate called for widgets: ${appWidgetIds.contentToString()}")
        for (appWidgetId in appWidgetIds) {
            // FIXED: Don't force new sentence on regular updates - preserves current content
            updateWidget(context, appWidgetManager, appWidgetId, forceNewSentence = false)
        }
    }
    
    /**
     * Single method to update a widget with current data and customizations.
     * CRITICAL FIX: Only fetch new sentence when explicitly requested, not on every update.
     */
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        forceNewSentence: Boolean = false
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                
                // Apply customizations using centralized helper
                val customization = WidgetCustomizationHelper.applyCustomizations(
                    context, views, WidgetType.MAIN, R.id.widget_container
                )
                
                // CRITICAL FIX: Only get new sentence when forced or when no current sentence exists
                val sentence = if (forceNewSentence || !currentSentences.containsKey(appWidgetId)) {
                    android.util.Log.d("GermanLearningWidget", "Getting new sentence for widget $appWidgetId (force: $forceNewSentence, no current: ${!currentSentences.containsKey(appWidgetId)})")
                    
                    // Load repositories only when we need a new sentence
                    val sentenceRepository = SentenceRepository.getInstance(context)
                    val preferencesRepository = UserPreferencesRepository(context)
                    val preferences = preferencesRepository.userPreferences.first()
                    
                    // Get sentence data using multi-level support
                    sentenceRepository.getRandomSentenceFromLevels(
                        levels = preferences.selectedGermanLevels,
                        topics = preferences.selectedTopics
                    )
                } else {
                    // Use existing sentence to prevent continuous changes
                    val existingSentence = currentSentences[appWidgetId]
                    android.util.Log.d("GermanLearningWidget", "Using existing sentence for widget $appWidgetId: ${existingSentence?.germanText}")
                    existingSentence
                }
                
                if (sentence != null) {
                    // Store current sentence only if it's new
                    if (forceNewSentence || !currentSentences.containsKey(appWidgetId)) {
                        currentSentences[appWidgetId] = sentence
                        android.util.Log.d("GermanLearningWidget", "Stored new sentence ${sentence.id} for widget $appWidgetId")
                    }
                    
                    // Set text content
                    views.setTextViewText(R.id.widget_german_text, sentence.germanText)
                    views.setTextViewText(R.id.widget_translation, sentence.translation)
                    views.setTextViewText(R.id.widget_topic, sentence.topic)
                    views.setTextViewText(R.id.widget_level_indicator, sentence.level) // Show actual sentence level
                    
                    // Apply automatic text customizations based on actual content
                    WidgetCustomizationHelper.applyAutoTextCustomizations(
                        views, customization,
                        R.id.widget_german_text, R.id.widget_translation,
                        sentence.germanText, sentence.translation,
                        isHeroWidget = false
                    )
                    
                    // Apply colors to secondary text elements (title, level, topic)
                    WidgetCustomizationHelper.applySecondaryTextColors(
                        views, customization,
                        titleTextViewId = R.id.widget_main_title, // Now includes the main title
                        levelTextViewId = R.id.widget_level_indicator,
                        topicTextViewId = R.id.widget_topic,
                        counterTextViewId = null
                    )
                    
                    // Apply dynamic backgrounds for better visibility
                    WidgetCustomizationHelper.applyDynamicBackgrounds(
                        views, customization,
                        topicTextViewId = R.id.widget_topic,
                        levelTextViewId = R.id.widget_level_indicator,
                        buttonId = R.id.widget_save_button
                    )
                    
                    // Set save button state - always check to ensure it's current
                    val sentenceRepository = SentenceRepository.getInstance(context)
                    val isSaved = sentenceRepository.isSentenceSaved(sentence.id)
                    views.setImageViewResource(
                        R.id.widget_save_button,
                        if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                    )
                    
                    // Set up save button click
                    setupSaveButton(context, views, appWidgetId, sentence.id)
                } else {
                    // No sentences found matching user preferences - show setup required message
                    android.util.Log.w("GermanLearningWidget", "No sentences found for widget $appWidgetId")
                    
                    // Show setup required message instead of defaulting to A1
                    views.setTextViewText(R.id.widget_german_text, "Setup Required")
                    views.setTextViewText(R.id.widget_translation, "Tap to configure your learning preferences")
                    views.setTextViewText(R.id.widget_topic, "Setup")
                    views.setTextViewText(R.id.widget_level_indicator, "")
                    
                    // Apply basic customizations without text auto-sizing
                    WidgetCustomizationHelper.applyCustomizations(
                        context, views, WidgetType.MAIN, R.id.widget_container
                    )
                    
                    // Hide save button for setup message
                    views.setImageViewResource(R.id.widget_save_button, R.drawable.ic_bookmark_border)
                    views.setOnClickPendingIntent(R.id.widget_save_button, null) // Disable save button
                }
                
                // Set up main click intent
                setupMainClick(context, views, appWidgetId)
                
                // Update widget on main thread
                CoroutineScope(Dispatchers.Main).launch {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("GermanLearningWidget", "Error updating widget $appWidgetId", e)
                // Show error state with basic customization
                showErrorState(context, appWidgetManager, appWidgetId)
            }
        }
    }
    
    /**
     * Set up main container click to open app.
     */
    private fun setupMainClick(context: Context, views: RemoteViews, appWidgetId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, "home")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)
    }
    
    /**
     * Set up save button click intent.
     */
    private fun setupSaveButton(context: Context, views: RemoteViews, appWidgetId: Int, sentenceId: Long) {
        android.util.Log.d("GermanLearningWidget", "Setting up save button for widget $appWidgetId, sentence $sentenceId")
        val saveIntent = Intent(context, GermanLearningWidget::class.java).apply {
            action = ACTION_SAVE_SENTENCE
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_SENTENCE_ID, sentenceId)
        }
        val savePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId + 2000, // Different request code from main click
            saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_save_button, savePendingIntent)
    }
    
    /**
     * Show error state with basic styling.
     */
    private fun showErrorState(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                
                // Apply basic customization
                WidgetCustomizationHelper.applyCustomizations(
                    context, views, WidgetType.MAIN, R.id.widget_container
                )
                
                views.setTextViewText(R.id.widget_german_text, "Error loading")
                views.setTextViewText(R.id.widget_translation, "Tap to open app")
                views.setTextViewText(R.id.widget_topic, "")
                views.setTextViewText(R.id.widget_level_indicator, "")
                
                setupMainClick(context, views, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("GermanLearningWidget", "Error showing error state", e)
            }
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("GermanLearningWidget", "onReceive called with action: ${intent.action}")
        
        when (intent.action) {
            ACTION_SAVE_SENTENCE -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val sentenceId = intent.getLongExtra(EXTRA_SENTENCE_ID, -1L)
                
                android.util.Log.d("GermanLearningWidget", "Save sentence action: widgetId=$widgetId, sentenceId=$sentenceId")
                
                if (widgetId != -1 && sentenceId != -1L) {
                    handleSaveSentence(context, widgetId, sentenceId)
                } else {
                    android.util.Log.e("GermanLearningWidget", "Invalid save sentence parameters: widgetId=$widgetId, sentenceId=$sentenceId")
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                // Handle both regular updates and custom updates with sentence data
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                val germanText = intent.getStringExtra("german_text")
                val translation = intent.getStringExtra("translation")
                
                // Check for fresh customization data
                val freshCustomizationBackgroundColor = intent.getStringExtra("fresh_customization_background_color")
                val freshCustomizationTextContrast = intent.getStringExtra("fresh_customization_text_contrast")
                
                // Check if this is a new sentence from the worker
                val triggerUpdate = intent.getBooleanExtra("triggerUpdate", false)
                val sentenceId = intent.getLongExtra("sentenceId", -1L)
                
                android.util.Log.d("GermanLearningWidget", "ACTION_APPWIDGET_UPDATE: germanText=$germanText, triggerUpdate=$triggerUpdate, sentenceId=$sentenceId")
                
                if (appWidgetIds != null) {
                    if (germanText != null && translation != null) {
                        // Custom update with specific sentence data from worker
                        updateWidgetsWithSentence(context, appWidgetIds, germanText, translation, intent)
                    } else if (freshCustomizationBackgroundColor != null || freshCustomizationTextContrast != null) {
                        // Custom update with fresh customization data
                        updateWidgetsWithFreshCustomization(context, appWidgetIds, intent)
                    } else if (triggerUpdate || sentenceId != -1L) {
                        // Worker triggered update - force new sentence
                        android.util.Log.d("GermanLearningWidget", "Worker triggered update - forcing new sentence")
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        for (appWidgetId in appWidgetIds) {
                            updateWidget(context, appWidgetManager, appWidgetId, forceNewSentence = true)
                        }
                    } else {
                        // Regular update - preserve current sentence
                        android.util.Log.d("GermanLearningWidget", "Regular update - preserving current sentence")
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        for (appWidgetId in appWidgetIds) {
                            updateWidget(context, appWidgetManager, appWidgetId, forceNewSentence = false)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Update widgets with fresh customization data from UI changes.
     */
    private fun updateWidgetsWithFreshCustomization(
        context: Context,
        appWidgetIds: IntArray,
        intent: Intent
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Extract customization data from intent
                val backgroundColorKey = intent.getStringExtra("fresh_customization_background_color")
                val textContrastKey = intent.getStringExtra("fresh_customization_text_contrast")
                
                android.util.Log.d("GermanLearningWidget", "Received fresh customization: bg=$backgroundColorKey, contrast=$textContrastKey")
                
                val appWidgetManager = AppWidgetManager.getInstance(context)
                
                for (appWidgetId in appWidgetIds) {
                    if (appWidgetId < 0) continue
                    
                    val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                    
                    // Create fresh customization object if we have the data
                    val freshCustomization = if (backgroundColorKey != null || textContrastKey != null) {
                        try {
                            // Get current customization as base
                            val currentCustomization = WidgetCustomization.createDefault(WidgetType.MAIN)
                            
                            // Override with fresh values
                            currentCustomization.copy(
                                backgroundColor = backgroundColorKey?.let { key ->
                                    WidgetBackgroundColor.values().find { it.key == key }
                                } ?: currentCustomization.backgroundColor,
                                textContrast = textContrastKey?.let { key ->
                                    WidgetTextContrast.values().find { it.key == key }
                                } ?: currentCustomization.textContrast
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("GermanLearningWidget", "Error creating fresh customization", e)
                            null
                        }
                    } else null
                    
                    // Apply customizations with fresh data
                    val customization = WidgetCustomizationHelper.applyCustomizations(
                        context, views, WidgetType.MAIN, R.id.widget_container, freshCustomization
                    )
                    
                    // Load current content or use existing content
                    val currentSentence = currentSentences[appWidgetId]
                    if (currentSentence != null) {
                        // Update existing content with new customizations
                        views.setTextViewText(R.id.widget_german_text, currentSentence.germanText)
                        views.setTextViewText(R.id.widget_translation, currentSentence.translation)
                        views.setTextViewText(R.id.widget_topic, currentSentence.topic)
                        views.setTextViewText(R.id.widget_level_indicator, currentSentence.level)
                        
                        // Apply automatic text customizations
                        WidgetCustomizationHelper.applyAutoTextCustomizations(
                            views, customization,
                            R.id.widget_german_text, R.id.widget_translation,
                            currentSentence.germanText, currentSentence.translation,
                            isHeroWidget = false
                        )
                        
                        // Set up save button
                        val sentenceRepository = SentenceRepository.getInstance(context)
                        val isSaved = sentenceRepository.isSentenceSaved(currentSentence.id)
                        views.setImageViewResource(
                            R.id.widget_save_button,
                            if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                        )
                        setupSaveButton(context, views, appWidgetId, currentSentence.id)
                    } else {
                        // Load fresh content with new customizations
                        loadFreshContent(context, views, appWidgetId, customization)
                    }
                    
                    setupMainClick(context, views, appWidgetId)
                    
                    // Update on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                        android.util.Log.d("GermanLearningWidget", "Widget $appWidgetId updated with fresh customization")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GermanLearningWidget", "Error updating widgets with fresh customization", e)
            }
        }
    }
    
    /**
     * Load fresh content for widget when no existing content is available.
     */
    private suspend fun loadFreshContent(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        customization: WidgetCustomization
    ) {
        try {
            val sentenceRepository = SentenceRepository.getInstance(context)
            val preferencesRepository = UserPreferencesRepository(context)
            val preferences = preferencesRepository.userPreferences.first()
            
            val sentence = sentenceRepository.getRandomSentenceFromLevels(
                levels = preferences.selectedGermanLevels,
                topics = preferences.selectedTopics
            )
            
            if (sentence != null) {
                currentSentences[appWidgetId] = sentence
                views.setTextViewText(R.id.widget_german_text, sentence.germanText)
                views.setTextViewText(R.id.widget_translation, sentence.translation)
                views.setTextViewText(R.id.widget_topic, sentence.topic)
                views.setTextViewText(R.id.widget_level_indicator, sentence.level)
                
                WidgetCustomizationHelper.applyAutoTextCustomizations(
                    views, customization,
                    R.id.widget_german_text, R.id.widget_translation,
                    sentence.germanText, sentence.translation,
                    isHeroWidget = false
                )
                
                // Apply colors to secondary text elements (title, level, topic)
                WidgetCustomizationHelper.applySecondaryTextColors(
                    views, customization,
                    titleTextViewId = R.id.widget_main_title, // Now includes the main title
                    levelTextViewId = R.id.widget_level_indicator,
                    topicTextViewId = R.id.widget_topic,
                    counterTextViewId = null
                )
                
                // Apply dynamic backgrounds for better visibility
                WidgetCustomizationHelper.applyDynamicBackgrounds(
                    views, customization,
                    topicTextViewId = R.id.widget_topic,
                    levelTextViewId = R.id.widget_level_indicator,
                    buttonId = R.id.widget_save_button
                )
                
                val isSaved = sentenceRepository.isSentenceSaved(sentence.id)
                views.setImageViewResource(
                    R.id.widget_save_button,
                    if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                )
                setupSaveButton(context, views, appWidgetId, sentence.id)
            } else {
                views.setTextViewText(R.id.widget_german_text, "Setup Required")
                views.setTextViewText(R.id.widget_translation, "Tap to configure preferences")
                views.setTextViewText(R.id.widget_topic, "Setup")
                views.setTextViewText(R.id.widget_level_indicator, "")
            }
        } catch (e: Exception) {
            android.util.Log.e("GermanLearningWidget", "Error loading fresh content", e)
        }
    }

    /**
     * Update widgets with specific sentence data (from WorkManager).
     */
    private fun updateWidgetsWithSentence(
        context: Context,
        appWidgetIds: IntArray,
        germanText: String,
        translation: String,
        intent: Intent
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val topic = intent.getStringExtra("topic")
                val sentenceId = intent.getLongExtra("sentence_id", -1L)
                val level = intent.getStringExtra("level") ?: "A1"
                
                for (appWidgetId in appWidgetIds) {
                    // Validate appWidgetId
                    if (appWidgetId < 0) {
                        android.util.Log.w("GermanLearningWidget", "Invalid widget ID: $appWidgetId")
                        continue
                    }
                    
                    val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                    
                    // Create sentence object for tracking with validation
                    val sentence = try {
                        GermanSentence(
                            id = sentenceId,
                            germanText = germanText,
                            translation = translation,
                            level = level,
                            topic = topic ?: "Unknown"
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("GermanLearningWidget", "Error creating sentence object", e)
                        continue
                    }
                    
                    // Store current sentence for bookmark functionality
                    currentSentences[appWidgetId] = sentence
                    android.util.Log.d("GermanLearningWidget", "Stored sentence from worker ${sentence.id} for widget $appWidgetId")
                    
                    // Apply customizations
                    val customization = WidgetCustomizationHelper.applyCustomizations(
                        context, views, WidgetType.MAIN, R.id.widget_container
                    )
                    
                    // Set content
                    views.setTextViewText(R.id.widget_german_text, germanText)
                    views.setTextViewText(R.id.widget_translation, translation)
                    views.setTextViewText(R.id.widget_topic, topic ?: "")
                    views.setTextViewText(R.id.widget_level_indicator, level)
                    
                    // Apply automatic text customizations based on actual content
                    WidgetCustomizationHelper.applyAutoTextCustomizations(
                        views, customization,
                        R.id.widget_german_text, R.id.widget_translation,
                        germanText, translation,
                        isHeroWidget = false
                    )
                    
                    // Apply colors to secondary text elements (title, level, topic)
                    WidgetCustomizationHelper.applySecondaryTextColors(
                        views, customization,
                        titleTextViewId = R.id.widget_main_title, // Now includes the main title
                        levelTextViewId = R.id.widget_level_indicator,
                        topicTextViewId = R.id.widget_topic,
                        counterTextViewId = null
                    )
                    
                    // Apply dynamic backgrounds for better visibility
                    WidgetCustomizationHelper.applyDynamicBackgrounds(
                        views, customization,
                        topicTextViewId = R.id.widget_topic,
                        levelTextViewId = R.id.widget_level_indicator,
                        buttonId = R.id.widget_save_button
                    )
                    
                    // Set up save button if we have sentence ID
                    if (sentenceId != -1L) {
                        val sentenceRepository = SentenceRepository.getInstance(context)
                        val isSaved = sentenceRepository.isSentenceSaved(sentenceId)
                        views.setImageViewResource(
                            R.id.widget_save_button,
                            if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                        )
                        setupSaveButton(context, views, appWidgetId, sentenceId)
                    }
                    
                    setupMainClick(context, views, appWidgetId)
                    
                    // Update on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GermanLearningWidget", "Error updating widgets with sentence", e)
            }
        }
    }
    
    /**
     * Handle save/unsave sentence action with improved reliability.
     */
    private fun handleSaveSentence(context: Context, widgetId: Int, sentenceId: Long) {
        android.util.Log.d("GermanLearningWidget", "handleSaveSentence: widgetId=$widgetId, sentenceId=$sentenceId")
        android.util.Log.d("GermanLearningWidget", "Current sentences map size: ${currentSentences.size}")
        android.util.Log.d("GermanLearningWidget", "Current sentences: ${currentSentences.keys}")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val currentSentence = currentSentences[widgetId]
                
                android.util.Log.d("GermanLearningWidget", "Retrieved sentence for widget $widgetId: $currentSentence")
                
                // CRITICAL FIX: Handle case where currentSentences map is empty or incorrect
                val sentenceToToggle = if (currentSentence != null && currentSentence.id == sentenceId) {
                    android.util.Log.d("GermanLearningWidget", "Using cached sentence: ${currentSentence.germanText}")
                    currentSentence
                } else {
                    android.util.Log.w("GermanLearningWidget", "Cached sentence mismatch or missing. Attempting to find sentence by ID: $sentenceId")
                    
                    // ROBUST FALLBACK: Try to find the sentence in repository by ID
                    val foundSentence = try {
                        sentenceRepository.getSentenceById(sentenceId)
                    } catch (e: Exception) {
                        android.util.Log.e("GermanLearningWidget", "Failed to find sentence by ID $sentenceId", e)
                        null
                    }
                    
                    if (foundSentence != null) {
                        android.util.Log.d("GermanLearningWidget", "Found sentence in repository: ${foundSentence.germanText}")
                        // Update cache with found sentence
                        currentSentences[widgetId] = foundSentence
                        foundSentence
                    } else {
                        android.util.Log.e("GermanLearningWidget", "Could not find sentence with ID $sentenceId anywhere")
                        null
                    }
                }
                
                if (sentenceToToggle != null) {
                    android.util.Log.d("GermanLearningWidget", "Toggling save state for sentence: ${sentenceToToggle.germanText}")
                    val isNowSaved = sentenceRepository.toggleSaveSentence(sentenceToToggle)
                    
                    android.util.Log.d("GermanLearningWidget", "Sentence is now saved: $isNowSaved")
                    
                    // Rebuild the entire widget with proper customizations
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                    
                    // Apply current customizations
                    val customization = WidgetCustomizationHelper.applyCustomizations(
                        context, views, WidgetType.MAIN, R.id.widget_container
                    )
                    
                    // Restore current content
                    views.setTextViewText(R.id.widget_german_text, sentenceToToggle.germanText)
                    views.setTextViewText(R.id.widget_translation, sentenceToToggle.translation)
                    views.setTextViewText(R.id.widget_topic, sentenceToToggle.topic)
                    views.setTextViewText(R.id.widget_level_indicator, sentenceToToggle.level)
                    
                    // Apply automatic text customizations
                    WidgetCustomizationHelper.applyAutoTextCustomizations(
                        views, customization,
                        R.id.widget_german_text, R.id.widget_translation,
                        sentenceToToggle.germanText, sentenceToToggle.translation,
                        isHeroWidget = false
                    )
                    
                    // Update save button icon
                    views.setImageViewResource(
                        R.id.widget_save_button,
                        if (isNowSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                    )
                    
                    // Set up click handlers
                    setupMainClick(context, views, widgetId)
                    setupSaveButton(context, views, widgetId, sentenceId)
                    
                    // Update widget on main thread
                    CoroutineScope(Dispatchers.Main).launch {
                        appWidgetManager.updateAppWidget(widgetId, views)
                        android.util.Log.d("GermanLearningWidget", "Widget $widgetId updated with new bookmark state")
                    }
                } else {
                    android.util.Log.e("GermanLearningWidget", "CRITICAL ERROR: Could not find sentence with ID $sentenceId. Save button will not work.")
                    
                    // LAST RESORT: Refresh the entire widget to try to recover
                    android.util.Log.w("GermanLearningWidget", "Attempting widget refresh as last resort")
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidget(context, appWidgetManager, widgetId, forceNewSentence = false)
                }
            } catch (e: Exception) {
                android.util.Log.e("GermanLearningWidget", "Error handling save sentence", e)
                
                // ERROR RECOVERY: Try to refresh the widget in case of any errors
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateWidget(context, appWidgetManager, widgetId, forceNewSentence = false)
                } catch (recoveryException: Exception) {
                    android.util.Log.e("GermanLearningWidget", "Error recovery also failed", recoveryException)
                }
            }
        }
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up tracked sentences when widgets are deleted
        appWidgetIds.forEach { 
            currentSentences.remove(it)
            android.util.Log.d("GermanLearningWidget", "Cleaned up sentence for deleted widget $it")
        }
    }
} 