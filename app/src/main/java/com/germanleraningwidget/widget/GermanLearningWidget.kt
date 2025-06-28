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
                    
                    // Set text content
                    views.setTextViewText(R.id.widget_german_text, sentence.germanText)
                    views.setTextViewText(R.id.widget_translation, sentence.translation)
                    views.setTextViewText(R.id.widget_topic, sentence.topic)
                    views.setTextViewText(R.id.widget_level_indicator, sentence.level) // Show actual sentence level
                    
                    // Apply text customizations
                    WidgetCustomizationHelper.applyTextCustomizations(
                        views, customization,
                        R.id.widget_german_text, R.id.widget_translation,
                        18f, 14f // Base sizes for main widget
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
                    // Show default content with customizations
                    views.setTextViewText(R.id.widget_german_text, "Guten Tag!")
                    views.setTextViewText(R.id.widget_translation, "Good day!")
                    views.setTextViewText(R.id.widget_topic, "Greetings")
                    views.setTextViewText(R.id.widget_level_indicator, preferences.primaryGermanLevel) // Show primary level
                    
                    // Apply text customizations to default content
                    WidgetCustomizationHelper.applyTextCustomizations(
                        views, customization,
                        R.id.widget_german_text, R.id.widget_translation,
                        18f, 14f
                    )
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
        val saveIntent = Intent(context, GermanLearningWidget::class.java).apply {
            action = ACTION_SAVE_SENTENCE
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_SENTENCE_ID, sentenceId)
        }
        val savePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId,
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
        
        when (intent.action) {
            ACTION_SAVE_SENTENCE -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val sentenceId = intent.getLongExtra(EXTRA_SENTENCE_ID, -1L)
                
                if (widgetId != -1 && sentenceId != -1L) {
                    handleSaveSentence(context, widgetId, sentenceId)
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
                
                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                    
                    // Apply customizations
                    val customization = WidgetCustomizationHelper.applyCustomizations(
                        context, views, WidgetType.MAIN, R.id.widget_container
                    )
                    
                    // Set content
                    views.setTextViewText(R.id.widget_german_text, germanText)
                    views.setTextViewText(R.id.widget_translation, translation)
                    views.setTextViewText(R.id.widget_topic, topic ?: "")
                    
                    // Get user level for display
                    val preferencesRepository = UserPreferencesRepository(context)
                    val preferences = preferencesRepository.userPreferences.first()
                    views.setTextViewText(R.id.widget_level_indicator, preferences.primaryGermanLevel)
                    
                    // Apply text customizations
                    WidgetCustomizationHelper.applyTextCustomizations(
                        views, customization,
                        R.id.widget_german_text, R.id.widget_translation,
                        18f, 14f
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val currentSentence = currentSentences[widgetId]
                
                if (currentSentence != null && currentSentence.id == sentenceId) {
                    val isNowSaved = sentenceRepository.toggleSaveSentence(currentSentence)
                    
                    // Update just the save button
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.widget_german_learning)
                    
                    // Apply current customizations
                    WidgetCustomizationHelper.applyCustomizations(
                        context, views, WidgetType.MAIN, R.id.widget_container
                    )
                    
                    // Restore current content
                    views.setTextViewText(R.id.widget_german_text, currentSentence.germanText)
                    views.setTextViewText(R.id.widget_translation, currentSentence.translation)
                    views.setTextViewText(R.id.widget_topic, currentSentence.topic)
                    
                    // Update save button
                    views.setImageViewResource(
                        R.id.widget_save_button,
                        if (isNowSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_border
                    )
                    
                    setupMainClick(context, views, widgetId)
                    setupSaveButton(context, views, widgetId, sentenceId)
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        appWidgetManager.updateAppWidget(widgetId, views)
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
        appWidgetIds.forEach { currentSentences.remove(it) }
    }
} 