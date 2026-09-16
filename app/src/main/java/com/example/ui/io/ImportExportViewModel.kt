package com.example.ui.io

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.repository.FlashcardRepository
import com.example.domain.io.FlashcardBackupDto
import com.example.domain.io.FlashcardImportExportHelper
import com.example.domain.srs.CardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ImportMode {
    NEW_DECKS,
    MERGE_INTO_EXISTING
}

data class ImportExportUiState(
    val decks: List<DeckEntity> = emptyList(),
    val selectedDeckId: Long = 0L,
    val exportedJson: String = "",
    val importInputText: String = "",
    val isInspecting: Boolean = false,
    val previewBackup: FlashcardBackupDto? = null,
    val importMode: ImportMode = ImportMode.NEW_DECKS,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class ImportExportViewModel(
    private val initialDeckId: Long = 0L,
    private val repository: FlashcardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportExportUiState(selectedDeckId = initialDeckId))
    val uiState: StateFlow<ImportExportUiState> = _uiState.asStateFlow()

    init {
        loadDecksAndGenerateExport()
    }

    private fun loadDecksAndGenerateExport() {
        viewModelScope.launch {
            repository.allDecks.collect { decks ->
                val targetId = if (_uiState.value.selectedDeckId != 0L) {
                    _uiState.value.selectedDeckId
                } else {
                    decks.firstOrNull()?.id ?: 0L
                }
                _uiState.value = _uiState.value.copy(decks = decks, selectedDeckId = targetId)
                refreshExportJson(targetId)
            }
        }
    }

    fun onDeckSelected(deckId: Long) {
        _uiState.value = _uiState.value.copy(selectedDeckId = deckId)
        viewModelScope.launch {
            refreshExportJson(deckId)
        }
    }

    fun onImportInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(
            importInputText = text,
            errorMessage = null,
            previewBackup = null
        )
    }

    fun setImportMode(mode: ImportMode) {
        _uiState.value = _uiState.value.copy(importMode = mode)
    }

    private suspend fun refreshExportJson(deckId: Long) {
        val pairs = mutableListOf<Pair<DeckEntity, List<CardEntity>>>()
        if (deckId > 0) {
            val deck = _uiState.value.decks.find { it.id == deckId }
            if (deck != null) {
                val cards = repository.getCardsSnapshot(deckId)
                pairs.add(deck to cards)
            }
        } else {
            for (deck in _uiState.value.decks) {
                val cards = repository.getCardsSnapshot(deck.id)
                pairs.add(deck to cards)
            }
        }
        val json = FlashcardImportExportHelper.exportToJson(pairs)
        _uiState.value = _uiState.value.copy(exportedJson = json)
    }

    fun inspectImportJson() {
        val text = _uiState.value.importInputText.trim()
        if (text.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please paste valid JSON content.")
            return
        }

        val parsed = FlashcardImportExportHelper.parseImportJson(text)
        parsed.fold(
            onSuccess = { backup ->
                _uiState.value = _uiState.value.copy(
                    previewBackup = backup,
                    errorMessage = null
                )
            },
            onFailure = { err ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = err.localizedMessage ?: "Invalid JSON format."
                )
            }
        )
    }

    fun confirmImport() {
        val preview = _uiState.value.previewBackup ?: return
        val mode = _uiState.value.importMode
        val targetDeckId = _uiState.value.selectedDeckId

        viewModelScope.launch {
            var totalCardsImported = 0
            when (mode) {
                ImportMode.NEW_DECKS -> {
                    for (deckBackup in preview.decks) {
                        val cardsToInsert = deckBackup.cards.map { cardDto ->
                            CardEntity(
                                deckId = 0,
                                front = cardDto.front,
                                back = cardDto.back,
                                state = try { CardState.valueOf(cardDto.state) } catch (e: Exception) { CardState.NEW },
                                repetitions = cardDto.repetitions,
                                intervalDays = cardDto.intervalDays,
                                easeFactor = cardDto.easeFactor
                            )
                        }
                        repository.importDeckWithCards(deckBackup.name, deckBackup.description, cardsToInsert)
                        totalCardsImported += cardsToInsert.size
                    }
                }
                ImportMode.MERGE_INTO_EXISTING -> {
                    val targetId = if (targetDeckId != 0L) {
                        targetDeckId
                    } else {
                        repository.createDeck("Merged Decks")
                    }

                    val cardsToInsert = preview.decks.flatMap { it.cards }.map { cardDto ->
                        CardEntity(
                            deckId = targetId,
                            front = cardDto.front,
                            back = cardDto.back,
                            state = try { CardState.valueOf(cardDto.state) } catch (e: Exception) { CardState.NEW },
                            repetitions = cardDto.repetitions,
                            intervalDays = cardDto.intervalDays,
                            easeFactor = cardDto.easeFactor
                        )
                    }
                    repository.createCards(cardsToInsert)
                    totalCardsImported = cardsToInsert.size
                }
            }

            _uiState.value = _uiState.value.copy(
                previewBackup = null,
                importInputText = "",
                successMessage = "Successfully imported $totalCardsImported card(s)!"
            )
            refreshExportJson(_uiState.value.selectedDeckId)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    class Factory(
        private val initialDeckId: Long,
        private val repository: FlashcardRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ImportExportViewModel(initialDeckId, repository) as T
        }
    }
}
