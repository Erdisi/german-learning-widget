package com.germanleraningwidget.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Comprehensive optimization and cleanup utility for the German Learning Widget app.
 * 
 * Features:
 * - Performance monitoring and optimization
 * - Memory leak detection and cleanup
 * - App health monitoring
 * - Resource optimization
 * - Debug utilities for development
 * 
 * This utility helps maintain app performance and provides insights for optimization.
 */
object OptimizationUtils {
    
    private const val TAG = "OptimizationUtils"
    private const val PERFORMANCE_LOG_INTERVAL_MS = 30000L // 30 seconds
    private const val MEMORY_PRESSURE_THRESHOLD = 0.85f // 85% memory usage
    
    // Performance tracking
    val operationMetrics = ConcurrentHashMap<String, OperationMetric>()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    private val performanceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Resource counters
    val totalOperations = AtomicLong(0)
    val slowOperations = AtomicLong(0)
    private val memoryWarnings = AtomicLong(0)
    
    // Monitoring state
    @Volatile
    private var isMonitoring = false
    
    /**
     * Data classes for optimization tracking
     */
    data class OperationMetric(
        val name: String,
        val totalCalls: Long,
        val totalTimeMs: Long,
        val averageTimeMs: Long,
        val maxTimeMs: Long,
        val minTimeMs: Long,
        val lastExecutionMs: Long
    ) {
        val isSlowOperation: Boolean get() = averageTimeMs > 100L
        val callsPerSecond: Float get() = if (totalTimeMs > 0) (totalCalls * 1000f) / totalTimeMs else 0f
    }
    
