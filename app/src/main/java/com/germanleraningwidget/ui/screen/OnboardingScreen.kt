package com.germanleraningwidget.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.germanleraningwidget.ui.components.GermanLevelStep
import com.germanleraningwidget.ui.components.TopicsStep
import com.germanleraningwidget.ui.components.FrequencyStep
import com.germanleraningwidget.ui.components.WelcomeStep
import com.germanleraningwidget.ui.viewmodel.OnboardingViewModel
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import com.germanleraningwidget.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 4 // Updated to 4 steps (removed native language step)
    
    // Handle onboarding completion
    LaunchedEffect(uiState.isOnboardingCompleted) {
        if (uiState.isOnboardingCompleted) {
            // Schedule work when onboarding is completed
            SentenceDeliveryWorker.scheduleWork(context, uiState.selectedFrequency)
            onOnboardingComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (currentStep + 1) / totalSteps.toFloat() },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        )
        
        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep()
                    1 -> GermanLevelStep(
                        selectedLevels = uiState.selectedGermanLevels,
                        primaryLevel = uiState.primaryGermanLevel,
                        onLevelToggled = { viewModel.toggleGermanLevel(it) },
                        onPrimaryLevelChanged = { viewModel.updatePrimaryGermanLevel(it) }
                    )
                    2 -> TopicsStep(
                        selectedTopics = uiState.selectedTopics,
                        onTopicToggled = { topic ->
                            val newTopics = if (uiState.selectedTopics.contains(topic)) {
                                uiState.selectedTopics - topic
                            } else {
                                uiState.selectedTopics + topic
                            }
                            viewModel.updateSelectedTopics(newTopics)
                        }
                    )
                    3 -> FrequencyStep(
                        selectedFrequency = uiState.selectedFrequency,
                        onFrequencySelected = { viewModel.updateDeliveryFrequency(it) }
                    )
                }
            }
        }
        
        // Navigation buttons
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(UnifiedDesign.ContentPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Next/Complete button
                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            // Complete onboarding
                            viewModel.completeOnboarding()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = when (currentStep) {
                        0 -> true
                        1 -> uiState.selectedGermanLevels.isNotEmpty()
                        2 -> uiState.selectedTopics.isNotEmpty()
                        3 -> true
                        else -> false
                    }
                ) {
                    Text(if (currentStep < totalSteps - 1) "Next" else "Complete")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
} 