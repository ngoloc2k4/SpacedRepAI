package com.example.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DeckEntity
import com.example.data.repository.FlashcardRepository
import com.example.domain.srs.CardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class TodaySummary(
    val dueCount: Int = 0,
    val learningCount: Int = 0,
    val newCount: Int = 0,
    val totalToReview: Int = 0
)

data class DeckItemUiModel(
    val deck: DeckEntity,
    val totalCards: Int = 0,
    val dueCards: Int = 0,
    val learningCards: Int = 0,
    val newCards: Int = 0,
    val reviewProgress: Float = 0f
)

data class DeckListUiState(
    val todaySummary: TodaySummary = TodaySummary(),
    val decks: List<DeckItemUiModel> = emptyList(),
    val isLoading: Boolean = true
)

class DeckListViewModel(
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckListUiState())
    val uiState: StateFlow<DeckListUiState> = _uiState.asStateFlow()

    init {
        loadDecksWithStats()
    }

    private fun loadDecksWithStats() {
        viewModelScope.launch {
            combine(repository.allDecks, repository.allCards) { decks, allCards ->
                val now = System.currentTimeMillis()
                val scheduler = repository.getScheduler()

                val cardsByDeck = allCards.groupBy { it.deckId }

                var totalGlobalDue = 0
                var totalGlobalLearning = 0
                var totalGlobalNew = 0

                val deckItems = decks.map { deck ->
                    val cards = cardsByDeck[deck.id] ?: emptyList()
                    val queue = scheduler.buildDailyQueue(cards, currentTime = now)

                    val dueCount = queue.dueCards.size
                    val learningCount = queue.learningCards.size
                    val newCount = queue.newCards.size

                    totalGlobalDue += dueCount
                    totalGlobalLearning += learningCount
                    totalGlobalNew += newCount

                    val reviewedCount = cards.count { it.state == CardState.REVIEW && it.repetitions > 0 }
                    val progress = if (cards.isNotEmpty()) {
                        (reviewedCount.toFloat() / cards.size.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    DeckItemUiModel(
                        deck = deck,
                        totalCards = cards.size,
                        dueCards = dueCount,
                        learningCards = learningCount,
                        newCards = newCount,
                        reviewProgress = progress
                    )
                }

                val globalQueue = scheduler.buildDailyQueue(allCards, currentTime = now)
                val summary = TodaySummary(
                    dueCount = totalGlobalDue,
                    learningCount = totalGlobalLearning,
                    newCount = totalGlobalNew,
                    totalToReview = globalQueue.totalCount
                )

                DeckListUiState(
                    todaySummary = summary,
                    decks = deckItems,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun createDeck(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createDeck(name, description)
        }
    }

    fun deleteDeck(deckId: Long) {
        viewModelScope.launch {
            repository.deleteDeck(deckId)
        }
    }

    class Factory(private val repository: FlashcardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeckListViewModel(repository) as T
        }
    }
}
