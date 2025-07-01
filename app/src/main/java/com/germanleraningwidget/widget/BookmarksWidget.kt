package com.germanleraningwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.germanleraningwidget.MainActivity
import com.germanleraningwidget.R
import com.germanleraningwidget.data.model.GermanSentence
import com.germanleraningwidget.data.model.WidgetType
import com.germanleraningwidget.data.repository.SentenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BookmarksWidget : AppWidgetProvider() {
    
    companion object {
        private const val ACTION_NEXT_BOOKMARK = "com.germanleraningwidget.ACTION_NEXT_BOOKMARK"
        private const val ACTION_REMOVE_BOOKMARK = "com.germanleraningwidget.ACTION_REMOVE_BOOKMARK"
        private const val EXTRA_WIDGET_ID = "extra_widget_id"
        private const val EXTRA_SENTENCE_ID = "extra_sentence_id"
        
        // Track current sentence index for each widget
        private val currentIndices = ConcurrentHashMap<Int, Int>()
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BookmarksWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: intArrayOf() // Handle potential null return
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, BookmarksWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_bookmarks)
                
                // Apply customizations using centralized helper
                val customization = WidgetCustomizationHelper.applyCustomizations(
                    context, views, WidgetType.BOOKMARKS, R.id.widget_bookmarks_container
                )
                
                // Set up main click intent
                setupMainClick(context, views, appWidgetId)
                
                // Load bookmarks with null safety
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = try {
                    sentenceRepository.getSavedSentences() ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("BookmarksWidget", "Error loading bookmarks", e)
                    emptyList()
                }
                
                if (bookmarkedSentences.isEmpty()) {
                    showEmptyState(views, customization)
                } else {
                    // Get current index for this widget, ensuring it's valid
                    val currentIndex = currentIndices.getOrDefault(appWidgetId, 0)
                    val validIndex = if (currentIndex >= bookmarkedSentences.size) 0 else currentIndex.coerceAtLeast(0)
                    currentIndices[appWidgetId] = validIndex
                    
                    // Safe array access with bounds checking
                    val currentSentence = bookmarkedSentences.getOrNull(validIndex) ?: run {
                        android.util.Log.w("BookmarksWidget", "Invalid index $validIndex for ${bookmarkedSentences.size} bookmarks")
                        showEmptyState(views, customization)
                        return@launch
                    }
                    
                    // Update counter and content
                    views.setTextViewText(R.id.widget_bookmarks_counter, "${validIndex + 1}/${bookmarkedSentences.size}")
                    views.setTextViewText(R.id.widget_bookmarks_german_text, currentSentence.germanText)
                    views.setTextViewText(R.id.widget_bookmarks_translation, currentSentence.translation)
                    views.setTextViewText(R.id.widget_bookmarks_topic, currentSentence.topic)
                    
                    // Apply automatic text customizations based on content
                    WidgetCustomizationHelper.applyAutoTextCustomizations(
                        views, customization,
                        R.id.widget_bookmarks_german_text, R.id.widget_bookmarks_translation,
                        currentSentence.germanText, currentSentence.translation,
                        isHeroWidget = false
                    )
                    
                    // Apply colors to secondary text elements (title, counter, topic)
                    WidgetCustomizationHelper.applySecondaryTextColors(
                        views, customization,
                        titleTextViewId = R.id.widget_bookmarks_title, // Now includes the bookmarks title
                        levelTextViewId = null,
                        topicTextViewId = R.id.widget_bookmarks_topic,
                        counterTextViewId = R.id.widget_bookmarks_counter
                    )
                    
                    // Apply dynamic backgrounds for better visibility
                    WidgetCustomizationHelper.applyDynamicBackgrounds(
                        views, customization,
                        topicTextViewId = R.id.widget_bookmarks_topic,
                        levelTextViewId = null,
                        buttonId = R.id.widget_bookmarks_next_button // Apply to next button for visibility
                    )
                    
                    // Set up action buttons
                    setupNextButton(context, views, appWidgetId)
                    setupRemoveButton(context, views, appWidgetId, currentSentence.id)
                }
                
                // Update the widget on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("BookmarksWidget", "Error updating widget $appWidgetId", e)
                showErrorState(context, appWidgetManager, appWidgetId)
            }
        }
    }
    
    /**
     * Set up main container click to open bookmarks screen.
     */
    private fun setupMainClick(context: Context, views: RemoteViews, appWidgetId: Int) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, "bookmarks")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_bookmarks_container, pendingIntent)
    }
    
    /**
     * Set up next button to cycle through bookmarks.
     */
    private fun setupNextButton(context: Context, views: RemoteViews, appWidgetId: Int) {
        val nextIntent = Intent(context, BookmarksWidget::class.java).apply {
            action = ACTION_NEXT_BOOKMARK
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_bookmarks_next_button, nextPendingIntent)
    }
    
    /**
     * Set up remove button to remove current bookmark.
     */
    private fun setupRemoveButton(context: Context, views: RemoteViews, appWidgetId: Int, sentenceId: Long) {
        val removeIntent = Intent(context, BookmarksWidget::class.java).apply {
            action = ACTION_REMOVE_BOOKMARK
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_SENTENCE_ID, sentenceId)
        }
        val removePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 1,
            removeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_bookmarks_remove_button, removePendingIntent)
    }
    
    /**
     * Show empty state when no bookmarks exist.
     */
    private fun showEmptyState(views: RemoteViews, customization: com.germanleraningwidget.data.model.WidgetCustomization) {
        views.setTextViewText(R.id.widget_bookmarks_counter, "0/0")
        views.setTextViewText(R.id.widget_bookmarks_german_text, "No bookmarks yet")
        views.setTextViewText(R.id.widget_bookmarks_translation, "Save sentences to see them here")
        views.setTextViewText(R.id.widget_bookmarks_topic, "")
        
        // Apply automatic text customizations to empty state
        WidgetCustomizationHelper.applyAutoTextCustomizations(
            views, customization,
            R.id.widget_bookmarks_german_text, R.id.widget_bookmarks_translation,
            "No bookmarks yet", "Save sentences to see them here",
            isHeroWidget = false
        )
        
        // Apply colors to secondary text elements (title, counter, topic)
        WidgetCustomizationHelper.applySecondaryTextColors(
            views, customization,
            titleTextViewId = R.id.widget_bookmarks_title, // Now includes the bookmarks title
            levelTextViewId = null,
            topicTextViewId = R.id.widget_bookmarks_topic,
            counterTextViewId = R.id.widget_bookmarks_counter
        )
        
        // Apply dynamic backgrounds for better visibility
        WidgetCustomizationHelper.applyDynamicBackgrounds(
            views, customization,
            topicTextViewId = R.id.widget_bookmarks_topic,
            levelTextViewId = null,
            buttonId = R.id.widget_bookmarks_next_button
        )
    }
    
    /**
     * Show error state.
     */
    private fun showErrorState(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_bookmarks)
                
                // Apply basic customization
                WidgetCustomizationHelper.applyCustomizations(
                    context, views, WidgetType.BOOKMARKS, R.id.widget_bookmarks_container
                )
                
                views.setTextViewText(R.id.widget_bookmarks_counter, "0/0")
                views.setTextViewText(R.id.widget_bookmarks_german_text, "Error loading")
                views.setTextViewText(R.id.widget_bookmarks_translation, "Tap to open app")
                views.setTextViewText(R.id.widget_bookmarks_topic, "")
                
                setupMainClick(context, views, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                android.util.Log.e("BookmarksWidget", "Error showing error state", e)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        when (intent.action) {
            ACTION_NEXT_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    handleNextBookmark(context, widgetId)
                }
            }
            ACTION_REMOVE_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val sentenceId = intent.getLongExtra(EXTRA_SENTENCE_ID, -1L)
                if (widgetId != -1 && sentenceId != -1L) {
                    handleRemoveBookmark(context, widgetId, sentenceId)
                }
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (appWidgetIds != null) {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    for (appWidgetId in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
        }
    }
    
    /**
     * Handle next bookmark button click.
     */
    private fun handleNextBookmark(context: Context, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = try {
                    sentenceRepository.getSavedSentences() ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("BookmarksWidget", "Error loading bookmarks in handleNextBookmark", e)
                    emptyList()
                }
                
                if (bookmarkedSentences.isNotEmpty()) {
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0).coerceAtLeast(0)
                    val nextIndex = ((currentIndex + 1) % bookmarkedSentences.size).coerceAtLeast(0)
                    currentIndices[widgetId] = nextIndex
                    
                    // Update widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksWidget", "Error handling next bookmark", e)
            }
        }
    }
    
    /**
     * Handle remove bookmark button click.
     */
    private fun handleRemoveBookmark(context: Context, widgetId: Int, sentenceId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = try {
                    sentenceRepository.getSavedSentences() ?: emptyList()
                } catch (e: Exception) {
                    android.util.Log.e("BookmarksWidget", "Error loading bookmarks in handleRemoveBookmark", e)
                    emptyList()
                }
                
                // Find and remove the sentence
                val sentenceToRemove = bookmarkedSentences.find { it.id == sentenceId }
                if (sentenceToRemove != null) {
                    sentenceRepository.toggleSaveSentence(sentenceToRemove) // This will remove it
                    
                    // Adjust current index if needed
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0).coerceAtLeast(0)
                    val newBookmarkedSentences = try {
                        sentenceRepository.getSavedSentences() ?: emptyList()
                    } catch (e: Exception) {
                        android.util.Log.e("BookmarksWidget", "Error loading new bookmarks", e)
                        emptyList()
                    }
                    
                    if (newBookmarkedSentences.isEmpty()) {
                        currentIndices[widgetId] = 0
                    } else if (currentIndex >= newBookmarkedSentences.size) {
                        currentIndices[widgetId] = 0
                    }
                    
                    // Update widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                    
                    // Also update other widgets
                    GermanLearningWidget.updateAllWidgets(context)
                    BookmarksHeroWidget.updateAllWidgets(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksWidget", "Error handling remove bookmark", e)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up tracked indices when widgets are deleted
        appWidgetIds.forEach { currentIndices.remove(it) }
    }
} 