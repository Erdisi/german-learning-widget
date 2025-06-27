package com.germanleraningwidget

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager

class GermanLearningApplication : Application(), Configuration.Provider {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize WorkManager manually since we disabled automatic initialization
        WorkManager.initialize(this, workManagerConfiguration)
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
} 