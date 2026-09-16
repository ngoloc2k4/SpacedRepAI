package com.example.ui.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.DeckEntity
import com.example.data.repository.FlashcardRepository
import com.example.domain.ai.AiService
import com.example.domain.ai.GeneratedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AiInputMode {
    TEXT,
    IMAGE
}

enum class SaveDestination {
    CURRENT_DECK,
    NEW_DECK
}

data class EditableGeneratedCard(
    val id: String = java.util.UUID.randomUUID().toString(),
    var front: String,
    var back: String,
    var isSelected: Boolean = true
)

data class AiGeneratorUiState(
    val decks: List<DeckEntity> = emptyList(),
    val selectedDeckId: Long = 0L,
    val inputMode: AiInputMode = AiInputMode.TEXT,
    val topic: String = "",
    val cardCount: Int = 5,
    val selectedImageUri: Uri? = null,
    val selectedImageBase64: String? = null,
    val imageMimeType: String = "image/jpeg",
    val imagePromptHint: String = "",
    val saveDestination: SaveDestination = SaveDestination.CURRENT_DECK,
    val newDeckName: String = "",
    val newDeckDescription: String = "",
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val generatedCards: List<EditableGeneratedCard> = emptyList(),
    val errorMessage: String? = null,
    val successSavedCount: Int? = null,
    val savedToDeckName: String? = null,
    val savedToDeckId: Long? = null
)

