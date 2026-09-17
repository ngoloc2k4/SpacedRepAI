package com.example.ui.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.repository.FlashcardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardListUiState(
    val deck: DeckEntity? = null,
    val cards: List<CardEntity> = emptyList(),
    val isLoading: Boolean = true
)

class CardListViewModel(
    private val deckId: Long,
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardListUiState())
    val uiState: StateFlow<CardListUiState> = _uiState.asStateFlow()

    init {
        loadDeckAndCards()
    }

    private fun loadDeckAndCards() {
        viewModelScope.launch {
            repository.getDeck(deckId).collect { deck ->
                _uiState.value = _uiState.value.copy(deck = deck)
            }
        }
        viewModelScope.launch {
            repository.getCardsForDeck(deckId).collect { cards ->
                _uiState.value = _uiState.value.copy(cards = cards, isLoading = false)
            }
        }
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