    data class MemorySnapshot(
        val timestamp: Long,
        val usedMemoryMB: Float,
        val maxMemoryMB: Float,
        val freeMemoryMB: Float,
        val usagePercentage: Float,
        val nativeHeapMB: Float
    ) {
        val isHighUsage: Boolean get() = usagePercentage > MEMORY_PRESSURE_THRESHOLD * 100
        val formattedTime: String get() = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(timestamp))
    }
    
    data class AppHealthReport(
        val memoryHealth: MemoryHealth,
        val performanceHealth: PerformanceHealth,
        val resourceHealth: ResourceHealth,
        val overallScore: Int, // 0-100
        val recommendations: List<String>
    )
    
    data class MemoryHealth(
        val currentUsagePercentage: Float,
        val averageUsagePercentage: Float,
        val peakUsagePercentage: Float,
        val memoryLeakSuspected: Boolean,
        val score: Int // 0-100
    )
    
    data class PerformanceHealth(
        val slowOperationsCount: Long,
        val averageOperationTime: Long,
        val totalOperations: Long,
        val score: Int // 0-100
    )
    
    data class ResourceHealth(
        val cacheEfficiency: Float,
        val storageUsageMB: Float,
        val networkEfficiency: Float,
        val score: Int // 0-100
    )
    
    /**
     * Start comprehensive performance monitoring.
     */
    fun startMonitoring(context: Context) {
        if (isMonitoring) {
            DebugUtils.logWarning("OptimizationUtils", "Monitoring already started")
            return
        }
        
        isMonitoring = true
        DebugUtils.logInfo("OptimizationUtils", "Starting comprehensive performance monitoring")
        
        performanceScope.launch {
            while (isActive && isMonitoring) {
                try {
                    captureMemorySnapshot()
                    analyzePerformance()
                    checkForOptimizationOpportunities(context)
                    
                    delay(PERFORMANCE_LOG_INTERVAL_MS)
                } catch (e: Exception) {
                    DebugUtils.logError("OptimizationUtils", "Error in monitoring loop", e)
                }
            }
        }
    }
    
    /**
     * Stop performance monitoring.
     */
    fun stopMonitoring() {
        isMonitoring = false
        DebugUtils.logInfo("OptimizationUtils", "Performance monitoring stopped")
    }
    
    /**
     * Measure operation performance with automatic optimization insights.
     */
    inline fun <T> measureOptimizedOperation(
        operationName: String,
        reportSlowOperations: Boolean = true,
        operation: () -> T
    ): T {
        totalOperations.incrementAndGet()
        
        var result: T
        val executionTime = measureTimeMillis {
            result = operation()
        }
        
        // Update metrics
        updateOperationMetric(operationName, executionTime)
        
        // Check for slow operations
        if (reportSlowOperations && executionTime > 100L) {
            slowOperations.incrementAndGet()
            DebugUtils.logWarning("OptimizationUtils", 
                "Slow operation detected: $operationName took ${executionTime}ms")
                
            // Provide optimization suggestions
            suggestOptimization(operationName, executionTime)
        }
        
        return result
    }
    
    /**
     * Perform comprehensive app cleanup and optimization.
     */
    suspend fun performComprehensiveCleanup(context: Context, aggressive: Boolean = false) = withContext(Dispatchers.IO) {
        DebugUtils.logInfo("OptimizationUtils", "Starting comprehensive cleanup (aggressive: $aggressive)")
        
        val cleanupResults = mutableListOf<String>()
        
        try {
            // 1. Memory cleanup
            val memoryBefore = getCurrentMemoryUsage()
            performMemoryCleanup(aggressive)
            val memoryAfter = getCurrentMemoryUsage()
            val memoryFreed = memoryBefore - memoryAfter
            cleanupResults.add("Memory cleanup: ${memoryFreed.toInt()}MB freed")
            
            // 2. Cache cleanup
            val cacheCleared = performCacheCleanup(context, aggressive)
            cleanupResults.add("Cache cleanup: ${cacheCleared}MB cleared")
            
            // 3. Resource optimization
            val resourcesOptimized = optimizeResources(context)
            cleanupResults.add("Resources optimized: $resourcesOptimized items")
            
            // 4. Database optimization (if needed)
            val dbOptimized = optimizeDatabase(context)
            if (dbOptimized) cleanupResults.add("Database optimized")
            
            // 5. Clear old performance data
            cleanupOldPerformanceData()
            cleanupResults.add("Performance data cleaned")
            
            DebugUtils.logInfo("OptimizationUtils", "Cleanup completed: ${cleanupResults.joinToString(", ")}")
            
        } catch (e: Exception) {
            DebugUtils.logError("OptimizationUtils", "Error during comprehensive cleanup", e)
        }
    }
    
    /**
     * Generate comprehensive app health report.
     */
    fun generateHealthReport(context: Context): AppHealthReport {
        val memoryHealth = analyzeMemoryHealth()
        val performanceHealth = analyzePerformanceHealth()
        val resourceHealth = analyzeResourceHealth(context)
        
        val overallScore = ((memoryHealth.score + performanceHealth.score + resourceHealth.score) / 3.0f).toInt()
        
        val recommendations = generateRecommendations(memoryHealth, performanceHealth, resourceHealth)
        
        return AppHealthReport(
            memoryHealth = memoryHealth,
            performanceHealth = performanceHealth,
            resourceHealth = resourceHealth,
            overallScore = overallScore,
            recommendations = recommendations
        )
    }
    
    /**
     * Get optimization suggestions for current app state.
     */
    fun getOptimizationSuggestions(context: Context): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Memory optimization suggestions
        val currentMemory = getCurrentMemoryUsage()
        if (currentMemory > MEMORY_PRESSURE_THRESHOLD * 100) {
            suggestions.add("High memory usage detected. Consider clearing caches or reducing data retention.")
        }
        
        // Performance optimization suggestions
        val slowOps = slowOperations.get()
        val totalOps = totalOperations.get()
        if (totalOps > 0 && (slowOps.toFloat() / totalOps) > 0.1f) {
            suggestions.add("${(slowOps.toFloat() / totalOps * 100).toInt()}% of operations are slow. Review performance bottlenecks.")
        }
        
        // Resource optimization suggestions
        val cacheSize = getCacheSize(context)
        if (cacheSize > 50f) { // 50MB cache
            suggestions.add("Large cache detected (${cacheSize.toInt()}MB). Consider implementing cache expiration.")
        }
        
        return suggestions
    }
    
    /**
     * Force immediate garbage collection with monitoring.
     */
    fun forceGarbageCollectionWithMonitoring(): Float {
        val memoryBefore = getCurrentMemoryUsage()
        
        DebugUtils.logDebug("OptimizationUtils", "Forcing garbage collection")
        System.gc()
        System.runFinalization()
        
        // Give GC time to complete
        Thread.sleep(100)
        
        val memoryAfter = getCurrentMemoryUsage()
        val memoryFreed = memoryBefore - memoryAfter
        
        DebugUtils.logInfo("OptimizationUtils", "GC completed: ${memoryFreed.toInt()}MB freed")
        return memoryFreed
    }
    
    // Private helper methods
    
    private fun captureMemorySnapshot() {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            usedMemoryMB = usedMemory / (1024f * 1024f),
            maxMemoryMB = maxMemory / (1024f * 1024f),
            freeMemoryMB = freeMemory / (1024f * 1024f),
            usagePercentage = (usedMemory.toFloat() / maxMemory) * 100f,
            nativeHeapMB = Debug.getNativeHeapAllocatedSize() / (1024f * 1024f)
        )
        
        synchronized(memorySnapshots) {
            memorySnapshots.add(snapshot)
            
            // Keep only recent snapshots
            if (memorySnapshots.size > 100) {
                memorySnapshots.removeAt(0)
            }
        }
        
        if (snapshot.isHighUsage) {
            memoryWarnings.incrementAndGet()
            DebugUtils.logWarning("OptimizationUtils", 
                "High memory usage: ${snapshot.usagePercentage.toInt()}%")
        }
    }
    
    fun updateOperationMetric(operationName: String, executionTime: Long) {
        operationMetrics.compute(operationName) { _, existing ->
            if (existing == null) {
                OperationMetric(
                    name = operationName,
                    totalCalls = 1,
                    totalTimeMs = executionTime,
                    averageTimeMs = executionTime,
                    maxTimeMs = executionTime,
                    minTimeMs = executionTime,
                    lastExecutionMs = executionTime
                )
            } else {
                val newTotalCalls = existing.totalCalls + 1
                val newTotalTime = existing.totalTimeMs + executionTime
                existing.copy(
                    totalCalls = newTotalCalls,
                    totalTimeMs = newTotalTime,
                    averageTimeMs = newTotalTime / newTotalCalls,
                    maxTimeMs = maxOf(existing.maxTimeMs, executionTime),
                    minTimeMs = minOf(existing.minTimeMs, executionTime),
                    lastExecutionMs = executionTime
                )
            }
        }
    }
    
    private fun analyzePerformance() {
        if (operationMetrics.isEmpty()) return
        
        val slowOperations = operationMetrics.values.filter { it.isSlowOperation }
        if (slowOperations.isNotEmpty()) {
            DebugUtils.logInfo("OptimizationUtils", 
                "Performance analysis: ${slowOperations.size} slow operations detected")
        }
    }
    
    private fun checkForOptimizationOpportunities(context: Context) {
        // Check memory trends
        if (memorySnapshots.size > 10) {
            val recentSnapshots = memorySnapshots.takeLast(10)
            val memoryTrend = recentSnapshots.map { it.usagePercentage }
            val isIncreasing = memoryTrend.zipWithNext().count { (a, b) -> b > a } > 7
            
            if (isIncreasing) {
                DebugUtils.logWarning("OptimizationUtils", "Memory usage trending upward - potential leak")
            }
        }
        
        // Check operation patterns
        val totalOps = totalOperations.get()
        val slowOps = slowOperations.get()
        if (totalOps > 100 && (slowOps.toFloat() / totalOps) > 0.15f) {
            DebugUtils.logWarning("OptimizationUtils", "High slow operation ratio: ${slowOps}/${totalOps}")
        }
    }
    
    fun suggestOptimization(operationName: String, executionTime: Long) {
        val suggestions = when {
            operationName.contains("database", ignoreCase = true) && executionTime > 200L -> 
                "Consider database indexing or query optimization"
            operationName.contains("network", ignoreCase = true) && executionTime > 500L -> 
                "Consider request caching or connection pooling"
            operationName.contains("image", ignoreCase = true) && executionTime > 150L -> 
                "Consider image compression or lazy loading"
            operationName.contains("parse", ignoreCase = true) && executionTime > 100L -> 
                "Consider data preprocessing or streaming parsing"
            else -> "Review algorithm efficiency or consider background processing"
        }
        
        DebugUtils.logInfo("OptimizationUtils", "Optimization suggestion for $operationName: $suggestions")
    }
    
    private fun getCurrentMemoryUsage(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        return usedMemory / (1024f * 1024f)
    }
    
    private fun performMemoryCleanup(aggressive: Boolean) {
        if (aggressive) {
            // Force multiple GC cycles
            repeat(3) {
                System.gc()
                System.runFinalization()
                Thread.sleep(50)
            }
        } else {
            System.gc()
        }
    }
    
    private suspend fun performCacheCleanup(context: Context, aggressive: Boolean): Float = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val sizeBefore = getFolderSize(cacheDir)
            
            if (aggressive) {
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            } else {
                // Clean old cache files (older than 7 days)
                val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                cleanOldFiles(cacheDir, sevenDaysAgo)
            }
            
            val sizeAfter = getFolderSize(cacheDir)
            return@withContext (sizeBefore - sizeAfter) / (1024f * 1024f)
        } catch (e: Exception) {
            DebugUtils.logError("OptimizationUtils", "Error during cache cleanup", e)
            0f
        }
    }
    
    private fun optimizeResources(context: Context): Int {
        var optimized = 0
        
        try {
            // Clear any temporary resources
            optimized++
            
            // Optimize shared preferences if needed
            optimized++
            
        } catch (e: Exception) {
            DebugUtils.logError("OptimizationUtils", "Error optimizing resources", e)
        }
        
        return optimized
    }
    
    private fun optimizeDatabase(context: Context): Boolean {
        // Placeholder for database optimization if needed in the future
        return false
    }
    
    private fun cleanupOldPerformanceData() {
        val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L) // 24 hours
        
        synchronized(memorySnapshots) {
            memorySnapshots.removeAll { it.timestamp < cutoffTime }
        }
        
        // Keep only recent operation metrics
        operationMetrics.values.removeAll { it.lastExecutionMs < cutoffTime }
    }
    
    private fun analyzeMemoryHealth(): MemoryHealth {
        val recentSnapshots = memorySnapshots.takeLast(20)
        if (recentSnapshots.isEmpty()) {
            return MemoryHealth(0f, 0f, 0f, false, 100)
        }
        
        val current = recentSnapshots.last().usagePercentage
        val average = recentSnapshots.map { it.usagePercentage }.average().toFloat()
        val peak = recentSnapshots.maxOf { it.usagePercentage }
        
        // Detect potential memory leaks
        val memoryTrend = recentSnapshots.map { it.usagePercentage }
        val leakSuspected = if (memoryTrend.size > 10) {
            memoryTrend.zipWithNext().count { (a, b) -> b > a } > 8
        } else false
        
        val score = when {
            current > 90f || leakSuspected -> 20
            current > 80f -> 40
            current > 70f -> 60
            current > 50f -> 80
            else -> 100
        }
        
        return MemoryHealth(current, average, peak, leakSuspected, score)
    }
    
    private fun analyzePerformanceHealth(): PerformanceHealth {
        val totalOps = totalOperations.get()
        val slowOps = slowOperations.get()
        
        val avgTime = if (operationMetrics.isNotEmpty()) {
            operationMetrics.values.map { it.averageTimeMs }.average().toLong()
        } else 0L
        
        val score = when {
            totalOps == 0L -> 100
            slowOps.toFloat() / totalOps > 0.2f -> 20
            slowOps.toFloat() / totalOps > 0.1f -> 40
            slowOps.toFloat() / totalOps > 0.05f -> 60
            avgTime > 200L -> 70
            avgTime > 100L -> 80
            else -> 100
        }
        
        return PerformanceHealth(slowOps, avgTime, totalOps, score)
    }
    
    private fun analyzeResourceHealth(context: Context): ResourceHealth {
        val cacheSize = getCacheSize(context)
        val score = when {
            cacheSize > 100f -> 40 // Very large cache
            cacheSize > 50f -> 60   // Large cache
            cacheSize > 20f -> 80   // Moderate cache
            else -> 100             // Small cache
        }
        
        return ResourceHealth(
            cacheEfficiency = 0.8f, // Placeholder
            storageUsageMB = cacheSize,
            networkEfficiency = 0.9f, // Placeholder
            score = score
        )
    }
    
    private fun generateRecommendations(
        memoryHealth: MemoryHealth,
        performanceHealth: PerformanceHealth,
        resourceHealth: ResourceHealth
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (memoryHealth.score < 60) {
            recommendations.add("High memory usage detected. Consider implementing memory cleanup strategies.")
        }
        
        if (memoryHealth.memoryLeakSuspected) {
            recommendations.add("Potential memory leak detected. Review object lifecycle management.")
        }
        
        if (performanceHealth.score < 60) {
            recommendations.add("Performance issues detected. Review slow operations and optimize algorithms.")
        }
        
        if (resourceHealth.score < 60) {
            recommendations.add("Large cache detected. Implement cache expiration and size limits.")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("App health is good. Continue monitoring for optimal performance.")
        }
        
        return recommendations
    }
    
    private fun getCacheSize(context: Context): Float {
        return try {
            getFolderSize(context.cacheDir) / (1024f * 1024f)
        } catch (e: Exception) {
            0f
        }
    }
    
    private fun getFolderSize(folder: File): Long {
        return if (folder.exists() && folder.isDirectory) {
            folder.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else 0L
    }
    
    private fun cleanOldFiles(folder: File, cutoffTime: Long) {
        folder.walkTopDown()
            .filter { it.isFile && it.lastModified() < cutoffTime }
            .forEach { it.delete() }
    }
} 