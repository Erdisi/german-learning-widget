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
import com.germanleraningwidget.data.repository.SentenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Hero-style bookmarks widget implementing Material Design 3 Hero carousel layout.
 * Features a prominent central bookmark with smaller preview items on the sides,
 * providing an elegant and immersive bookmark browsing experience.
 */
class BookmarksHeroWidget : AppWidgetProvider() {
    
    companion object {
        private const val ACTION_NEXT_BOOKMARK = "com.germanleraningwidget.hero.ACTION_NEXT_BOOKMARK"
        private const val ACTION_PREVIOUS_BOOKMARK = "com.germanleraningwidget.hero.ACTION_PREVIOUS_BOOKMARK"
        private const val ACTION_REMOVE_BOOKMARK = "com.germanleraningwidget.hero.ACTION_REMOVE_BOOKMARK"
        private const val ACTION_SELECT_BOOKMARK = "com.germanleraningwidget.hero.ACTION_SELECT_BOOKMARK"
        private const val EXTRA_WIDGET_ID = "extra_widget_id"
        private const val EXTRA_SENTENCE_ID = "extra_sentence_id"
        private const val EXTRA_TARGET_INDEX = "extra_target_index"
        
        // Track current sentence index for each widget
        private val currentIndices = ConcurrentHashMap<Int, Int>()
        
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, BookmarksHeroWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            if (appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, BookmarksHeroWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        android.util.Log.d("BookmarksHeroWidget", "onUpdate called for ${appWidgetIds.size} widgets")
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        android.util.Log.d("BookmarksHeroWidget", "Updating hero bookmarks widget $appWidgetId")
        
        val views = RemoteViews(context.packageName, R.layout.widget_bookmarks_hero)
        
        // Create intent to open the app when widget container is tapped
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
        views.setOnClickPendingIntent(R.id.widget_hero_container, pendingIntent)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                
                android.util.Log.d("BookmarksHeroWidget", "Found ${bookmarkedSentences.size} bookmarked sentences")
                
                if (bookmarkedSentences.isEmpty()) {
                    showEmptyState(views)
                } else {
                    // Get current index for this widget
                    val currentIndex = currentIndices.getOrDefault(appWidgetId, 0)
                    val validIndex = if (currentIndex >= bookmarkedSentences.size) 0 else currentIndex
                    currentIndices[appWidgetId] = validIndex
                    
                    updateHeroLayout(context, views, bookmarkedSentences, validIndex, appWidgetId)
                }
                
                // Update the widget on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    android.util.Log.d("BookmarksHeroWidget", "Hero widget $appWidgetId updated successfully")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error updating widget $appWidgetId", e)
                showErrorState(views)
                
                CoroutineScope(Dispatchers.Main).launch {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
    
    private fun showEmptyState(views: RemoteViews) {
        // Hide hero layout and show empty state
        views.setInt(R.id.widget_hero_content, "setVisibility", android.view.View.GONE)
        views.setInt(R.id.widget_hero_empty_state, "setVisibility", android.view.View.VISIBLE)
        
        views.setTextViewText(R.id.widget_hero_empty_title, "No bookmarks yet")
        views.setTextViewText(R.id.widget_hero_empty_subtitle, "Save sentences from the learning widget to see them here")
    }
    
    private fun showErrorState(views: RemoteViews) {
        views.setInt(R.id.widget_hero_content, "setVisibility", android.view.View.GONE)
        views.setInt(R.id.widget_hero_empty_state, "setVisibility", android.view.View.VISIBLE)
        
        views.setTextViewText(R.id.widget_hero_empty_title, "Error loading bookmarks")
        views.setTextViewText(R.id.widget_hero_empty_subtitle, "Tap to open app and try again")
    }
    
    private fun updateHeroLayout(
        context: Context,
        views: RemoteViews,
        bookmarkedSentences: List<GermanSentence>,
        currentIndex: Int,
        appWidgetId: Int
    ) {
        // Show hero content layout
        views.setInt(R.id.widget_hero_content, "setVisibility", android.view.View.VISIBLE)
        views.setInt(R.id.widget_hero_empty_state, "setVisibility", android.view.View.GONE)
        
        val currentSentence = bookmarkedSentences[currentIndex]
        val totalCount = bookmarkedSentences.size
        
        // Update main hero content
        views.setTextViewText(R.id.widget_hero_german_text, currentSentence.germanText)
        views.setTextViewText(R.id.widget_hero_translation, currentSentence.translation)
        views.setTextViewText(R.id.widget_hero_topic, currentSentence.topic)
        views.setTextViewText(R.id.widget_hero_counter, "${currentIndex + 1}/$totalCount")
        
        // Update side previews (if more than one bookmark exists)
        if (totalCount > 1) {
            views.setInt(R.id.widget_hero_previews, "setVisibility", android.view.View.VISIBLE)
            
            // Left preview (previous item)
            val prevIndex = if (currentIndex == 0) totalCount - 1 else currentIndex - 1
            val prevSentence = bookmarkedSentences[prevIndex]
            views.setTextViewText(R.id.widget_hero_prev_text, truncateText(prevSentence.germanText, 25))
            
            // Right preview (next item)  
            val nextIndex = (currentIndex + 1) % totalCount
            val nextSentence = bookmarkedSentences[nextIndex]
            views.setTextViewText(R.id.widget_hero_next_text, truncateText(nextSentence.germanText, 25))
            
            // Set up preview click handlers
            setupPreviewClickHandlers(context, views, appWidgetId, prevIndex, nextIndex)
        } else {
            views.setInt(R.id.widget_hero_previews, "setVisibility", android.view.View.GONE)
        }
        
        // Set up navigation buttons
        setupNavigationButtons(context, views, appWidgetId, totalCount > 1)
        
        // Set up action buttons
        setupActionButtons(context, views, appWidgetId, currentSentence.id)
        
        // Update progress indicators
        updateProgressIndicators(views, currentIndex, totalCount)
    }
    
    private fun setupPreviewClickHandlers(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        prevIndex: Int,
        nextIndex: Int
    ) {
        // Previous preview click
        val prevIntent = Intent(context, BookmarksHeroWidget::class.java).apply {
            action = ACTION_SELECT_BOOKMARK
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_TARGET_INDEX, prevIndex)
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 1000 + prevIndex,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_hero_prev_preview, prevPendingIntent)
        
        // Next preview click
        val nextIntent = Intent(context, BookmarksHeroWidget::class.java).apply {
            action = ACTION_SELECT_BOOKMARK
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_TARGET_INDEX, nextIndex)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 1000 + nextIndex,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_hero_next_preview, nextPendingIntent)
    }
    
    private fun setupNavigationButtons(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        hasMultipleItems: Boolean
    ) {
        if (hasMultipleItems) {
            views.setInt(R.id.widget_hero_nav_buttons, "setVisibility", android.view.View.VISIBLE)
            
            // Previous button
            val prevIntent = Intent(context, BookmarksHeroWidget::class.java).apply {
                action = ACTION_PREVIOUS_BOOKMARK
                putExtra(EXTRA_WIDGET_ID, appWidgetId)
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100,
                prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_hero_prev_button, prevPendingIntent)
            
            // Next button
            val nextIntent = Intent(context, BookmarksHeroWidget::class.java).apply {
                action = ACTION_NEXT_BOOKMARK
                putExtra(EXTRA_WIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 1,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_hero_next_button, nextPendingIntent)
        } else {
            views.setInt(R.id.widget_hero_nav_buttons, "setVisibility", android.view.View.GONE)
        }
    }
    
    private fun setupActionButtons(
        context: Context,
        views: RemoteViews,
        appWidgetId: Int,
        sentenceId: Long
    ) {
        // Remove bookmark button
        val removeIntent = Intent(context, BookmarksHeroWidget::class.java).apply {
            action = ACTION_REMOVE_BOOKMARK
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            putExtra(EXTRA_SENTENCE_ID, sentenceId)
        }
        val removePendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId * 100 + 2,
            removeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_hero_remove_button, removePendingIntent)
    }
    
    private fun updateProgressIndicators(views: RemoteViews, currentIndex: Int, totalCount: Int) {
        // Update progress dots (show up to 5 dots)
        val maxDots = minOf(5, totalCount)
        val dotIds = arrayOf(
            R.id.widget_hero_dot_1,
            R.id.widget_hero_dot_2,
            R.id.widget_hero_dot_3,
            R.id.widget_hero_dot_4,
            R.id.widget_hero_dot_5
        )
        
        for (i in 0 until maxDots) {
            views.setInt(dotIds[i], "setVisibility", android.view.View.VISIBLE)
            
            // Highlight current dot
            val isActive = when {
                totalCount <= 5 -> i == currentIndex
                currentIndex < 3 -> i == currentIndex
                currentIndex >= totalCount - 2 -> i == (maxDots - (totalCount - currentIndex))
                else -> i == 2 // Middle dot when scrolling through many items
            }
            
            views.setInt(
                dotIds[i],
                "setBackgroundResource",
                if (isActive) R.drawable.widget_dot_active else R.drawable.widget_dot_inactive
            )
        }
        
        // Hide unused dots
        for (i in maxDots until dotIds.size) {
            views.setInt(dotIds[i], "setVisibility", android.view.View.GONE)
        }
    }
    
    private fun truncateText(text: String, maxLength: Int): String {
        return if (text.length > maxLength) {
            text.take(maxLength - 3) + "..."
        } else {
            text
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("BookmarksHeroWidget", "Received action: ${intent.action}")
        
        when (intent.action) {
            ACTION_NEXT_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    handleNextBookmark(context, widgetId)
                }
            }
            ACTION_PREVIOUS_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                if (widgetId != -1) {
                    handlePreviousBookmark(context, widgetId)
                }
            }
            ACTION_SELECT_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val targetIndex = intent.getIntExtra(EXTRA_TARGET_INDEX, -1)
                if (widgetId != -1 && targetIndex != -1) {
                    handleSelectBookmark(context, widgetId, targetIndex)
                }
            }
            ACTION_REMOVE_BOOKMARK -> {
                val widgetId = intent.getIntExtra(EXTRA_WIDGET_ID, -1)
                val sentenceId = intent.getLongExtra(EXTRA_SENTENCE_ID, -1L)
                if (widgetId != -1 && sentenceId != -1L) {
                    handleRemoveBookmark(context, widgetId, sentenceId)
                }
            }
        }
    }
    
    private fun handleNextBookmark(context: Context, widgetId: Int) {
        android.util.Log.d("BookmarksHeroWidget", "Next bookmark clicked for widget $widgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                
                if (bookmarkedSentences.isNotEmpty()) {
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0)
                    val nextIndex = (currentIndex + 1) % bookmarkedSentences.size
                    currentIndices[widgetId] = nextIndex
                    
                    android.util.Log.d("BookmarksHeroWidget", "Moving to next bookmark: $nextIndex/${bookmarkedSentences.size}")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error handling next bookmark", e)
            }
        }
    }
    
    private fun handlePreviousBookmark(context: Context, widgetId: Int) {
        android.util.Log.d("BookmarksHeroWidget", "Previous bookmark clicked for widget $widgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                
                if (bookmarkedSentences.isNotEmpty()) {
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0)
                    val prevIndex = if (currentIndex == 0) bookmarkedSentences.size - 1 else currentIndex - 1
                    currentIndices[widgetId] = prevIndex
                    
                    android.util.Log.d("BookmarksHeroWidget", "Moving to previous bookmark: $prevIndex/${bookmarkedSentences.size}")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error handling previous bookmark", e)
            }
        }
    }
    
    private fun handleSelectBookmark(context: Context, widgetId: Int, targetIndex: Int) {
        android.util.Log.d("BookmarksHeroWidget", "Select bookmark clicked: index $targetIndex for widget $widgetId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                
                if (targetIndex >= 0 && targetIndex < bookmarkedSentences.size) {
                    currentIndices[widgetId] = targetIndex
                    
                    android.util.Log.d("BookmarksHeroWidget", "Selected bookmark: $targetIndex/${bookmarkedSentences.size}")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error handling select bookmark", e)
            }
        }
    }
    
    private fun handleRemoveBookmark(context: Context, widgetId: Int, sentenceId: Long) {
        android.util.Log.d("BookmarksHeroWidget", "Remove bookmark clicked for sentence $sentenceId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sentenceRepository = SentenceRepository.getInstance(context)
                val bookmarkedSentences = sentenceRepository.getSavedSentences()
                val sentenceToRemove = bookmarkedSentences.find { it.id == sentenceId }
                
                if (sentenceToRemove != null) {
                    // Remove the bookmark
                    sentenceRepository.toggleSaveSentence(sentenceToRemove)
                    
                    // Adjust current index if needed
                    val currentIndex = currentIndices.getOrDefault(widgetId, 0)
                    val newBookmarkedSentences = sentenceRepository.getSavedSentences()
                    
                    if (newBookmarkedSentences.isEmpty()) {
                        currentIndices[widgetId] = 0
                    } else if (currentIndex >= newBookmarkedSentences.size) {
                        currentIndices[widgetId] = 0
                    }
                    
                    android.util.Log.d("BookmarksHeroWidget", "Bookmark removed, updating widget")
                    
                    // Update the widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateAppWidget(context, appWidgetManager, widgetId)
                    
                    // Also update the main learning widget to reflect bookmark change
                    GermanLearningWidget.updateAllWidgets(context)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookmarksHeroWidget", "Error removing bookmark", e)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        // Clean up tracked indices when widgets are deleted
        appWidgetIds.forEach { currentIndices.remove(it) }
    }
} 