package com.germanleraningwidget.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.germanleraningwidget.data.model.DeliveryFrequency
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.ui.components.FrequencyStep
import com.germanleraningwidget.ui.components.GermanLevelStep
import com.germanleraningwidget.ui.components.NativeLanguageStep
import com.germanleraningwidget.ui.components.TopicsStep
import com.germanleraningwidget.ui.viewmodel.AvailableLanguages
import com.germanleraningwidget.ui.viewmodel.AvailableTopics
import com.germanleraningwidget.ui.viewmodel.OnboardingUiState
import com.germanleraningwidget.ui.viewmodel.OnboardingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onOnboardingComplete: (com.germanleraningwidget.data.model.DeliveryFrequency) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentStep by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(uiState.isOnboardingCompleted) {
        if (uiState.isOnboardingCompleted) {
            onOnboardingComplete(uiState.selectedFrequency)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Your Learning") },
                navigationIcon = {
                    if (currentStep > 0) {
                        IconButton(onClick = { currentStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentStep = currentStep,
                onNext = { currentStep++ },
                onPrevious = { currentStep-- },
                onComplete = {
                    scope.launch {
                        try {
                            viewModel.savePreferences()
                        } catch (e: Exception) {
                            // Handle any errors during save
                            // You could show a snackbar or error message here
                        }
                    }
                },
                canProceed = when (currentStep) {
                    0 -> uiState.selectedLanguage.isNotBlank()
                    1 -> true // German level always has a default
                    2 -> uiState.selectedTopics.isNotEmpty()
                    3 -> true // Frequency always has a default
                    else -> false
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentStep + 1) / 4f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            
            // Step indicator
            Text(
                text = "Step ${currentStep + 1} of 4",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Step content
            when (currentStep) {
                0 -> NativeLanguageStep(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = { viewModel.updateNativeLanguage(it) }
                )
                1 -> GermanLevelStep(
                    selectedLevel = uiState.selectedLevel,
                    onLevelSelected = { viewModel.updateGermanLevel(it) }
                )
                2 -> TopicsStep(
                    selectedTopics = uiState.selectedTopics,
                    onTopicToggled = { viewModel.toggleTopic(it) }
                )
                3 -> FrequencyStep(
                    selectedFrequency = uiState.selectedFrequency,
                    onFrequencySelected = { viewModel.updateDeliveryFrequency(it) }
                )
            }
        }
    }
}

@Composable
fun OnboardingBottomBar(
    currentStep: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit,
    canProceed: Boolean
) {
    BottomAppBar(
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous button
            if (currentStep > 0) {
                OutlinedButton(onClick = onPrevious) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }
            
            // Next/Complete button
            if (currentStep < 3) {
                Button(
                    onClick = onNext,
                    enabled = canProceed
                ) {
                    Text("Next")
                }
            } else {
                Button(
                    onClick = onComplete,
                    enabled = canProceed
                ) {
                    Text("Get Started")
                }
            }
        }
    }
} 