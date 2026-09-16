package com.example.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.FlashcardRepository
import com.example.domain.stats.StatsCalculator
import com.example.domain.stats.StatsData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class StatsUiState(
    val stats: StatsData = StatsData(),
    val isLoading: Boolean = true
)

class StatsViewModel(
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            combine(
                repository.allCards,
                repository.allReviewLogs,
                repository.allDecks
            ) { cards, logs, decks ->
                val calculated = StatsCalculator.calculate(
                    cards = cards,
                    logs = logs,
                    decksCount = decks.size
                )
                StatsUiState(stats = calculated, isLoading = false)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    class Factory(private val repository: FlashcardRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(repository) as T
        }
    }
}
