package com.germanleraningwidget.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.unit.dp
import com.germanleraningwidget.data.model.DeliveryFrequency
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.UserPreferences
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.ui.viewmodel.AvailableLanguages
import com.germanleraningwidget.ui.viewmodel.AvailableTopics
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearningSetupScreen(
    userPreferences: UserPreferences,
    preferencesRepository: UserPreferencesRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    var selectedLevel by remember { mutableStateOf(userPreferences.germanLevel) }
    var selectedLanguage by remember { mutableStateOf(userPreferences.nativeLanguage) }
    var selectedTopics by remember { mutableStateOf(userPreferences.selectedTopics) }
    var selectedFrequency by remember { mutableStateOf(userPreferences.deliveryFrequency) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    
    val isFormValid = selectedLanguage.isNotBlank() && selectedTopics.isNotEmpty()
    
    // Check if any changes have been made compared to original preferences
    val hasChanges by remember(selectedLevel, selectedLanguage, selectedTopics, selectedFrequency) {
        derivedStateOf {
            selectedLevel != userPreferences.germanLevel ||
            selectedLanguage != userPreferences.nativeLanguage ||
            selectedTopics.toSet() != userPreferences.selectedTopics.toSet() ||
            selectedFrequency != userPreferences.deliveryFrequency
        }
    }
    
    // Reset success state after showing animation
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            kotlinx.coroutines.delay(2000)
            showSuccess = false
        }
    }
    
    // Clear error when user makes valid changes
    LaunchedEffect(hasChanges, isFormValid) {
        if (hasChanges && isFormValid) {
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom = 140.dp // Optimized space for floating button + bottom nav
            )
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Learning Preferences",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (hasChanges) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                modifier = Modifier.size(8.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.tertiary
                            ) {}
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (hasChanges) {
                            "You have unsaved changes. Tap Save to apply them."
                        } else {
                            "Adjust settings to get the most relevant German sentences for your level and interests."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hasChanges) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            // Error Message
            errorMessage?.let { error ->
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            
            // German Level Section
            item {
                SettingsSection(
                    title = "German Level",
                    subtitle = "Choose your current proficiency"
                ) {
                    GermanLevelSelector(
                        selectedLevel = selectedLevel,
                        onLevelSelected = { selectedLevel = it }
                    )
                }
            }
            
            // Native Language Section
            item {
                SettingsSection(
                    title = "Native Language",
                    subtitle = "Language for translations"
                ) {
                    LanguageSelector(
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { selectedLanguage = it }
                    )
                }
            }
            
            // Topics Section
            item {
                SettingsSection(
                    title = "Topics of Interest",
                    subtitle = "${selectedTopics.size} ${if (selectedTopics.size == 1) "topic" else "topics"} selected"
                ) {
                    TopicsSelector(
                        selectedTopics = selectedTopics,
                        onTopicToggled = { topic ->
                            selectedTopics = if (selectedTopics.contains(topic)) {
                                selectedTopics.toMutableSet().apply { remove(topic) }
                            } else {
                                selectedTopics.toMutableSet().apply { add(topic) }
                            }
                            errorMessage = null // Clear error when user makes changes
                        }
                    )
                }
            }
            
            // Delivery Frequency Section
            item {
                SettingsSection(
                    title = "Delivery Frequency",
                    subtitle = "How often to receive new sentences"
                ) {
                    FrequencySelector(
                        selectedFrequency = selectedFrequency,
                        onFrequencySelected = { selectedFrequency = it }
                    )
                }
            }
        }
        
        // Success Animation - positioned in center
        AnimatedVisibility(
            visible = showSuccess,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Preferences saved!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Floating Save Button - only shows when there are changes to save
        AnimatedVisibility(
            visible = hasChanges && isFormValid && !showSuccess && !isLoading,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight }
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight }
            ) + fadeOut(
                animationSpec = tween(200)
            ),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp) // Optimized positioning
        ) {
            FloatingActionButton(
                onClick = {
                    // Prevent multiple clicks during loading
                    if (isLoading) return@FloatingActionButton
                    
                    // Provide haptic feedback
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    
                    // Debug current state
                    android.util.Log.d("LearningSetup", "Current state - Level: $selectedLevel, Language: '$selectedLanguage', Topics: $selectedTopics, Frequency: $selectedFrequency")
                    android.util.Log.d("LearningSetup", "Original preferences - Level: ${userPreferences.germanLevel}, Language: '${userPreferences.nativeLanguage}', Topics: ${userPreferences.selectedTopics}, Frequency: ${userPreferences.deliveryFrequency}")
                    android.util.Log.d("LearningSetup", "Has changes: $hasChanges, Is valid: $isFormValid")
                    
                    isLoading = true
                    errorMessage = null
                    
                    val updatedPreferences = UserPreferences(
                        germanLevel = selectedLevel,
                        nativeLanguage = selectedLanguage.trim(),
                        selectedTopics = selectedTopics.filter { it.isNotBlank() }.toSet(),
                        deliveryFrequency = selectedFrequency,
                        isOnboardingCompleted = true
                    )
                    
                    scope.launch {
                        try {
                            // Log the preferences being saved for debugging
                            android.util.Log.d("LearningSetup", "Saving preferences: $updatedPreferences")
                            
                            // Note: WorkManager is now properly initialized in Application class
                            
                            // Validate preferences before saving
                            if (!updatedPreferences.isValid()) {
                                errorMessage = "Invalid preferences. Please check your selections."
                                isLoading = false
                                return@launch
                            }
                            
                            preferencesRepository.updateUserPreferences(updatedPreferences)
                            
                            // Reschedule work with new frequency (with error handling)
                            try {
                                SentenceDeliveryWorker.scheduleWork(context, selectedFrequency)
                                android.util.Log.d("LearningSetup", "Work scheduling completed successfully")
                            } catch (workError: Exception) {
                                android.util.Log.w("LearningSetup", "Work scheduling failed, but preferences were saved", workError)
                                // Don't fail the entire save operation if work scheduling fails
                            }
                            
                            android.util.Log.d("LearningSetup", "Preferences saved successfully")
                            isLoading = false
                            showSuccess = true
                        } catch (e: Exception) {
                            android.util.Log.e("LearningSetup", "Error saving preferences", e)
                            errorMessage = "Failed to save preferences: ${e.message ?: "Unknown error"}"
                            isLoading = false
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                    hoveredElevation = 10.dp
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Save preferences",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Save",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        content()
    }
}

@Composable
private fun GermanLevelSelector(
    selectedLevel: GermanLevel,
    onLevelSelected: (GermanLevel) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GermanLevel.values().forEach { level ->
            LevelCard(
                level = level,
                isSelected = selectedLevel == level,
                onClick = { onLevelSelected(level) }
            )
        }
    }
}

@Composable
private fun LevelCard(
    level: GermanLevel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = getLevelDescription(level),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AvailableLanguages.languages) { language ->
            LanguageChip(
                language = language,
                isSelected = selectedLanguage == language,
                onClick = { onLanguageSelected(language) }
            )
        }
    }
}

