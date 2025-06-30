package com.germanleraningwidget.ui.screen

import android.content.Intent
import android.net.Uri
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
import com.germanleraningwidget.ui.theme.*
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
    var showAboutDialog by remember { mutableStateOf(false) }

    // Helper function to update settings with haptic feedback
    fun updateSettingWithFeedback(updateAction: suspend () -> Unit, successMsg: String) {
        scope.launch {
            try {
                isLoading = true
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
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
                .padding(horizontal = UnifiedDesign.ContentPadding),
            verticalArrangement = Arrangement.spacedBy(UnifiedDesign.ContentGap),
            contentPadding = PaddingValues(vertical = UnifiedDesign.ContentPadding)
        ) {
            // Learning Preferences Section
            item {
                SettingsSection(
                    title = "Learning",
                    icon = Icons.Filled.School
                ) {
                    SettingsItem(
                        title = "Learning Preferences",
                        subtitle = "Levels: ${userPreferences.selectedGermanLevels.joinToString(", ")} (Primary: ${userPreferences.primaryGermanLevel}) • ${userPreferences.selectedTopics.size} topics",
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

            // Customization Section
            item {
                SettingsSection(
                    title = "Customization",
                    icon = Icons.Filled.Palette
                ) {
                    SettingsItem(
                        title = "Widget Customization",
                        subtitle = "Personalize backgrounds, text sizes, and contrast",
                        icon = Icons.Filled.Widgets,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToWidgetCustomization()
                        },
                        showArrow = true
                    )
                    
                    SettingsToggleItem(
                        title = "Theme",
                        subtitle = if (appSettings.isDarkModeEnabled == true) {
                            "Dark mode enabled"
                        } else if (appSettings.isDarkModeEnabled == false) {
                            "Light mode enabled"
                        } else {
                            "Following system theme"
                        },
                        icon = if (appSettings.isDarkModeEnabled == true) {
                            Icons.Filled.Brightness4
                        } else {
                            Icons.Filled.Brightness7
                        },
                        checked = appSettings.isDarkModeEnabled ?: false,
                        onCheckedChange = { enabled ->
                            updateSettingWithFeedback(
                                { appSettingsRepository.updateDarkModeEnabled(enabled) },
                                if (enabled) "Dark mode enabled" else "Light mode enabled"
                            )
                        }
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
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:erdisdriza@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "German Learning Widget - Support Request")
                                    putExtra(Intent.EXTRA_TEXT, "Hi!\n\nI need help with the German Learning Widget app.\n\n")
                                }
                                context.startActivity(intent)
                                successMessage = "Opening email composer..."
                            } catch (e: Exception) {
                                successMessage = "No email app found. Please email erdisdriza@gmail.com"
                            }
                        },
                        showArrow = true
                    )
                    
                    SettingsItem(
                        title = "Buy me a coffee",
                        subtitle = "Support the development",
                        icon = Icons.Filled.LocalCafe,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://buymeacoffee.com/driza")
                                }
                                context.startActivity(intent)
                                successMessage = "Opening Buy me a coffee..."
                            } catch (e: Exception) {
                                successMessage = "Could not open browser: ${e.message}"
                            }
                        },
                        showArrow = true
                    )
                }
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
                modifier = Modifier.padding(UnifiedDesign.ContentPadding),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(UnifiedDesign.ContentPadding),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
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
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Version: ${versionInfo.versionName}", style = MaterialTheme.typography.bodySmall)
                    Text("Build: ${versionInfo.versionCode}", style = MaterialTheme.typography.bodySmall)
                    Text("Package: ${versionInfo.packageName}", style = MaterialTheme.typography.bodySmall)
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
private fun SettingsSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            content()
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
    UnifiedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.padding(top = 4.dp)
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
    UnifiedCard(
        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.padding(top = 4.dp)
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