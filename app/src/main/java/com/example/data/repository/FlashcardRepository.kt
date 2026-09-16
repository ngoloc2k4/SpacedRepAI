package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.local.entity.ReviewLogEntity
import com.example.domain.srs.ReviewRating
import com.example.domain.srs.ReviewScheduler
import com.example.domain.srs.SrsAlgorithm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FlashcardRepository(
    private val database: AppDatabase,
    private val srsAlgorithm: SrsAlgorithm,
    private val scheduler: ReviewScheduler = ReviewScheduler()
) {
    private val deckDao get() = database.deckDao()
    private val cardDao get() = database.cardDao()
    private val reviewLogDao get() = database.reviewLogDao()

    // --- Decks ---
    val allDecks: Flow<List<DeckEntity>> = deckDao.getAllDecks()

    fun getDeck(deckId: Long): Flow<DeckEntity?> = deckDao.getDeckById(deckId)

    suspend fun createDeck(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        deckDao.insertDeck(DeckEntity(name = name.trim(), description = description.trim()))
    }

    suspend fun updateDeck(deck: DeckEntity) = withContext(Dispatchers.IO) {
        deckDao.updateDeck(deck)
    }

    suspend fun deleteDeck(deckId: Long) = withContext(Dispatchers.IO) {
        deckDao.deleteDeckById(deckId)
    }

    // --- Cards ---
    val allCards: Flow<List<CardEntity>> = cardDao.getAllCards()

    fun getCardsForDeck(deckId: Long): Flow<List<CardEntity>> = cardDao.getCardsForDeck(deckId)

    fun getCardsForDeckPaged(deckId: Long, limit: Int, offset: Int): Flow<List<CardEntity>> =
        cardDao.getCardsForDeckPaged(deckId, limit, offset)

    suspend fun getCardsForDeckPagedSnapshot(deckId: Long, limit: Int, offset: Int): List<CardEntity> =
        withContext(Dispatchers.IO) {
            cardDao.getCardsForDeckPagedSnapshot(deckId, limit, offset)
        }

    suspend fun searchCardsPaged(deckId: Long, query: String, limit: Int, offset: Int): List<CardEntity> =
        withContext(Dispatchers.IO) {
            cardDao.searchCardsPaged(deckId, query, limit, offset)
        }

    fun getCardCountForDeck(deckId: Long): Flow<Int> = cardDao.getCardCountForDeck(deckId)

    suspend fun getCardById(cardId: Long): CardEntity? = withContext(Dispatchers.IO) {
        cardDao.getCardByIdSnapshot(cardId)
    }

    suspend fun createCard(deckId: Long, front: String, back: String): Long = withContext(Dispatchers.IO) {
        cardDao.insertCard(
            CardEntity(
                deckId = deckId,
                front = front.trim(),
                back = back.trim()
            )
        )
    }

    suspend fun createCards(cards: List<CardEntity>): List<Long> = withContext(Dispatchers.IO) {
        cardDao.insertCards(cards)
    }

    suspend fun importDeckWithCards(deckName: String, deckDesc: String, cards: List<CardEntity>): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val newDeckId = deckDao.insertDeck(DeckEntity(name = deckName.trim(), description = deckDesc.trim()))
            val cardsWithDeckId = cards.map { it.copy(id = 0, deckId = newDeckId) }
            cardDao.insertCards(cardsWithDeckId)
            newDeckId
        }
    }

    suspend fun updateCard(card: CardEntity) = withContext(Dispatchers.IO) {
        cardDao.updateCard(card)
    }

    suspend fun deleteCard(cardId: Long) = withContext(Dispatchers.IO) {
        cardDao.deleteCardById(cardId)
    }

    // --- Review Execution ---
    suspend fun getCardsSnapshot(deckId: Long): List<CardEntity> = withContext(Dispatchers.IO) {
        cardDao.getCardsForDeckSnapshot(deckId)
    }

    suspend fun getAllCardsSnapshot(): List<CardEntity> = withContext(Dispatchers.IO) {
        cardDao.getAllCardsSnapshot()
    }

    fun getReviewLogsForCard(cardId: Long): Flow<List<ReviewLogEntity>> = reviewLogDao.getLogsForCard(cardId)

    val allReviewLogs: Flow<List<ReviewLogEntity>> = reviewLogDao.getAllLogs()

    /**
     * Executes SRS review computation inside a database transaction:
     * 1. Calculates next interval, ease factor, and state using SrsAlgorithm
     * 2. Writes full ReviewLogEntity
     * 3. Updates CardEntity in Room
     * Both succeed or both rollback together.
     */
    suspend fun reviewCard(
        card: CardEntity,
        rating: ReviewRating,
        timestamp: Long = System.currentTimeMillis()
    ): CardEntity = withContext(Dispatchers.IO) {
        val calc = srsAlgorithm.calculateNextReview(
            currentState = card.state,
            currentRepetitions = card.repetitions,
            currentIntervalDays = card.intervalDays,
            currentEaseFactor = card.easeFactor,
            rating = rating,
            reviewTimestampMillis = timestamp
        )

        val updatedCard = card.copy(
            state = calc.nextState,
            repetitions = calc.repetitions,
            intervalDays = calc.intervalDays,
            easeFactor = calc.easeFactor,
            nextReviewDate = calc.nextReviewDate
        )

        val log = ReviewLogEntity(
            cardId = card.id,
            rating = rating,
            reviewedAt = timestamp,
            intervalBefore = card.intervalDays,
            intervalAfter = calc.intervalDays,
            easeFactorBefore = card.easeFactor,
            easeFactorAfter = calc.easeFactor,
            stateBefore = card.state,
            stateAfter = calc.nextState
        )

        database.withTransaction {
            reviewLogDao.insertLog(log)
            cardDao.updateCard(updatedCard)
        }

        updatedCard
    }

    fun getScheduler(): ReviewScheduler = scheduler

    fun getAlgorithm(): SrsAlgorithm = srsAlgorithm
}
