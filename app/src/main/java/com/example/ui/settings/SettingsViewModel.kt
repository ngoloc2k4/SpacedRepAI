package com.example.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CardEntity
import com.example.data.preferences.AiProvider
import com.example.data.preferences.AppSettings
import com.example.data.preferences.AppSettingsManager
import com.example.data.repository.FlashcardRepository
import com.example.domain.ai.AiService
import com.example.domain.audio.TtsManager
import com.example.domain.notification.ReminderManager
import com.example.domain.srs.CardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val leechCardsCount: Int = 0,
    val leechCards: List<CardEntity> = emptyList(),
    val testNotificationSent: Boolean = false,
    val isTestingAi: Boolean = false,
    val aiTestSuccess: Boolean? = null,
    val aiTestMessage: String? = null
)

class SettingsViewModel(
    private val appSettingsManager: AppSettingsManager,
    private val reminderManager: ReminderManager,
    private val ttsManager: TtsManager,
    private val repository: FlashcardRepository,
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettingsManager.settingsFlow.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
        loadLeechCards()
    }

    fun loadLeechCards() {
        viewModelScope.launch {
            val allCards = repository.getAllCardsSnapshot()
            // Cards are considered difficult/leech if in RELEARNING or high reps with low ease factor or low interval
            val leeches = allCards.filter {
                it.state == CardState.RELEARNING ||
                (it.repetitions > 0 && it.easeFactor <= 1.7) ||
                (it.repetitions > 2 && it.intervalDays <= 1)
            }
            _uiState.value = _uiState.value.copy(
                leechCardsCount = leeches.size,
                leechCards = leeches
            )
        }
    }

    fun updateDailyNewCardsLimit(limit: Int) {
        appSettingsManager.updateDailyNewCardsLimit(limit)
    }

    fun updateDailyReviewCardsLimit(limit: Int) {
        appSettingsManager.updateDailyReviewCardsLimit(limit)
    }

    fun updateCardTimer(seconds: Int) {
        appSettingsManager.updateCardTimerSeconds(seconds)
    }

    fun updateAutoSpeakFront(enabled: Boolean) {
        appSettingsManager.updateAutoSpeakFront(enabled)
    }

    fun updateAutoSpeakBack(enabled: Boolean) {
        appSettingsManager.updateAutoSpeakBack(enabled)
    }

    fun updateSpeechRate(rate: Float) {
        appSettingsManager.updateSpeechRate(rate)
        ttsManager.setSpeechRate(rate)
    }

    fun updateSpeechPitch(pitch: Float) {
        appSettingsManager.updateSpeechPitch(pitch)
        ttsManager.setSpeechPitch(pitch)
    }

    fun testTts(sampleText: String) {
        ttsManager.speak(
            text = sampleText,
            rate = _uiState.value.settings.speechRate,
            pitch = _uiState.value.settings.speechPitch
        )
    }

    fun updateHaptic(enabled: Boolean) {
        appSettingsManager.updateHapticEnabled(enabled)
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        appSettingsManager.updateReminder(enabled, hour, minute)
        if (enabled) {
            reminderManager.scheduleDailyReminder(hour, minute)
        } else {
            reminderManager.cancelReminder()
        }
    }

    fun sendTestReminder() {
        viewModelScope.launch {
            val due = repository.getAllCardsSnapshot().size
            reminderManager.sendTestNotification(due.coerceAtLeast(1))
            _uiState.value = _uiState.value.copy(testNotificationSent = true)
        }
    }

    fun clearNotificationSentFlag() {
        _uiState.value = _uiState.value.copy(testNotificationSent = false)
    }

    // AI Provider & Custom Model Methods
    fun updateAiProvider(provider: AiProvider) {
        appSettingsManager.resetAiToProviderDefaults(provider)
    }

    fun updateAiEndpoint(endpoint: String) {
        appSettingsManager.updateAiEndpoint(endpoint)
    }

    fun updateAiHttpMethod(method: String) {
        appSettingsManager.updateAiHttpMethod(method)
    }

    fun updateAiModel(model: String) {
        appSettingsManager.updateAiModel(model)
    }

    fun updateAiApiKey(apiKey: String) {
        appSettingsManager.updateAiApiKey(apiKey)
    }

    fun updateAiCustomHeaders(headers: String) {
        appSettingsManager.updateAiCustomHeaders(headers)
    }

    fun resetAiDefaults() {
        val currentProvider = _uiState.value.settings.aiProvider
        appSettingsManager.resetAiToProviderDefaults(currentProvider)
    }

    fun testAiConnection() {
        _uiState.value = _uiState.value.copy(
            isTestingAi = true,
            aiTestSuccess = null,
            aiTestMessage = null
        )
        viewModelScope.launch {
            val result = aiService.testConnection()
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        isTestingAi = false,
                        aiTestSuccess = true,
                        aiTestMessage = msg
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isTestingAi = false,
                        aiTestSuccess = false,
                        aiTestMessage = err.localizedMessage ?: "Connection failed"
                    )
                }
            )
        }
    }

    fun dismissAiTestDialog() {
        _uiState.value = _uiState.value.copy(
            aiTestSuccess = null,
            aiTestMessage = null
        )
    }

    class Factory(
        private val appSettingsManager: AppSettingsManager,
        private val reminderManager: ReminderManager,
        private val ttsManager: TtsManager,
        private val repository: FlashcardRepository,
        private val aiService: AiService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                appSettingsManager = appSettingsManager,
                reminderManager = reminderManager,
                ttsManager = ttsManager,
                repository = repository,
                aiService = aiService
            ) as T
        }
    }
}
