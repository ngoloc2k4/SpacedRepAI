package com.example.domain.srs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Sm2AlgorithmTest {

    private lateinit var srs: Sm2Algorithm
    private val baseTimestamp = 1_700_000_000_000L

    @Before
    fun setup() {
        srs = Sm2Algorithm()
    }

    @Test
    fun testAgain_resetsRepetitionsAndMovesToLearning() {
        // Given a card in REVIEW state with previous repetitions
        val result = srs.calculateNextReview(
            currentState = CardState.REVIEW,
            currentRepetitions = 3,
            currentIntervalDays = 15,
            currentEaseFactor = 2.5,
            rating = ReviewRating.AGAIN,
            reviewTimestampMillis = baseTimestamp
        )

        assertEquals(0, result.repetitions)
        assertEquals(0, result.intervalDays)
        assertEquals(CardState.RELEARNING, result.nextState)
        assertEquals(2.3, result.easeFactor, 0.01)
        assertEquals(baseTimestamp + Sm2Algorithm.TEN_MINUTES_MILLIS, result.nextReviewDate)
    }

    @Test
    fun testHard_inReview_scalesIntervalModestly() {
        val result = srs.calculateNextReview(
            currentState = CardState.REVIEW,
            currentRepetitions = 2,
            currentIntervalDays = 10,
            currentEaseFactor = 2.5,
            rating = ReviewRating.HARD,
            reviewTimestampMillis = baseTimestamp
        )

        // 10 * 1.2 = 12 days
        assertEquals(3, result.repetitions)
        assertEquals(12, result.intervalDays)
        assertEquals(CardState.REVIEW, result.nextState)
        assertEquals(2.35, result.easeFactor, 0.01)
        assertEquals(baseTimestamp + (12 * Sm2Algorithm.ONE_DAY_MILLIS), result.nextReviewDate)
    }

    @Test
    fun testGood_transitionsNewCardToReview() {
        val result = srs.calculateNextReview(
            currentState = CardState.NEW,
            currentRepetitions = 0,
            currentIntervalDays = 0,
            currentEaseFactor = 2.5,
            rating = ReviewRating.GOOD,
            reviewTimestampMillis = baseTimestamp
        )

        assertEquals(1, result.repetitions)
        assertEquals(1, result.intervalDays)
        assertEquals(CardState.REVIEW, result.nextState)
        assertEquals(2.5, result.easeFactor, 0.01)
        assertEquals(baseTimestamp + Sm2Algorithm.ONE_DAY_MILLIS, result.nextReviewDate)
    }

    @Test
    fun testGood_subsequentReviewIntervals() {
        // First review in REVIEW state (rep 1 -> 6 days)
        val result1 = srs.calculateNextReview(
            currentState = CardState.REVIEW,
            currentRepetitions = 1,
            currentIntervalDays = 1,
            currentEaseFactor = 2.5,
            rating = ReviewRating.GOOD,
            reviewTimestampMillis = baseTimestamp
        )
        assertEquals(2, result1.repetitions)
        assertEquals(6, result1.intervalDays)

        // Second review in REVIEW state (rep 2 -> 6 * 2.5 = 15 days)
        val result2 = srs.calculateNextReview(
            currentState = CardState.REVIEW,
            currentRepetitions = 2,
            currentIntervalDays = 6,
            currentEaseFactor = 2.5,
            rating = ReviewRating.GOOD,
            reviewTimestampMillis = baseTimestamp
        )
        assertEquals(3, result2.repetitions)
        assertEquals(15, result2.intervalDays)
    }

    @Test
    fun testEasy_newCard_jumpsToFourDaysAndIncreasesEF() {
        val result = srs.calculateNextReview(
            currentState = CardState.NEW,
            currentRepetitions = 0,
            currentIntervalDays = 0,
            currentEaseFactor = 2.5,
            rating = ReviewRating.EASY,
            reviewTimestampMillis = baseTimestamp
        )

        assertEquals(1, result.repetitions)
        assertEquals(4, result.intervalDays)
        assertEquals(CardState.REVIEW, result.nextState)
        assertEquals(2.65, result.easeFactor, 0.01)
        assertEquals(baseTimestamp + (4 * Sm2Algorithm.ONE_DAY_MILLIS), result.nextReviewDate)
    }

    @Test
    fun testEaseFactor_doesNotDropBelowMinimum() {
        val result = srs.calculateNextReview(
            currentState = CardState.LEARNING,
            currentRepetitions = 0,
            currentIntervalDays = 0,
            currentEaseFactor = 1.35,
            rating = ReviewRating.AGAIN,
            reviewTimestampMillis = baseTimestamp
        )

        // 1.35 - 0.20 would be 1.15, but floor is 1.30
        assertEquals(1.30, result.easeFactor, 0.01)
    }
}
