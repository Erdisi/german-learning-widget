package com.germanleraningwidget.util

/**
 * Centralized constants for the German Learning Widget app.
 * 
 * This file consolidates all app-wide constants to prevent duplication
 * and ensure consistency across the codebase.
 */
object AppConstants {
    
    // Widget Configuration
    const val FIXED_UPDATE_INTERVAL_MINUTES = 90
    const val FIXED_SENTENCES_PER_DAY = 10
    
    // Performance and Caching
    const val CACHE_VALIDITY_MS = 30000L // 30 seconds
    const val PERFORMANCE_LOG_INTERVAL_MS = 30000L // 30 seconds
    const val MEMORY_WARNING_THRESHOLD = 0.8f // 80% of max memory
    const val MEMORY_PRESSURE_THRESHOLD = 0.85f // 85% memory usage
    const val SLOW_OPERATION_THRESHOLD_MS = 500L
    const val MONITORING_INTERVAL_MS = 10000L // 10 seconds
    
    // Text Sizing Constants
    const val BASE_GERMAN_SIZE = 18f
    const val BASE_TRANSLATION_SIZE = 14f
    const val HERO_BASE_GERMAN_SIZE = 22f
    const val HERO_BASE_TRANSLATION_SIZE = 16f
    
    const val MIN_GERMAN_SIZE = 14f
    const val MAX_GERMAN_SIZE = 24f
    const val MIN_TRANSLATION_SIZE = 11f
    const val MAX_TRANSLATION_SIZE = 18f
    
    const val MIN_HERO_GERMAN_SIZE = 18f
    const val MAX_HERO_GERMAN_SIZE = 28f
    const val MIN_HERO_TRANSLATION_SIZE = 14f
    const val MAX_HERO_TRANSLATION_SIZE = 20f
    
    // Text Length Thresholds
    const val SHORT_TEXT_THRESHOLD = 15
    const val MEDIUM_TEXT_THRESHOLD = 30
    const val LONG_TEXT_THRESHOLD = 50
    const val VERY_LONG_TEXT_THRESHOLD = 80
    
    // Logging Configuration
    const val MAX_LOG_ENTRIES = 1000
    const val MAX_ERROR_ENTRIES = 100
    const val TAG_PREFIX = "GLW_"
    
    // DataStore Keys
    const val BOOKMARKED_IDS_KEY_NAME = "bookmarked_sentence_ids"
    
    // Worker Configuration
    const val SENTENCE_DELIVERY_WORK_NAME = "SentenceDeliveryWork"
    
    // Widget Actions
    object WidgetActions {
        const val ACTION_SAVE_SENTENCE = "com.germanleraningwidget.SAVE_SENTENCE"
        const val ACTION_NEXT_BOOKMARK = "com.germanleraningwidget.ACTION_NEXT_BOOKMARK"
        const val ACTION_REMOVE_BOOKMARK = "com.germanleraningwidget.ACTION_REMOVE_BOOKMARK"
        
        // Hero Widget Actions
        const val ACTION_NEXT_BOOKMARK_HERO = "com.germanleraningwidget.hero.ACTION_NEXT_BOOKMARK"
        const val ACTION_PREVIOUS_BOOKMARK_HERO = "com.germanleraningwidget.hero.ACTION_PREVIOUS_BOOKMARK"
        const val ACTION_REMOVE_BOOKMARK_HERO = "com.germanleraningwidget.hero.ACTION_REMOVE_BOOKMARK"
        const val ACTION_SELECT_BOOKMARK_HERO = "com.germanleraningwidget.hero.ACTION_SELECT_BOOKMARK"
    }
    
    // Intent Extras
    object IntentExtras {
        const val EXTRA_WIDGET_ID = "widget_id"
        const val EXTRA_SENTENCE_ID = "sentence_id"
        const val EXTRA_TARGET_INDEX = "extra_target_index"
        const val EXTRA_NAVIGATE_TO = "navigate_to"
    }
    
    // Navigation Routes
    object Routes {
        const val ONBOARDING = "onboarding"
        const val HOME = "home"
        const val BOOKMARKS = "bookmarks"
        const val SETTINGS = "settings"
        const val LEARNING_PREFERENCES = "learning_preferences"
        const val WIDGET_CUSTOMIZATION = "widget_customization"
        const val WIDGET_DETAILS = "widget_details"
    }
    
    // App Configuration
    object Config {
        const val ENABLE_PERFORMANCE_MONITORING = true
        const val ENABLE_MEMORY_MONITORING = true
        const val ENABLE_DEBUG_LOGGING = true
        const val DEFAULT_NATIVE_LANGUAGE = "English"
    }
    
    // Performance Operation Names
    object PerformanceOperations {
        const val WORKER_EXECUTION = "worker_execution"
        const val SENTENCE_FETCH = "sentence_fetch"
        const val WIDGET_UPDATE = "widget_update"
        const val DAILY_POOL_GENERATION = "daily_pool_generation"
        const val GET_RANDOM_SENTENCE = "get_random_sentence"
        const val GET_DISTRIBUTED_SENTENCES = "get_distributed_sentences"
        const val SAVE_BOOKMARK = "save_bookmark"
        const val REMOVE_BOOKMARK = "remove_bookmark"
        const val GENERATE_DAILY_POOL = "generate_daily_pool"
        const val LOAD_PREFERENCES = "load_preferences"
        const val CHECK_POOL_REGENERATION = "check_pool_regeneration"
    }
} 