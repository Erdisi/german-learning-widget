package com.germanleraningwidget.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.germanleraningwidget.data.model.*
import com.germanleraningwidget.data.repository.AppSettingsRepository
import com.germanleraningwidget.data.repository.WidgetCustomizationRepository
import kotlinx.coroutines.launch

/**
 * Widget Customization Screen - Main entry point for widget customization.
 * 
 * Features:
 * - List of all available widgets
 * - Current customization preview for each widget
 * - Navigation to detailed customization for each widget
 * - Live preview of customizations
 * - Reset to defaults functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetCustomizationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWidgetDetails: (WidgetType) -> Unit
) {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // Repositories
    val appSettingsRepository = remember { AppSettingsRepository.getInstance(context) }
    val widgetCustomizationRepository = remember { WidgetCustomizationRepository.getInstance(context) }
    
    // State
    val appSettings by appSettingsRepository.appSettings.collectAsStateWithLifecycle(
        initialValue = AppSettings.createDefault()
    )
    val allCustomizations by widgetCustomizationRepository.allWidgetCustomizations.collectAsStateWithLifecycle(
        initialValue = AllWidgetCustomizations.createDefault()
    )
    var showResetDialog by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var isApplying by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Function to apply all widget updates
    fun applyAllChanges() {
        scope.launch {
            isApplying = true
            try {
                if (appSettings.hapticFeedbackEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                
                val result = widgetCustomizationRepository.updateAllWidgetCustomizations(allCustomizations)
                if (result.isSuccess) {
                    successMessage = "✅ All widgets updated successfully!"
                } else {
                    errorMessage = "❌ Failed to update widgets: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                errorMessage = "❌ Error updating widgets: ${e.message}"
            } finally {
                isApplying = false
            }
        }
    }
    
    // Auto-hide messages after 3 seconds
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
    }
    
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            kotlinx.coroutines.delay(5000)
            errorMessage = null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Widget Customization",
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (appSettings.hapticFeedbackEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Apply All Changes Button
                    Button(
                        onClick = { applyAllChanges() },
                        enabled = !isApplying && !isResetting,
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isApplying) "Applying..." else "Apply All",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    
                    // Reset All Button
                    IconButton(
                        onClick = {
                            if (appSettings.hapticFeedbackEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            showResetDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Palette,
                            contentDescription = "Reset All to Defaults"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Widgets,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Customize Your Widgets",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Personalize background colors, text sizes, and contrast for each widget type",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Success/Error Messages
            successMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            
            errorMessage?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Widget List Section
            item {
                Text(
                    text = "Available Widgets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(WidgetType.getAllTypes()) { widgetType ->
                WidgetCustomizationCard(
                    widgetType = widgetType,
                    customization = allCustomizations.getCustomization(widgetType),
                    hapticEnabled = appSettings.hapticFeedbackEnabled,
                    onClick = {
                        if (appSettings.hapticFeedbackEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onNavigateToWidgetDetails(widgetType)
                    }
                )
            }
            
            // Summary Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Settings Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = allCustomizations.allCustomizationsSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset All Widget Customizations?") },
            text = { 
                Text("This will reset all widget customizations to their default settings. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        isResetting = true
                        
                        if (appSettings.hapticFeedbackEnabled) {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        
                        // Reset all customizations
                        scope.launch {
                            try {
                                val result = widgetCustomizationRepository.resetToDefaults()
                                if (result.isSuccess) {
                                    successMessage = "✅ All widget customizations reset to defaults"
                                } else {
                                    errorMessage = "❌ Failed to reset customizations: ${result.exceptionOrNull()?.message}"
                                }
                            } catch (e: Exception) {
                                errorMessage = "❌ Error resetting customizations: ${e.message}"
                            } finally {
                                isResetting = false
                            }
                        }
                    },
                    enabled = !isResetting
                ) { 
                    if (isResetting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Reset All")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) { Text("Cancel") }
            }
        )
    }
}

/**
 * Widget Customization Card Component.
 */
@Composable
private fun WidgetCustomizationCard(
    widgetType: WidgetType,
    customization: WidgetCustomization,
    hapticEnabled: Boolean,
    onClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    // Animated background color based on customization
    val animatedBackgroundColor by animateColorAsState(
        targetValue = customization.backgroundColor.centerColor.copy(alpha = 0.1f),
        animationSpec = tween(durationMillis = 300),
        label = "background_color_animation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (hapticEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                onClick()
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(animatedBackgroundColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Widget Type Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = widgetType.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = widgetType.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Current Customization Preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Background Color Preview with Solid Color
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                // Use solid center color to match actual widget appearance
                                customization.backgroundColor.centerColor
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(6.dp)
                            )
                    )
                    
                    // Text Sizes Info
                    Column {
                        Text(
                            text = "German: ${customization.germanTextSize.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Translation: ${customization.translatedTextSize.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Background: ${customization.backgroundColor.displayName} • Contrast: ${customization.textContrast.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Navigation Arrow
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "Customize ${widgetType.displayName}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}