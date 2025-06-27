package com.germanleraningwidget.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.germanleraningwidget.data.model.DeliveryFrequency
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.UserPreferences
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentPreferences()
    }
    
    private fun loadCurrentPreferences() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                preferencesRepository.userPreferences.collect { preferences ->
                    _uiState.value = _uiState.value.copy(
                        selectedLevel = preferences.germanLevel,
                        selectedLanguage = preferences.nativeLanguage,
                        selectedTopics = preferences.selectedTopics.toMutableSet(),
                        selectedFrequency = preferences.deliveryFrequency,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load preferences: ${e.message}"
                )
            }
        }
    }
    
    fun updateGermanLevel(level: GermanLevel) {
        _uiState.value = _uiState.value.copy(selectedLevel = level)
    }
    
    fun updateNativeLanguage(language: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }
    
    fun toggleTopic(topic: String) {
        val currentTopics = _uiState.value.selectedTopics.toMutableSet()
        if (currentTopics.contains(topic)) {
            currentTopics.remove(topic)
        } else {
            currentTopics.add(topic)
        }
        _uiState.value = _uiState.value.copy(selectedTopics = currentTopics)
    }
    
    fun updateDeliveryFrequency(frequency: DeliveryFrequency) {
        _uiState.value = _uiState.value.copy(selectedFrequency = frequency)
    }
    
    suspend fun savePreferences(): DeliveryFrequency {
        val currentState = _uiState.value
        
        try {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            val preferences = UserPreferences(
                germanLevel = currentState.selectedLevel,
                nativeLanguage = currentState.selectedLanguage,
                selectedTopics = currentState.selectedTopics,
                deliveryFrequency = currentState.selectedFrequency,
                isOnboardingCompleted = true
            )
            
            preferencesRepository.updateUserPreferences(preferences)
            
            // Update UI state to mark onboarding as completed
            _uiState.value = currentState.copy(
                isOnboardingCompleted = true,
                isLoading = false,
                error = null
            )
            
            return currentState.selectedFrequency
        } catch (e: Exception) {
            _uiState.value = currentState.copy(
                isLoading = false,
                error = "Failed to save preferences: ${e.message}"
            )
            throw e
        }
    }
    
    fun isFormValid(): Boolean {
        val state = _uiState.value
        return state.selectedLanguage.isNotBlank() && 
               state.selectedTopics.isNotEmpty() &&
               !state.isLoading
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class OnboardingUiState(
    val selectedLevel: GermanLevel = GermanLevel.A1,
    val selectedLanguage: String = "English",
    val selectedTopics: MutableSet<String> = mutableSetOf(),
    val selectedFrequency: DeliveryFrequency = DeliveryFrequency.DAILY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOnboardingCompleted: Boolean = false
)

object AvailableTopics {
    val topics = listOf(
        "Travel",
        "Food",
        "Work",
        "Daily Life",
        "Technology",
        "Health",
        "Education",
        "Entertainment",
        "Sports",
        "Weather"
    )
}

object AvailableLanguages {
    val languages = listOf(
        "English",
        "Albanian",
        "Arabic",
        "Italian",
        "Russian",
        "Serbian",
        "Turkish",
        "Ukrainian"
    )
} 