@Composable
private fun LanguageChip(
    language: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(language) },
        selected = isSelected,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun TopicsSelector(
    selectedTopics: Set<String>,
    onTopicToggled: (String) -> Unit
) {
    val chunkedTopics = AvailableTopics.topics.chunked(3)
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chunkedTopics.forEach { rowTopics ->
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rowTopics) { topic ->
                    TopicChip(
                        topic = topic,
                        isSelected = selectedTopics.contains(topic),
                        onClick = { onTopicToggled(topic) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicChip(
    topic: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(topic) },
        selected = isSelected,
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        leadingIcon = if (isSelected) {
            {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}

@Composable
private fun FrequencySelector(
    selectedFrequency: DeliveryFrequency,
    onFrequencySelected: (DeliveryFrequency) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DeliveryFrequency.values().forEach { frequency ->
            FrequencyCard(
                frequency = frequency,
                isSelected = selectedFrequency == frequency,
                onClick = { onFrequencySelected(frequency) }
            )
        }
    }
}

@Composable
private fun FrequencyCard(
    frequency: DeliveryFrequency,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = frequency.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = getFrequencyDescription(frequency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getLevelDescription(level: GermanLevel): String {
    return when (level) {
        GermanLevel.A1 -> "Basic phrases and vocabulary"
        GermanLevel.A2 -> "Simple conversations and expressions"
        GermanLevel.B1 -> "Everyday situations and opinions"
        GermanLevel.B2 -> "Complex topics and discussions"
        GermanLevel.C1 -> "Academic and professional contexts"
        GermanLevel.C2 -> "Native-like proficiency"
    }
}

private fun getFrequencyDescription(frequency: DeliveryFrequency): String {
    return when (frequency) {
        DeliveryFrequency.EVERY_30_MINUTES -> "Most frequent learning sessions"
        DeliveryFrequency.EVERY_HOUR -> "Perfect for intensive learning"
        DeliveryFrequency.EVERY_2_HOURS -> "Regular learning rhythm"
        DeliveryFrequency.EVERY_4_HOURS -> "Good for busy schedules"
        DeliveryFrequency.EVERY_6_HOURS -> "Balanced learning pace"
        DeliveryFrequency.EVERY_12_HOURS -> "Twice daily practice"
        DeliveryFrequency.DAILY -> "Recommended for steady progress"
    }
} 