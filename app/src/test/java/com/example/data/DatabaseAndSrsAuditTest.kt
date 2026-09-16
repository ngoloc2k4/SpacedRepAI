package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.repository.FlashcardRepository
import com.example.domain.srs.CardState
import com.example.domain.srs.ReviewRating
import com.example.domain.srs.ReviewScheduler
import com.example.domain.srs.Sm2Algorithm
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseAndSrsAuditTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FlashcardRepository
    private val algorithm = Sm2Algorithm()
    private val scheduler = ReviewScheduler()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FlashcardRepository(database, algorithm, scheduler)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ==========================================
    // 1. DATABASE AUDIT: CRUD & CASCADE
    // ==========================================

    @Test
    fun testDeckCrudOperations() = runTest {
        // 1. Create
        val deckId = repository.createDeck("Algorithms", "Data structures & algorithms")
        assertTrue(deckId > 0)

        // Read
        val deck = repository.getDeck(deckId).first()
        assertNotNull(deck)
        assertEquals("Algorithms", deck?.name)
        assertEquals("Data structures & algorithms", deck?.description)

        // Update
        repository.updateDeck(deck!!.copy(name = "Advanced Algorithms"))
        val updated = repository.getDeck(deckId).first()
        assertEquals("Advanced Algorithms", updated?.name)

        // Delete
        repository.deleteDeck(deckId)
        val deleted = repository.getDeck(deckId).first()
        assertNull(deleted)
    }

    @Test
    fun testCardCrudOperations() = runTest {
        val deckId = repository.createDeck("Kotlin", "Kotlin basics")

        // 1. Create Card
        val cardId = repository.createCard(deckId, "What is val?", "Read-only reference")
        assertTrue(cardId > 0)

        // Read Card
        val card = repository.getCardById(cardId)
        assertNotNull(card)
        assertEquals("What is val?", card?.front)
        assertEquals("Read-only reference", card?.back)
        assertEquals(CardState.NEW, card?.state)
        assertEquals(0, card?.repetitions)

        // Update Card
        repository.updateCard(card!!.copy(back = "Immutable reference"))
        val updated = repository.getCardById(cardId)
        assertEquals("Immutable reference", updated?.back)

        // Delete Card
        repository.deleteCard(cardId)
        val deleted = repository.getCardById(cardId)
        assertNull(deleted)
    }

    @Test
    fun testCascadeDeletionWhenDeckDeleted() = runTest {
        val deckId = repository.createDeck("Physics", "Quantum mechanics")
        val cardId1 = repository.createCard(deckId, "Photon", "Particle of light")
        val cardId2 = repository.createCard(deckId, "Electron", "Elementary particle")

        // Also create a review log
        val card1 = repository.getCardById(cardId1)!!
        repository.reviewCard(card1, ReviewRating.GOOD)

        // Verify cards and logs exist
        assertEquals(2, repository.getCardsSnapshot(deckId).size)
        val logsBefore = repository.getReviewLogsForCard(cardId1).first()
        assertEquals(1, logsBefore.size)

        // Delete Deck
        repository.deleteDeck(deckId)

        // Verify cards for that deck are gone
        val cardsAfter = repository.getCardsSnapshot(deckId)
        assertTrue(cardsAfter.isEmpty())
        val card1After = repository.getCardById(cardId1)
        assertNull(card1After)
    }

    // ==========================================
    // 2. CARD LIFECYCLE AUDIT:
    // NEW -> LEARNING -> REVIEW -> RELEARNING -> REVIEW
    // ==========================================

    @Test
    fun testCompleteCardLifecycleStateTransitions() = runTest {
        val deckId = repository.createDeck("Languages", "Vocabulary")
        val cardId = repository.createCard(deckId, "Bonjour", "Hello")
        var card = repository.getCardById(cardId)!!

        // State 0: Initial state is NEW
        assertEquals(CardState.NEW, card.state)
        assertEquals(0, card.repetitions)
        assertEquals(0, card.intervalDays)

        val fixedTime = 1_700_000_000_000L

        // Transition 1: User rates HARD -> transitions to LEARNING
        card = repository.reviewCard(card, ReviewRating.HARD, timestamp = fixedTime)
        assertEquals(CardState.LEARNING, card.state)
        assertEquals(1, card.intervalDays)

        // Transition 2: User rates GOOD on learning card -> graduates to REVIEW
        card = repository.reviewCard(card, ReviewRating.GOOD, timestamp = fixedTime + 86_400_000L)
        assertEquals(CardState.REVIEW, card.state)
        assertEquals(1, card.repetitions)
        assertEquals(1, card.intervalDays)

        // Transition 3: User continues with GOOD in REVIEW -> interval grows (e.g. 6 days)
        card = repository.reviewCard(card, ReviewRating.GOOD, timestamp = fixedTime + 2 * 86_400_000L)
        assertEquals(CardState.REVIEW, card.state)
        assertEquals(2, card.repetitions)
        assertEquals(6, card.intervalDays)

        // Transition 4: User forgets card (AGAIN) -> drops into RELEARNING!
        val efBeforeLapse = card.easeFactor
        card = repository.reviewCard(card, ReviewRating.AGAIN, timestamp = fixedTime + 8 * 86_400_000L)
        assertEquals(CardState.RELEARNING, card.state)
        assertEquals(0, card.repetitions)
        assertEquals(0, card.intervalDays)
        assertTrue("Ease factor must decrease on AGAIN", card.easeFactor < efBeforeLapse)

        // Transition 5: In RELEARNING, user rates GOOD -> graduates back to REVIEW
        card = repository.reviewCard(card, ReviewRating.GOOD, timestamp = fixedTime + 8 * 86_400_000L + 600_000L)
        assertEquals(CardState.REVIEW, card.state)
        assertEquals(1, card.repetitions)
        assertEquals(1, card.intervalDays)
    }

    // ==========================================
    // 3. TIME & SCHEDULING AUDIT:
    // Due cards, UTC timestamp accuracy, multi-day roll
    // ==========================================

    @Test
    fun testDueSchedulingAndIntervalCalculations() = runTest {
        val baseTime = 1_700_000_000_000L
        val oneDay = Sm2Algorithm.ONE_DAY_MILLIS

        val deckId = repository.createDeck("History", "World history")
        val c1Id = repository.createCard(deckId, "1492", "Columbus reaches Americas")
        val c2Id = repository.createCard(deckId, "1969", "Moon landing")

        val card1 = repository.getCardById(c1Id)!!
        val card2 = repository.getCardById(c2Id)!!

        // Review card1 with GOOD: due in 1 day (baseTime + 1 day)
        val reviewed1 = repository.reviewCard(card1, ReviewRating.GOOD, timestamp = baseTime)
        assertEquals(baseTime + oneDay, reviewed1.nextReviewDate)

        // Review card2 with EASY: due in 4 days (baseTime + 4 days)
        val reviewed2 = repository.reviewCard(card2, ReviewRating.EASY, timestamp = baseTime)
        assertEquals(baseTime + 4 * oneDay, reviewed2.nextReviewDate)

        val allCards = repository.getCardsSnapshot(deckId)

        // At baseTime + 12 hours: neither is due yet
        val queueAt12h = scheduler.buildDailyQueue(allCards, currentTime = baseTime + 12 * 3600_000L)
        assertEquals(0, queueAt12h.dueCards.size)

        // At baseTime + 25 hours (Day 1 + 1h): card1 is due, card2 is NOT due
        val queueAtDay1 = scheduler.buildDailyQueue(allCards, currentTime = baseTime + 25 * 3600_000L)
        assertEquals(1, queueAtDay1.dueCards.size)
        assertEquals(reviewed1.id, queueAtDay1.dueCards[0].id)

        // At baseTime + 97 hours (Day 4 + 1h): BOTH card1 and card2 are due
        val queueAtDay4 = scheduler.buildDailyQueue(allCards, currentTime = baseTime + 97 * 3600_000L)
        assertEquals(2, queueAtDay4.dueCards.size)
    }

    // ==========================================
    // 4. ATOMIC TRANSACTION AUDIT:
    // Update Card + Insert ReviewLog
    // ==========================================

    @Test
    fun testReviewCardTransactionIntegrityAndLogConsistency() = runTest {
        val deckId = repository.createDeck("Chemistry", "Periodic table")
        val cardId = repository.createCard(deckId, "H", "Hydrogen")
        val initialCard = repository.getCardById(cardId)!!

        val reviewTimestamp = 1_700_500_000_000L
        val updatedCard = repository.reviewCard(initialCard, ReviewRating.EASY, timestamp = reviewTimestamp)

        // Check CardEntity updated in DB
        val persistedCard = repository.getCardById(cardId)!!
        assertEquals(updatedCard.state, persistedCard.state)
        assertEquals(updatedCard.repetitions, persistedCard.repetitions)
        assertEquals(updatedCard.intervalDays, persistedCard.intervalDays)
        assertEquals(updatedCard.easeFactor, persistedCard.easeFactor, 0.001)
        assertEquals(updatedCard.nextReviewDate, persistedCard.nextReviewDate)

        // Check ReviewLogEntity inserted in DB
        val logs = repository.getReviewLogsForCard(cardId).first()
        assertEquals(1, logs.size)
        val log = logs[0]
        assertEquals(cardId, log.cardId)
        assertEquals(ReviewRating.EASY, log.rating)
        assertEquals(reviewTimestamp, log.reviewedAt)
        assertEquals(initialCard.intervalDays, log.intervalBefore)
        assertEquals(updatedCard.intervalDays, log.intervalAfter)
        assertEquals(initialCard.easeFactor, log.easeFactorBefore, 0.001)
        assertEquals(updatedCard.easeFactor, log.easeFactorAfter, 0.001)
        assertEquals(initialCard.state, log.stateBefore)
        assertEquals(updatedCard.state, log.stateAfter)
    }
}
