package com.example.domain.srs

import com.example.data.local.entity.CardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewSessionTest {

    private fun createCard(id: Long, front: String, back: String): CardEntity {
        return CardEntity(
            id = id,
            deckId = 1L,
            front = front,
            back = back,
            state = CardState.NEW,
            repetitions = 0,
            intervalDays = 0,
            easeFactor = 2.5
        )
    }

    @Test
    fun testReviewSessionQueueAndTransitions() {
        val c1 = createCard(1L, "Q1", "A1")
        val c2 = createCard(2L, "Q2", "A2")

        val session = ReviewSession(
            deckId = 1L,
            deckName = "Test Deck",
            queue = listOf(c1, c2),
            startTimeMillis = 1000L
        )

        assertEquals(2, session.totalQueueSize)
        assertEquals(0, session.currentIndex)
        assertEquals(c1, session.currentCard)
        assertFalse(session.isCompleted)

        // Rate first card GOOD -> advances to card 2
        val s1 = session.applyRating(ReviewRating.GOOD)
        assertEquals(1, s1.currentIndex)
        assertEquals(c2, s1.currentCard)
        assertEquals(1, s1.goodCount)
        assertEquals(1, s1.reviewedCount)
        assertFalse(s1.isCompleted)

        // Rate second card AGAIN -> re-queues c2 at the end!
        val s2 = s1.applyRating(ReviewRating.AGAIN)
        assertEquals(2, s2.currentIndex)
        assertEquals(c2, s2.currentCard) // c2 is now reappearing!
        assertEquals(3, s2.totalQueueSize)
        assertEquals(1, s2.againCount)
        assertEquals(2, s2.reviewedCount)
        assertFalse(s2.isCompleted)

        // Rate the re-queued c2 EASY -> completes session
        val s3 = s2.applyRating(ReviewRating.EASY)
        assertEquals(3, s3.currentIndex)
        assertNull(s3.currentCard)
        assertTrue(s3.isCompleted)
        assertEquals(3, s3.reviewedCount)
        assertEquals(1, s3.againCount)
        assertEquals(1, s3.goodCount)
        assertEquals(1, s3.easyCount)
        assertEquals(2, s3.correctCount)
        assertNotNull(s3.endTimeMillis)
    }

    @Test
    fun testDurationFormatting() {
        val session = ReviewSession(
            deckId = 1L,
            deckName = "Test Deck",
            queue = emptyList(),
            startTimeMillis = 10_000L,
            endTimeMillis = 10_000L + 125_000L // 2 minutes 5 seconds
        )

        assertEquals("2m 5s", session.formatDuration())
    }

    @Test
    fun testIntervalPreviewFormatting() {
        val algorithm = Sm2Algorithm()
        val newCard = createCard(10L, "New", "Answer")

        val previews = IntervalPreviewHelper.getPreviewsForCard(newCard, algorithm, currentTimeMillis = 1_000_000L)

        assertEquals("10m", previews[ReviewRating.AGAIN]?.intervalText)
        assertEquals("1d", previews[ReviewRating.HARD]?.intervalText)
        assertEquals("1d", previews[ReviewRating.GOOD]?.intervalText)
        assertEquals("4d", previews[ReviewRating.EASY]?.intervalText)
    }
}
