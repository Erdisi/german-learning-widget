package com.germanleraningwidget.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.germanleraningwidget.data.model.DeliveryFrequency
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.UserPreferences
import com.germanleraningwidget.data.repository.UserPreferencesRepository
import com.germanleraningwidget.worker.SentenceDeliveryWorker
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * ViewModel for managing onboarding flow state and user preferences.
 * 
 * Features:
 * - Thread-safe state management
 * - Comprehensive error handling
 * - Form validation
 * - Reactive state updates
 * - Proper resource cleanup
 * 
 * Thread Safety: All operations are thread-safe using Mutex
 * Error Handling: Graceful error handling with user-friendly messages
 * Performance: Efficient state updates with distinctUntilChanged
 */
class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "OnboardingViewModel"
    }
    
    // Mutex for protecting state updates
    private val stateMutex = Mutex()
    
    // Exception handler for coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Unhandled exception in OnboardingViewModel", exception)
        viewModelScope.launch {
            updateStateWithError("An unexpected error occurred: ${exception.message}")
        }
    }
    
    // UI State
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentPreferences()
    }
    
    /**
     * Load current user preferences and initialize UI state.
     */
    private fun loadCurrentPreferences() {
        viewModelScope.launch(exceptionHandler) {
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                preferencesRepository.userPreferences
                    .distinctUntilChanged()
                    .catch { e ->
                        Log.e(TAG, "Error loading preferences", e)
                        updateStateWithError("Failed to load preferences: ${e.message}")
                    }
                    .collect { preferences ->
                        stateMutex.withLock {
                            _uiState.value = _uiState.value.copy(
                                selectedLevel = preferences.germanLevel,
                                selectedLanguage = preferences.nativeLanguage,
                                selectedTopics = preferences.selectedTopics.toMutableSet(),
                                selectedFrequency = preferences.deliveryFrequency,
                                isOnboardingCompleted = preferences.isOnboardingCompleted,
                                isLoading = false,
                                error = null
                            )
                        }
                        Log.d(TAG, "Preferences loaded successfully")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load preferences", e)
                updateStateWithError("Failed to load preferences: ${e.message}")
            }
        }
    }
    
    /**
     * Update German proficiency level.
     */
    fun updateGermanLevel(level: GermanLevel) {
        viewModelScope.launch {
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(selectedLevel = level)
                Log.d(TAG, "German level updated to: $level")
            }
        }
    }
    
    /**
     * Update native language with validation.
     */
    fun updateNativeLanguage(language: String) {
        viewModelScope.launch {
            val trimmedLanguage = language.trim()
            if (trimmedLanguage.isBlank()) {
                updateStateWithError("Language cannot be empty")
                return@launch
            }
            
            if (!AvailableLanguages.languages.contains(trimmedLanguage)) {
                updateStateWithError("Please select a valid language")
                return@launch
            }
            
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(
                    selectedLanguage = trimmedLanguage,
                    error = null
                )
                Log.d(TAG, "Native language updated to: $trimmedLanguage")
            }
        }
    }
    
    /**
     * Toggle topic selection with validation.
     */
    fun toggleTopic(topic: String) {
        viewModelScope.launch {
            val trimmedTopic = topic.trim()
            if (trimmedTopic.isBlank()) {
                updateStateWithError("Invalid topic")
                return@launch
            }
            
            if (!AvailableTopics.topics.contains(trimmedTopic)) {
                updateStateWithError("Please select a valid topic")
                return@launch
            }
            
            stateMutex.withLock {
                val currentTopics = _uiState.value.selectedTopics.toMutableSet()
                val wasRemoved = if (currentTopics.contains(trimmedTopic)) {
                    currentTopics.remove(trimmedTopic)
                    true
                } else {
                    currentTopics.add(trimmedTopic)
                    false
                }
                
                _uiState.value = _uiState.value.copy(
                    selectedTopics = currentTopics,
                    error = null
                )
                
                val action = if (wasRemoved) "removed from" else "added to"
                Log.d(TAG, "Topic '$trimmedTopic' $action selection")
            }
        }
    }
    
    /**
     * Update delivery frequency.
     */
    fun updateDeliveryFrequency(frequency: DeliveryFrequency) {
        viewModelScope.launch {
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(selectedFrequency = frequency)
                Log.d(TAG, "Delivery frequency updated to: ${frequency.displayName}")
            }
        }
    }
    
    /**
     * Save user preferences with comprehensive validation.
     */
    suspend fun savePreferences(): Result<DeliveryFrequency> {
        return try {
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            val currentState = _uiState.value
            
            // Validate form before saving
            val validationResult = validateForm(currentState)
            if (!validationResult.isSuccess) {
                updateStateWithError(validationResult.errorMessage ?: "Form validation failed")
                return Result.failure(ValidationException(validationResult.errorMessage ?: "Invalid form"))
            }
            
            val preferences = UserPreferences(
                germanLevel = currentState.selectedLevel,
                nativeLanguage = currentState.selectedLanguage,
                selectedTopics = currentState.selectedTopics.toSet(),
                deliveryFrequency = currentState.selectedFrequency,
                isOnboardingCompleted = true
            )
            
            // Save preferences
            val saveResult = preferencesRepository.updateUserPreferences(preferences)
            saveResult.getOrThrow()
            
            // Update UI state
            stateMutex.withLock {
                _uiState.value = currentState.copy(
                    isOnboardingCompleted = true,
                    isLoading = false,
                    error = null
                )
            }
            
            Log.i(TAG, "Preferences saved successfully")
            Result.success(currentState.selectedFrequency)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save preferences", e)
            val errorMessage = when (e) {
                is ValidationException -> e.message ?: "Validation error"
                is UserPreferencesRepository.PreferencesException -> "Failed to save preferences"
                else -> "An unexpected error occurred"
            }
            updateStateWithError(errorMessage)
            Result.failure(e)
        }
    }
    
    /**
     * Validate the current form state.
     */
    private fun validateForm(state: OnboardingUiState): UserPreferences.ValidationResult {
        if (state.selectedLanguage.isBlank()) {
            return UserPreferences.ValidationResult.Error("Please select a native language")
        }
        
        if (!AvailableLanguages.languages.contains(state.selectedLanguage)) {
            return UserPreferences.ValidationResult.Error("Please select a valid language")
        }
        
        if (state.selectedTopics.isEmpty()) {
            return UserPreferences.ValidationResult.Error("Please select at least one topic")
        }
        
        val invalidTopics = state.selectedTopics.filter { !AvailableTopics.topics.contains(it) }
        if (invalidTopics.isNotEmpty()) {
            return UserPreferences.ValidationResult.Error("Some selected topics are invalid")
        }
        
        return UserPreferences.ValidationResult.Success
    }
    
    /**
     * Check if the form is valid for submission.
     */
    fun isFormValid(): Boolean {
        val state = _uiState.value
        return !state.isLoading && 
               state.selectedLanguage.isNotBlank() && 
               state.selectedTopics.isNotEmpty() &&
               validateForm(state).isSuccess
    }
    
    /**
     * Clear current error state.
     */
    fun clearError() {
        viewModelScope.launch {
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }
    
    /**
     * Reset form to default values.
     */
    fun resetForm() {
        viewModelScope.launch {
            stateMutex.withLock {
                _uiState.value = OnboardingUiState()
                Log.d(TAG, "Form reset to defaults")
            }
        }
    }
    
    /**
     * Get current preferences without subscribing to changes.
     */
    suspend fun getCurrentPreferences(): Result<UserPreferences> {
        return try {
            val preferences = preferencesRepository.userPreferences.first()
            Result.success(preferences)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current preferences", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update state with error message.
     */
    private suspend fun updateStateWithError(errorMessage: String) {
        stateMutex.withLock {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = errorMessage
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "OnboardingViewModel cleared")
    }
    
    /**
     * Custom exception for validation errors.
     */
    class ValidationException(message: String) : Exception(message)
}

/**
 * Immutable UI state for onboarding screen.
 */
data class OnboardingUiState(
    val selectedLevel: GermanLevel = GermanLevel.A1,
    val selectedLanguage: String = "English",
    val selectedTopics: MutableSet<String> = mutableSetOf(),
    val selectedFrequency: DeliveryFrequency = DeliveryFrequency.DAILY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOnboardingCompleted: Boolean = false
) {
    /**
     * Get immutable copy of selected topics.
     */
    val selectedTopicsImmutable: Set<String> get() = selectedTopics.toSet()
    
    /**
     * Check if any topics are selected.
     */
    val hasSelectedTopics: Boolean get() = selectedTopics.isNotEmpty()
    
    /**
     * Get validation status.
     */
    val isValid: Boolean get() = selectedLanguage.isNotBlank() && 
                                 selectedTopics.isNotEmpty() && 
                                 !isLoading
}

/**
 * Available topics for German learning.
 * Organized by difficulty and relevance.
 */
object AvailableTopics {
    val topics = listOf(
        "Greetings",
        "Introductions", 
        "Daily Life",
        "Food",
        "Travel",
        "Weather",
        "Health",
        "Work",
        "Education",
        "Technology",
        "Entertainment",
        "Sports",
        "Language"
    ).sorted()
    
    /**
     * Get topics recommended for beginners.
     */
    val beginnerTopics = listOf(
        "Greetings",
        "Introductions",
        "Daily Life",
        "Food"
    )
    
    /**
     * Get topics by difficulty level.
     */
    fun getTopicsForLevel(level: GermanLevel): List<String> {
        return when (level) {
            GermanLevel.A1, GermanLevel.A2 -> beginnerTopics
            else -> topics
        }
    }
}

/**
 * Available native languages.
 * Organized alphabetically for better UX.
 */
object AvailableLanguages {
    val languages = listOf(
        "Albanian",
        "Arabic", 
        "English",
        "Italian",
        "Russian",
        "Serbian",
        "Turkish",
        "Ukrainian"
    ).sorted()
    
    /**
     * Get language display name with flag emoji if available.
     */
    fun getDisplayName(language: String): String {
        return when (language) {
            "Albanian" -> "🇦🇱 Albanian"
            "Arabic" -> "🇸🇦 Arabic"
            "English" -> "🇺🇸 English"
            "Italian" -> "🇮🇹 Italian"
            "Russian" -> "🇷🇺 Russian"
            "Serbian" -> "🇷🇸 Serbian"
            "Turkish" -> "🇹🇷 Turkish"
            "Ukrainian" -> "🇺🇦 Ukrainian"
            else -> language
        }
    }
} 