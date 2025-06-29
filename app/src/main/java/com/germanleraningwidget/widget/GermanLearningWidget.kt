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
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
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
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    /**
     * Single method to update a widget with current data and customizations.
     */
    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                
                // Apply customizations using centralized helper
                val customization = WidgetCustomizationHelper.applyCustomizations(
                    context, views, WidgetType.MAIN, R.id.widget_container
                )
                
                // Load repositories
                val sentenceRepository = SentenceRepository.getInstance(context)
                val preferencesRepository = UserPreferencesRepository(context)
                val preferences = preferencesRepository.userPreferences.first()
                
                // Get sentence data using multi-level support
                val sentence = sentenceRepository.getRandomSentenceFromLevels(
                    levels = preferences.selectedGermanLevels,
                    topics = preferences.selectedTopics.toList()
                )
                
                if (sentence != null) {
                    // Store current sentence
                    currentSentences[appWidgetId] = sentence
                    android.util.Log.d("GermanLearningWidget", "Stored sentence ${sentence.id} for widget $appWidgetId")
                    
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
                    
                    // Set save button state
                    val isSaved = sentenceRepository.isSentenceSaved(sentence.id)
                    views.setImageViewResource(
                        R.id.widget_save_button,
                        if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                    )
                    
                    // Set up save button click
                    setupSaveButton(context, views, appWidgetId, sentence.id)
                } else {
                    // Create a default sentence for the save button to work
                    val defaultSentence = GermanSentence(
                        id = 999L, // Special ID for default sentence
                        germanText = "Guten Tag!",
                        translation = "Good day!",
                        level = preferences.primaryGermanLevel,
                        topic = "Greetings"
                    )
                    
                    // Store default sentence
                    currentSentences[appWidgetId] = defaultSentence
                    
                    // Show default content with customizations
                    views.setTextViewText(R.id.widget_german_text, defaultSentence.germanText)
                    views.setTextViewText(R.id.widget_translation, defaultSentence.translation)
                    views.setTextViewText(R.id.widget_topic, defaultSentence.topic)
                    views.setTextViewText(R.id.widget_level_indicator, defaultSentence.level)
                    
                    // Apply automatic text customizations to default content
                    WidgetCustomizationHelper.applyAutoTextCustomizations(
                        views, customization,
                        R.id.widget_german_text, R.id.widget_translation,
                        defaultSentence.germanText, defaultSentence.translation,
                        isHeroWidget = false
                    )
                    
                    // Set save button state for default sentence
                    val isSaved = sentenceRepository.isSentenceSaved(defaultSentence.id)
                    views.setImageViewResource(
                        R.id.widget_save_button,
                        if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                    )
                    
                    // Set up save button click for default sentence
                    setupSaveButton(context, views, appWidgetId, defaultSentence.id)
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
                views.setTextViewText(R.id.widget_level_indicator, "A1")
                
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
                
                if (appWidgetIds != null) {
                    if (germanText != null && translation != null) {
                        // Custom update with specific sentence data
                        updateWidgetsWithSentence(context, appWidgetIds, germanText, translation, intent)
                    } else {
                        // Regular update - refresh all widgets
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        for (appWidgetId in appWidgetIds) {
                            updateWidget(context, appWidgetManager, appWidgetId)
                        }
                    }
                }
            }
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
                    val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                    
                    // Create sentence object for tracking
                    val sentence = GermanSentence(
                        id = sentenceId,
                        germanText = germanText,
                        translation = translation,
                        level = level,
                        topic = topic ?: "Unknown"
                    )
                    
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
     * Handle save/unsave sentence action.
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
                
                if (currentSentence != null && currentSentence.id == sentenceId) {
                    android.util.Log.d("GermanLearningWidget", "Toggling save state for sentence: ${currentSentence.germanText}")
                    val isNowSaved = sentenceRepository.toggleSaveSentence(currentSentence)
                    
                    android.util.Log.d("GermanLearningWidget", "Sentence is now saved: $isNowSaved")
                    
                    // Rebuild the entire widget with proper customizations
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                    
                    // Apply current customizations
                    val customization = WidgetCustomizationHelper.applyCustomizations(
                        context, views, WidgetType.MAIN, R.id.widget_container
                    )
                    
                    // Restore current content
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
                    android.util.Log.e("GermanLearningWidget", "Could not find sentence for widget $widgetId or sentence ID mismatch. Expected: $sentenceId, Current sentence: $currentSentence")
                    
                    // Fallback: try to find the sentence in the repository and update
                    if (sentenceId == 999L) {
                        // Handle default sentence
                        val defaultSentence = GermanSentence(
                            id = 999L,
                            germanText = "Guten Tag!",
                            translation = "Good day!",
                            level = "A1",
                            topic = "Greetings"
                        )
                        currentSentences[widgetId] = defaultSentence
                        sentenceRepository.toggleSaveSentence(defaultSentence)
                        
                        // Update widget with default sentence
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        updateWidget(context, appWidgetManager, widgetId)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GermanLearningWidget", "Error handling save sentence", e)
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