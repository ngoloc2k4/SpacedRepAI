package com.example.domain.srs

import com.example.data.local.entity.CardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReviewSchedulerTest {

    private lateinit var scheduler: ReviewScheduler
    private val now = 1_000_000L

    @Before
    fun setup() {
        scheduler = ReviewScheduler(defaultMaxNewCards = 2, defaultMaxReviewCards = 2)
    }

    @Test
    fun testBuildDailyQueue_classifiesAndLimitsCards() {
        val cards = listOf(
            // Due card (due in the past)
            CardEntity(id = 1, deckId = 1, front = "Q1", back = "A1", state = CardState.REVIEW, nextReviewDate = now - 100),
            // Due card (not due yet)
            CardEntity(id = 2, deckId = 1, front = "Q2", back = "A2", state = CardState.REVIEW, nextReviewDate = now + 5000),
            // Learning card (due)
            CardEntity(id = 3, deckId = 1, front = "Q3", back = "A3", state = CardState.LEARNING, nextReviewDate = now - 50),
            // New cards (3 items, limit is 2)
            CardEntity(id = 4, deckId = 1, front = "N1", back = "A4", state = CardState.NEW),
            CardEntity(id = 5, deckId = 1, front = "N2", back = "A5", state = CardState.NEW),
            CardEntity(id = 6, deckId = 1, front = "N3", back = "A6", state = CardState.NEW)
        )

        val queue = scheduler.buildDailyQueue(cards, currentTime = now)

        assertEquals(1, queue.learningCards.size)
        assertEquals(3L, queue.learningCards.first().id)

        assertEquals(1, queue.dueCards.size)
        assertEquals(1L, queue.dueCards.first().id)

        assertEquals(2, queue.newCards.size)
        assertEquals(4L, queue.newCards[0].id)
        assertEquals(5L, queue.newCards[1].id)

        assertEquals(4, queue.totalCount)

        // Study list priority
        val studyList = queue.toStudyList()
        assertEquals(3L, studyList[0].id) // Learning first
        assertEquals(1L, studyList[1].id) // Due second
        assertEquals(4L, studyList[2].id) // New third
    }
}
