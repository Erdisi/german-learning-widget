package com.germanleraningwidget.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.germanleraningwidget.data.model.GermanLevel
import com.germanleraningwidget.data.model.UserPreferences
import com.germanleraningwidget.data.repository.UserPreferencesRepository

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
     * Load current user preferences and initialize UI state with optimized performance.
     */
    private fun loadCurrentPreferences() {
        viewModelScope.launch(exceptionHandler) {
            stateMutex.withLock {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            
            try {
                preferencesRepository.userPreferences
                    .distinctUntilChanged { old, new ->
                        // Custom equality check to avoid unnecessary updates - Optimized: More efficient comparison
                        old.selectedGermanLevels == new.selectedGermanLevels &&
                        old.primaryGermanLevel == new.primaryGermanLevel &&
                        old.selectedTopics == new.selectedTopics &&
                        old.isOnboardingCompleted == new.isOnboardingCompleted
                    }
                    .catch { e ->
                        Log.e(TAG, "Error loading preferences", e)
                        updateStateWithError("Failed to load preferences: ${e.message}")
                    }
                    .collect { preferences ->
                        stateMutex.withLock {
                            // Only update if the UI state actually changed to avoid unnecessary recompositions - Fixed: Better error handling
                            val currentState = _uiState.value
                            try {
                                val needsUpdate = currentState.selectedGermanLevels != preferences.selectedGermanLevels ||
                                        currentState.primaryGermanLevel != preferences.primaryGermanLevel ||
                                        currentState.selectedTopics != preferences.selectedTopics ||
                                        currentState.isOnboardingCompleted != preferences.isOnboardingCompleted ||
                                        currentState.isLoading
                                
                                if (needsUpdate) {
                                    _uiState.value = currentState.copy(
                                        selectedGermanLevels = preferences.selectedGermanLevels.toSet(), // Fixed: Ensure immutability
                                        primaryGermanLevel = preferences.primaryGermanLevel,
                                        selectedTopics = preferences.selectedTopics.toMutableSet(),
                                        isOnboardingCompleted = preferences.isOnboardingCompleted,
                                        isLoading = false,
                                        error = null
                                    )
                                    Log.d(TAG, "Preferences loaded and UI state updated")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error updating UI state", e)
                                updateStateWithError("Failed to update preferences display")
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in loadCurrentPreferences", e)
                updateStateWithError("Failed to initialize preferences: ${e.message}")
            }
        }
    }
    
    /**
     * Update selected German level - LEGACY METHOD for backward compatibility
     * 
     * ⚠️ DEPRECATED: Use updateSelectedGermanLevels and updatePrimaryGermanLevel instead for multi-level support
     * 
     * Migration Guide:
     * - Replace: viewModel.updateGermanLevel(level)
     * - With: viewModel.updateSelectedGermanLevels(setOf(level)) + viewModel.updatePrimaryGermanLevel(level)
     * - Or use: viewModel.updateSelectedGermanLevels(levels) for multiple levels
     * 
     * @deprecated Use updateSelectedGermanLevels and updatePrimaryGermanLevel instead
     */
    @Deprecated(
        message = "Use updateSelectedGermanLevels and updatePrimaryGermanLevel for multi-level support",
        replaceWith = ReplaceWith("updateSelectedGermanLevels(setOf(level)); updatePrimaryGermanLevel(level)"),
        level = DeprecationLevel.WARNING
    )
    fun updateGermanLevel(level: String) {
        _uiState.value = _uiState.value.copy(
            selectedGermanLevels = setOf(level),
            primaryGermanLevel = level
        )
    }
    
    /**
     * Update selected German levels (multi-level support)
     */
    fun updateSelectedGermanLevels(levels: Set<String>) {
        val currentState = _uiState.value
        val validLevels = levels.filter { it.isNotBlank() }.toSet()
        
        if (validLevels.isEmpty()) {
            Log.w(TAG, "Cannot set empty German levels")
            return
        }
        
        // Ensure primary level is still valid
        val newPrimaryLevel = if (currentState.primaryGermanLevel in validLevels) {
            currentState.primaryGermanLevel
        } else {
            // Set primary to the lowest selected level
            validLevels.minByOrNull { level ->
                when (level) {
                    "A1" -> 1; "A2" -> 2; "B1" -> 3; "B2" -> 4; "C1" -> 5; "C2" -> 6
                    else -> 1
                }
            } ?: validLevels.first()
        }
        
        _uiState.value = currentState.copy(
            selectedGermanLevels = validLevels,
            primaryGermanLevel = newPrimaryLevel
        )
    }
    
    /**
     * Toggle a German level (add if not present, remove if present)
     */
    fun toggleGermanLevel(level: String) {
        val currentState = _uiState.value
        val currentLevels = currentState.selectedGermanLevels
        
        if (level.isBlank()) {
            Log.w(TAG, "Cannot toggle blank German level")
            return
        }
        
        val newLevels = if (currentLevels.contains(level)) {
            // Removing level - ensure at least one remains
            if (currentLevels.size <= 1) {
                Log.w(TAG, "Cannot remove the last German level")
                return
            }
            
            // Don't allow removing primary level
            if (level == currentState.primaryGermanLevel) {
                Log.w(TAG, "Cannot remove primary German level. Change primary first.")
                return
            }
            
            currentLevels - level
        } else {
            // Adding level
            currentLevels + level
        }
        
        updateSelectedGermanLevels(newLevels)
    }
    
    /**
     * Update primary German level
     */
    fun updatePrimaryGermanLevel(level: String) {
        val currentState = _uiState.value
        
        if (level.isBlank()) {
            Log.w(TAG, "Cannot set blank primary German level")
            return
        }
        
        // Ensure the level is in selected levels
        val newLevels = if (level in currentState.selectedGermanLevels) {
            currentState.selectedGermanLevels
        } else {
            currentState.selectedGermanLevels + level
        }
        
        _uiState.value = currentState.copy(
            selectedGermanLevels = newLevels,
            primaryGermanLevel = level
        )
    }
    
    /**
     * Update selected topics
     */
    fun updateSelectedTopics(topics: Set<String>) {
        _uiState.value = _uiState.value.copy(selectedTopics = topics.toMutableSet())
    }
    

    
    /**
     * Complete onboarding and save preferences
     */
    fun completeOnboarding() {
        viewModelScope.launch(exceptionHandler) {
            try {
                stateMutex.withLock {
                    _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                }
                
                val currentState = _uiState.value
                
                // Validate form before saving
                val validationResult = validateForm(currentState)
                if (!validationResult.isSuccess) {
                    updateStateWithError(validationResult.errorMessage ?: "Form validation failed")
                    return@launch
                }
                
                val preferences = UserPreferences(
                    selectedGermanLevels = currentState.selectedGermanLevels,
                    primaryGermanLevel = currentState.primaryGermanLevel,
                    selectedTopics = currentState.selectedTopics.toSet(),
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
                
                Log.i(TAG, "Onboarding completed successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete onboarding", e)
                val errorMessage = when (e) {
                    is ValidationException -> e.message ?: "Validation error"
                    else -> "An unexpected error occurred"
                }
                updateStateWithError(errorMessage)
            }
        }
    }
    
    /**
     * Save user preferences with comprehensive validation.
     */
    suspend fun savePreferences(): Result<Unit> {
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
                selectedGermanLevels = currentState.selectedGermanLevels,
                primaryGermanLevel = currentState.primaryGermanLevel,
                selectedTopics = currentState.selectedTopics.toSet(),
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
            Result.success(Unit)
            
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
        if (state.selectedGermanLevels.isEmpty()) {
            return UserPreferences.ValidationResult.Error("Please select at least one German level")
        }
        
        if (state.primaryGermanLevel !in state.selectedGermanLevels) {
            return UserPreferences.ValidationResult.Error("Primary level must be one of the selected levels")
        }
        
        if (state.selectedTopics.isEmpty()) {
            return UserPreferences.ValidationResult.Error("Please select at least one topic")
        }
        
        val invalidTopics = state.selectedTopics.filter { !com.germanleraningwidget.data.model.AvailableTopics.topics.contains(it) }
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
               state.selectedGermanLevels.isNotEmpty() &&
               state.selectedTopics.isNotEmpty() &&
               state.primaryGermanLevel in state.selectedGermanLevels &&
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
 * UI State for onboarding flow - UPDATED FOR MULTI-LEVEL SUPPORT
 */
data class OnboardingUiState(
    // Multi-level German selection
    val selectedGermanLevels: Set<String> = emptySet(),
    val primaryGermanLevel: String = "",
    
    // Other preferences
    val selectedTopics: MutableSet<String> = mutableSetOf(),
    
    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val isOnboardingCompleted: Boolean = false
) {
    // Backward compatibility property
    /**
     * Backward compatibility property for single level selection
     * 
     * ⚠️ DEPRECATED: Use selectedGermanLevels and primaryGermanLevel instead for multi-level support
     * 
     * Migration Guide:
     * - Replace: uiState.selectedLevel
     * - With: uiState.primaryGermanLevel
     * - Or for multiple levels: uiState.selectedGermanLevels
     * 
     * @deprecated Use selectedGermanLevels and primaryGermanLevel instead
     */
    @Deprecated(
        message = "Use primaryGermanLevel for single level or selectedGermanLevels for multi-level support",
        replaceWith = ReplaceWith("primaryGermanLevel"),
        level = DeprecationLevel.WARNING
    )
    val selectedLevel: String get() = primaryGermanLevel
    
    /**
     * Check if the form is valid for completion
     */
    val isFormValid: Boolean get() = selectedGermanLevels.isNotEmpty() && 
                                    selectedTopics.isNotEmpty() &&
                                    primaryGermanLevel in selectedGermanLevels
}

// AvailableTopics moved to UserPreferences.kt for unified topic management
// Now using com.germanleraningwidget.data.model.AvailableTopics across the entire app

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