class AiGeneratorViewModel(
    private val initialDeckId: Long = 0L,
    private val repository: FlashcardRepository,
    private val aiService: AiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AiGeneratorUiState(selectedDeckId = initialDeckId)
    )
    val uiState: StateFlow<AiGeneratorUiState> = _uiState.asStateFlow()

    init {
        loadDecks()
    }

    private fun loadDecks() {
        viewModelScope.launch {
            repository.allDecks.collect { decks ->
                val targetId = if (_uiState.value.selectedDeckId != 0L) {
                    _uiState.value.selectedDeckId
                } else {
                    decks.firstOrNull()?.id ?: 0L
                }
                _uiState.value = _uiState.value.copy(
                    decks = decks,
                    selectedDeckId = targetId
                )
            }
        }
    }

    fun setInputMode(mode: AiInputMode) {
        _uiState.value = _uiState.value.copy(inputMode = mode, errorMessage = null)
    }

    fun onTopicChanged(newTopic: String) {
        _uiState.value = _uiState.value.copy(topic = newTopic, errorMessage = null)
    }

    fun onCardCountChanged(newCount: Int) {
        _uiState.value = _uiState.value.copy(cardCount = newCount.coerceIn(3, 15))
    }

    fun onDeckSelected(deckId: Long) {
        _uiState.value = _uiState.value.copy(selectedDeckId = deckId)
    }

    fun setSaveDestination(dest: SaveDestination) {
        _uiState.value = _uiState.value.copy(saveDestination = dest)
    }

    fun onNewDeckNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newDeckName = name)
    }

    fun onNewDeckDescriptionChanged(desc: String) {
        _uiState.value = _uiState.value.copy(newDeckDescription = desc)
    }

    fun onImagePromptHintChanged(hint: String) {
        _uiState.value = _uiState.value.copy(imagePromptHint = hint)
    }

    fun onImageSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val maxDim = 1280
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scaledBitmap = if (width > maxDim || height > maxDim) {
                        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
                        Bitmap.createScaledBitmap(originalBitmap, (width * ratio).toInt(), (height * ratio).toInt(), true)
                    } else {
                        originalBitmap
                    }

                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val bytes = outputStream.toByteArray()
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

                    _uiState.value = _uiState.value.copy(
                        selectedImageUri = uri,
                        selectedImageBase64 = base64,
                        imageMimeType = "image/jpeg",
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(errorMessage = "Could not decode the selected image.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Error reading image: ${e.localizedMessage}")
            }
        }
    }

    fun clearSelectedImage() {
        _uiState.value = _uiState.value.copy(
            selectedImageUri = null,
            selectedImageBase64 = null
        )
    }

    fun generateCards() {
        val state = _uiState.value
        if (state.inputMode == AiInputMode.TEXT) {
            val currentTopic = state.topic.trim()
            if (currentTopic.isEmpty()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please enter a topic or study subject.")
                return
            }

            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                successSavedCount = null
            )

            viewModelScope.launch {
                val result = aiService.generateCards(
                    topic = currentTopic,
                    count = state.cardCount
                )

                result.fold(
                    onSuccess = { cards ->
                        val editableList = cards.map {
                            EditableGeneratedCard(front = it.front, back = it.back, isSelected = true)
                        }
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            generatedCards = editableList
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            errorMessage = error.localizedMessage ?: "Failed to generate cards."
                        )
                    }
                )
            }
        } else {
            // IMAGE Mode
            val imageBase64 = state.selectedImageBase64
            if (imageBase64.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(errorMessage = "Please upload or select an image first.")
                return
            }

            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                errorMessage = null,
                successSavedCount = null
            )

            viewModelScope.launch {
                val result = aiService.generateCardsFromImage(
                    imageBase64 = imageBase64,
                    mimeType = state.imageMimeType,
                    count = state.cardCount,
                    promptHint = state.imagePromptHint
                )

                result.fold(
                    onSuccess = { cards ->
                        val editableList = cards.map {
                            EditableGeneratedCard(front = it.front, back = it.back, isSelected = true)
                        }
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            generatedCards = editableList
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isGenerating = false,
                            errorMessage = error.localizedMessage ?: "Failed to scan and generate cards from image."
                        )
                    }
                )
            }
        }
    }

    fun toggleCardSelection(cardId: String) {
        val updated = _uiState.value.generatedCards.map {
            if (it.id == cardId) it.copy(isSelected = !it.isSelected) else it
        }
        _uiState.value = _uiState.value.copy(generatedCards = updated)
    }

    fun updateCardContent(cardId: String, newFront: String, newBack: String) {
        val updated = _uiState.value.generatedCards.map {
            if (it.id == cardId) it.copy(front = newFront, back = newBack) else it
        }
        _uiState.value = _uiState.value.copy(generatedCards = updated)
    }

    fun selectAll(select: Boolean) {
        val updated = _uiState.value.generatedCards.map { it.copy(isSelected = select) }
        _uiState.value = _uiState.value.copy(generatedCards = updated)
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(
            successSavedCount = null,
            savedToDeckName = null,
            savedToDeckId = null
        )
    }

    fun saveSelectedCards() {
        val state = _uiState.value
        val selected = state.generatedCards.filter { it.isSelected && it.front.isNotBlank() && it.back.isNotBlank() }
        if (selected.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "No cards selected to save.")
            return
        }

        _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val destination = state.saveDestination
                val targetDeckId: Long
                val targetDeckName: String

                if (destination == SaveDestination.NEW_DECK || state.selectedDeckId == 0L) {
                    val nameInput = state.newDeckName.trim()
                    val defaultName = if (state.inputMode == AiInputMode.IMAGE) {
                        "Visual Notes (${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())})"
                    } else {
                        state.topic.take(30).ifEmpty { "AI Generated Deck" }
                    }
                    val finalDeckName = nameInput.ifEmpty { defaultName }
                    val desc = state.newDeckDescription.trim().ifEmpty { "Created with AI Flashcard Studio" }

                    targetDeckId = repository.createDeck(finalDeckName, desc)
                    targetDeckName = finalDeckName
                } else {
                    targetDeckId = state.selectedDeckId
                    targetDeckName = state.decks.find { it.id == targetDeckId }?.name ?: "Current Deck"
                }

                for (card in selected) {
                    repository.createCard(targetDeckId, card.front.trim(), card.back.trim())
                }

                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    generatedCards = emptyList(),
                    successSavedCount = selected.size,
                    savedToDeckName = targetDeckName,
                    savedToDeckId = targetDeckId,
                    topic = "",
                    selectedImageUri = null,
                    selectedImageBase64 = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save flashcards: ${e.localizedMessage}"
                )
            }
        }
    }

    class Factory(
        private val initialDeckId: Long,
        private val repository: FlashcardRepository,
        private val aiService: AiService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiGeneratorViewModel(initialDeckId, repository, aiService) as T
        }
    }
}

