package com.example.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.preferences.AppSettings
import com.example.data.preferences.AppSettingsManager
import com.example.data.repository.FlashcardRepository
import com.example.domain.ai.AiService
import com.example.domain.ai.AnswerEvaluation
import com.example.domain.audio.TtsManager
import com.example.domain.srs.IntervalPreview
import com.example.domain.srs.IntervalPreviewHelper
import com.example.domain.srs.ReviewRating
import com.example.domain.srs.ReviewSession
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class ReviewUiState(
    val session: ReviewSession? = null,
    val deck: DeckEntity? = null,
    val isFlipped: Boolean = false,
    val intervalPreviews: Map<ReviewRating, IntervalPreview> = emptyMap(),
    val isLoading: Boolean = true,
    // AI additions
    val typeAnswerMode: Boolean = false,
    val userAnswer: String = "",
    val isAiEvaluating: Boolean = false,
    val aiEvaluation: AnswerEvaluation? = null,
    val isAiExplaining: Boolean = false,
    val aiDialogTitle: String? = null,
    val aiDialogContent: String? = null,
    // Audio / TTS (Phase 8)
    val isSpeaking: Boolean = false,
    val hapticEnabled: Boolean = true,
    // Settings & Timer (Phase 9)
    val timerTotalSeconds: Int = 0,
    val timerSecondsRemaining: Int = 0
) {
    val currentCard: CardEntity?
        get() = session?.currentCard

    val totalQueueSize: Int
        get() = session?.totalQueueSize ?: 0

    val currentIndex: Int
        get() = session?.currentIndex ?: 0

    val progress: Float
        get() = session?.progress ?: 0f

    val isCompleted: Boolean
        get() = session?.isCompleted ?: false

    val reviewedCount: Int
        get() = session?.reviewedCount ?: 0

    val againCount: Int
        get() = session?.againCount ?: 0

    val hardCount: Int
        get() = session?.hardCount ?: 0

    val goodCount: Int
        get() = session?.goodCount ?: 0

    val easyCount: Int
        get() = session?.easyCount ?: 0

    val formattedDuration: String
        get() = session?.formatDuration() ?: "0s"
}

