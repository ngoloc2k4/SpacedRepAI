package com.example.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.repository.FlashcardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardListUiState(
    val deck: DeckEntity? = null,
    val cards: List<CardEntity> = emptyList(),
    val totalCount: Int = 0,
    val displayedCount: Int = 0,
    val pageSize: Int = 25,
    val hasMoreCards: Boolean = false,
    val isLoadingMore: Boolean = false,
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class CardListViewModel(
    private val deckId: Long,
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardListUiState())
    val uiState: StateFlow<CardListUiState> = _uiState.asStateFlow()

    private var allCardsCache: List<CardEntity> = emptyList()
    private var currentPage: Int = 1
    private var cardsCollectorJob: Job? = null

    init {
        loadDeckAndCards()
    }

    private fun loadDeckAndCards() {
        viewModelScope.launch {
            repository.getDeck(deckId).collect { deck ->
                _uiState.value = _uiState.value.copy(deck = deck)
            }
        }
        observeCards()
    }

    private fun observeCards() {
        cardsCollectorJob?.cancel()
        cardsCollectorJob = viewModelScope.launch {
            repository.getCardsForDeck(deckId).collect { cards ->
                allCardsCache = cards
                applyPaginationAndFilter()
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        currentPage = 1
        applyPaginationAndFilter()
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (!currentState.hasMoreCards || currentState.isLoadingMore) return
        currentPage++
        applyPaginationAndFilter()
    }

    private fun applyPaginationAndFilter() {
        val query = _uiState.value.searchQuery.trim()
        val filtered = if (query.isEmpty()) {
            allCardsCache
        } else {
            allCardsCache.filter {
                it.front.contains(query, ignoreCase = true) ||
                it.back.contains(query, ignoreCase = true)
            }
        }

        val limit = currentPage * _uiState.value.pageSize
        val paginated = filtered.take(limit)
        val hasMore = paginated.size < filtered.size

        _uiState.value = _uiState.value.copy(
            cards = paginated,
            totalCount = filtered.size,
            displayedCount = paginated.size,
            hasMoreCards = hasMore,
            isLoading = false,
            isLoadingMore = false
        )
    }

    fun addCard(front: String, back: String) {
        if (front.isBlank() || back.isBlank()) return
        viewModelScope.launch {
            repository.createCard(deckId, front, back)
        }
    }

    fun updateCard(card: CardEntity, newFront: String, newBack: String) {
        if (newFront.isBlank() || newBack.isBlank()) return
        viewModelScope.launch {
            repository.updateCard(card.copy(front = newFront.trim(), back = newBack.trim()))
        }
    }

    fun deleteCard(cardId: Long) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    class Factory(
        private val deckId: Long,
        private val repository: FlashcardRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CardListViewModel(deckId, repository) as T
        }
    }
}

