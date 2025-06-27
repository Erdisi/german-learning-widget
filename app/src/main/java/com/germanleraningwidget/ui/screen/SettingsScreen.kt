package com.germanleraningwidget.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.germanleraningwidget.data.model.AppSettings
import com.germanleraningwidget.data.model.UserPreferences
import com.germanleraningwidget.data.repository.AppSettingsRepository
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    onNavigateBack: () -> Unit,
    onNavigateToLearningPreferences: () -> Unit,
    onNavigateToWidgetCustomization: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    // App settings repository
    val appSettingsRepository = remember { AppSettingsRepository.getInstance(context) }
    
    // App settings state
    val appSettings by appSettingsRepository.appSettings.collectAsStateWithLifecycle(
        initialValue = AppSettings.createDefault()
    )
    
    // App version info
    val appVersionInfo = remember { appSettingsRepository.getAppVersion() }
    val systemNotificationsEnabled = remember { appSettingsRepository.areNotificationsEnabledInSystem() }
    
    // UI state
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showTextSizeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Helper function to update settings with haptic feedback
    fun updateSettingWithFeedback(updateAction: suspend () -> Unit, successMsg: String) {
        scope.launch {
            try {
                isLoading = true
                if (appSettings.hapticFeedbackEnabled) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                updateAction()
                successMessage = successMsg
            } catch (e: Exception) {
                successMessage = "Failed to update setting: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Learning Preferences Section
            item {
                SettingsSection(
                    title = "Learning",
                    icon = Icons.Filled.School
                ) {
                    SettingsItem(
                        title = "Learning Preferences",
                        subtitle = "Level: ${userPreferences.germanLevel.displayName} • ${userPreferences.selectedTopics.size} topics",
                        icon = Icons.Filled.Tune,
                        onClick = onNavigateToLearningPreferences,
                        showArrow = true
                    )
                }
            }

            // Notifications Section
            item {
                SettingsSection(
                    title = "Notifications",
                    icon = Icons.Filled.Notifications
                ) {
                    SettingsToggleItem(
                        title = "Learning Reminders",
                        subtitle = if (systemNotificationsEnabled) {
                            "Get notified when new sentences are available"
                        } else {
                            "Enable notifications in system settings first"
                        },
                        icon = Icons.Filled.NotificationsActive,
                        checked = appSettings.learningRemindersEnabled && systemNotificationsEnabled,
                        enabled = systemNotificationsEnabled,
                        onCheckedChange = { enabled ->
                            updateSettingWithFeedback(
                                { appSettingsRepository.updateLearningRemindersEnabled(enabled) },
                                if (enabled) "Learning reminders enabled" else "Learning reminders disabled"
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Notification Settings",
                        subtitle = if (systemNotificationsEnabled) {
                            "Customize notification preferences"
                        } else {
                            "Notifications are disabled - tap to enable"
                        },
                        icon = if (systemNotificationsEnabled) {
                            Icons.Filled.NotificationImportant
                        } else {
                            Icons.Filled.NotificationsOff
                        },
                        onClick = {
                            try {
                                val intent = appSettingsRepository.openSystemNotificationSettings()
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                successMessage = "Could not open notification settings"
                            }
                        },
                        showArrow = true
                    )
                }
            }

            // Accessibility & Experience Section
            item {
                SettingsSection(
                    title = "Accessibility & Experience",
                    icon = Icons.Filled.Accessibility
                ) {
                    SettingsToggleItem(
                        title = "Haptic Feedback",
                        subtitle = "Vibrate on interactions",
                        icon = Icons.Filled.Vibration,
                        checked = appSettings.hapticFeedbackEnabled,
                        onCheckedChange = { enabled ->
                            updateSettingWithFeedback(
                                { appSettingsRepository.updateHapticFeedbackEnabled(enabled) },
                                if (enabled) "Haptic feedback enabled" else "Haptic feedback disabled"
                            )
                        }
                    )
                    
                    SettingsItem(
                        title = "Text Size",
                        subtitle = "Current: ${appSettings.textSizeDescription}",
                        icon = Icons.Filled.FormatSize,
                        onClick = { showTextSizeDialog = true },
                        showArrow = true
                    )
                }
            }

            // Widget Customization Section
            item {
                SettingsSection(
                    title = "Widget Customization",
                    icon = Icons.Filled.Widgets
                ) {
                    SettingsItem(
                        title = "Customize Widgets",
                        subtitle = "Personalize backgrounds, text sizes, and contrast",
                        icon = Icons.Filled.Palette,
                        onClick = {
                            if (appSettings.hapticFeedbackEnabled) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            onNavigateToWidgetCustomization()
                        },
                        showArrow = true
                    )
                }
            }

            // About Section
            item {
                SettingsSection(
                    title = "About",
                    icon = Icons.Filled.Info
                ) {
                    SettingsItem(
                        title = "German Learning Widget",
                        subtitle = "Version ${appVersionInfo.versionName}",
                        icon = Icons.Filled.Apps,
                        onClick = { showAboutDialog = true }
                    )
                    
                    SettingsItem(
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        icon = Icons.Filled.PrivacyTip,
                        onClick = {
                            // Privacy policy functionality will be implemented in future versions
                            successMessage = "Privacy policy coming soon"
                        },
                        showArrow = true
                    )
                    
                    SettingsItem(
                        title = "Support",
                        subtitle = "Get help and send feedback",
                        icon = Icons.Filled.Support,
                        onClick = {
                            // Support system will be implemented in future versions
                            successMessage = "Support options coming soon"
                        },
                        showArrow = true
                    )
                }
            }

            // Add some bottom padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Success message
    successMessage?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(3000)
            successMessage = null
        }
        
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Text Size Dialog
    if (showTextSizeDialog) {
        TextSizeDialog(
            currentScale = appSettings.textSizeScale,
            onScaleSelected = { scale ->
                updateSettingWithFeedback(
                    { appSettingsRepository.updateTextSizeScale(scale) },
                    "Text size updated to ${AppSettings(textSizeScale = scale).textSizeDescription}"
                )
                showTextSizeDialog = false
            },
            onDismiss = { showTextSizeDialog = false }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(
            versionInfo = appVersionInfo,
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
private fun TextSizeDialog(
    currentScale: Float,
    onScaleSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val scales = listOf(
        0.8f to "Small",
        1.0f to "Default", 
        1.2f to "Large",
        1.5f to "Extra Large"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Text Size") },
        text = {
            Column {
                Text(
                    "Choose your preferred text size for better readability.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                scales.forEach { (scale, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = scale == currentScale,
                            onClick = { onScaleSelected(scale) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AboutDialog(
    versionInfo: AppSettingsRepository.AppVersionInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("German Learning Widget")
            }
        },
        text = {
            Column {
                Text(
                    "Learn German through innovative widget-based delivery. Get contextual German sentences directly on your home screen for passive learning throughout daily device usage.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text("Version: ${versionInfo.versionName}", style = MaterialTheme.typography.bodySmall)
                Text("Build: ${versionInfo.versionCode}", style = MaterialTheme.typography.bodySmall)
                Text("Package: ${versionInfo.packageName}", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit = {},
    showArrow: Boolean = false,
    isDestructive: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            if (showArrow) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
                
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}