class ReviewViewModel(
    private val deckId: Long,
    private val repository: FlashcardRepository,
    private val aiService: AiService,
    private val ttsManager: TtsManager,
    private val appSettingsManager: AppSettingsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private var currentSettings: AppSettings = appSettingsManager.settingsFlow.value
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                _uiState.value = _uiState.value.copy(isSpeaking = speaking)
            }
        }

        viewModelScope.launch {
            appSettingsManager.settingsFlow.collect { settings ->
                currentSettings = settings
                _uiState.value = _uiState.value.copy(
                    hapticEnabled = settings.hapticEnabled,
                    timerTotalSeconds = settings.cardTimerSeconds
                )
            }
        }

        loadDeckAndQueue()
    }

    private fun loadDeckAndQueue() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val (deck, allCards) = if (deckId > 0) {
                val d = repository.getDeck(deckId).firstOrNull()
                val cards = repository.getCardsSnapshot(deckId)
                d to cards
            } else {
                null to repository.getAllCardsSnapshot()
            }

            // Apply daily limits from Phase 9 settings
            val dailyQueue = repository.getScheduler().buildDailyQueue(
                cards = allCards,
                maxNewCards = currentSettings.dailyNewCardsLimit,
                maxReviewCards = currentSettings.dailyReviewCardsLimit,
                currentTime = now
            )
            val studyList = if (dailyQueue.isEmpty && allCards.isNotEmpty()) {
                allCards
            } else {
                dailyQueue.toStudyList()
            }

            val session = ReviewSession(
                deckId = deckId,
                deckName = deck?.name ?: "Daily Review",
                queue = studyList,
                startTimeMillis = now
            )

            val previews = session.currentCard?.let { card ->
                IntervalPreviewHelper.getPreviewsForCard(card, repository.getAlgorithm(), now)
            } ?: emptyMap()

            _uiState.value = ReviewUiState(
                session = session,
                deck = deck,
                isFlipped = false,
                intervalPreviews = previews,
                isLoading = false,
                hapticEnabled = currentSettings.hapticEnabled,
                timerTotalSeconds = currentSettings.cardTimerSeconds,
                timerSecondsRemaining = currentSettings.cardTimerSeconds
            )

            onCardPresented()
        }
    }

    private fun onCardPresented() {
        timerJob?.cancel()
        val timerSec = currentSettings.cardTimerSeconds
        if (timerSec > 0) {
            _uiState.value = _uiState.value.copy(timerSecondsRemaining = timerSec)
            timerJob = viewModelScope.launch {
                var remaining = timerSec
                while (remaining > 0 && !_uiState.value.isFlipped) {
                    delay(1000L)
                    remaining--
                    _uiState.value = _uiState.value.copy(timerSecondsRemaining = remaining)
                }
            }
        }

        if (currentSettings.autoSpeakFront) {
            speakCurrentFront()
        }
    }

    fun flipCard() {
        timerJob?.cancel()
        val willFlipToBack = !_uiState.value.isFlipped
        _uiState.value = _uiState.value.copy(isFlipped = willFlipToBack)

        if (willFlipToBack && currentSettings.autoSpeakBack) {
            speakCurrentBack()
        }
    }

    fun speakCurrentFront() {
        _uiState.value.currentCard?.let {
            ttsManager.speak(it.front, currentSettings.speechRate, currentSettings.speechPitch)
        }
    }

    fun speakCurrentBack() {
        _uiState.value.currentCard?.let {
            ttsManager.speak(it.back, currentSettings.speechRate, currentSettings.speechPitch)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun onUserAnswerChanged(text: String) {
        _uiState.value = _uiState.value.copy(userAnswer = text)
    }

    fun toggleTypeAnswerMode() {
        _uiState.value = _uiState.value.copy(typeAnswerMode = !_uiState.value.typeAnswerMode)
    }

    fun evaluateAnswerWithAi() {
        val currentCard = _uiState.value.currentCard ?: return
        val answer = _uiState.value.userAnswer.trim()
        if (answer.isEmpty()) return

        _uiState.value = _uiState.value.copy(isAiEvaluating = true)
        viewModelScope.launch {
            val result = aiService.evaluateAnswer(
                front = currentCard.front,
                expectedBack = currentCard.back,
                userAnswer = answer
            )

            result.fold(
                onSuccess = { eval ->
                    _uiState.value = _uiState.value.copy(
                        isAiEvaluating = false,
                        aiEvaluation = eval,
                        isFlipped = true // reveal card after evaluation
                    )
                    if (currentSettings.autoSpeakBack) {
                        speakCurrentBack()
                    }
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isAiEvaluating = false,
                        aiDialogTitle = "AI Evaluation",
                        aiDialogContent = err.localizedMessage ?: "Could not evaluate answer."
                    )
                }
            )
        }
    }

    fun requestAiExplanation() {
        val currentCard = _uiState.value.currentCard ?: return
        _uiState.value = _uiState.value.copy(isAiExplaining = true)

        viewModelScope.launch {
            val result = aiService.explainCard(currentCard.front, currentCard.back)
            result.fold(
                onSuccess = { explanation ->
                    _uiState.value = _uiState.value.copy(
                        isAiExplaining = false,
                        aiDialogTitle = "AI Explanation",
                        aiDialogContent = explanation
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isAiExplaining = false,
                        aiDialogTitle = "AI Explanation",
                        aiDialogContent = err.localizedMessage ?: "Could not load explanation."
                    )
                }
            )
        }
    }

    fun requestAiMnemonic() {
        val currentCard = _uiState.value.currentCard ?: return
        _uiState.value = _uiState.value.copy(isAiExplaining = true)

        viewModelScope.launch {
            val result = aiService.generateMnemonic(currentCard.front, currentCard.back)
            result.fold(
                onSuccess = { mnemonic ->
                    _uiState.value = _uiState.value.copy(
                        isAiExplaining = false,
                        aiDialogTitle = "Mnemonic Hook",
                        aiDialogContent = mnemonic
                    )
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isAiExplaining = false,
                        aiDialogTitle = "Mnemonic Hook",
                        aiDialogContent = err.localizedMessage ?: "Could not generate mnemonic."
                    )
                }
            )
        }
    }

    fun dismissAiDialog() {
        _uiState.value = _uiState.value.copy(aiDialogTitle = null, aiDialogContent = null)
    }

    fun rateCard(rating: ReviewRating) {
        timerJob?.cancel()
        ttsManager.stop()
        val currentSession = _uiState.value.session ?: return
        val currentCard = currentSession.currentCard ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.reviewCard(currentCard, rating, now)

            val updatedSession = currentSession.applyRating(rating)
            val nextPreviews = updatedSession.currentCard?.let { nextCard ->
                IntervalPreviewHelper.getPreviewsForCard(nextCard, repository.getAlgorithm(), now)
            } ?: emptyMap()

            _uiState.value = _uiState.value.copy(
                session = updatedSession,
                isFlipped = false,
                intervalPreviews = nextPreviews,
                userAnswer = "",
                aiEvaluation = null
            )

            if (!updatedSession.isCompleted) {
                onCardPresented()
            }
        }
    }

    fun restartSession() {
        _uiState.value = _uiState.value.copy(isLoading = true, userAnswer = "", aiEvaluation = null)
        loadDeckAndQueue()
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        ttsManager.stop()
    }

    class Factory(
        private val deckId: Long,
        private val repository: FlashcardRepository,
        private val aiService: AiService,
        private val ttsManager: TtsManager,
        private val appSettingsManager: AppSettingsManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReviewViewModel(deckId, repository, aiService, ttsManager, appSettingsManager) as T
        }
    }
}
