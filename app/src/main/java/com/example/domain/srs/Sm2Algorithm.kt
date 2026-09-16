package com.example.domain.srs

import kotlin.math.max
import kotlin.math.roundToInt

class Sm2Algorithm(
    private val defaultEaseFactor: Double = 2.5,
    private val minEaseFactor: Double = 1.3
) : SrsAlgorithm {

    companion object {
        const val ONE_DAY_MILLIS = 86_400_000L
        const val TEN_MINUTES_MILLIS = 10 * 60 * 1000L
    }

    override fun calculateNextReview(
        currentState: CardState,
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Double,
        rating: ReviewRating,
        reviewTimestampMillis: Long
    ): SrsCalculationResult {
        val safeEaseFactor = if (currentEaseFactor < minEaseFactor) defaultEaseFactor else currentEaseFactor

        return when (rating) {
            ReviewRating.AGAIN -> handleAgain(currentState, safeEaseFactor, reviewTimestampMillis)
            ReviewRating.HARD -> handleHard(currentState, currentRepetitions, currentIntervalDays, safeEaseFactor, reviewTimestampMillis)
            ReviewRating.GOOD -> handleGood(currentState, currentRepetitions, currentIntervalDays, safeEaseFactor, reviewTimestampMillis)
            ReviewRating.EASY -> handleEasy(currentState, currentRepetitions, currentIntervalDays, safeEaseFactor, reviewTimestampMillis)
        }
    }

    private fun handleAgain(
        currentState: CardState,
        currentEaseFactor: Double,
        timestamp: Long
    ): SrsCalculationResult {
        val nextState = if (currentState == CardState.REVIEW) CardState.RELEARNING else CardState.LEARNING
        val newEaseFactor = max(minEaseFactor, currentEaseFactor - 0.20)
        return SrsCalculationResult(
            repetitions = 0,
            intervalDays = 0,
            easeFactor = newEaseFactor,
            nextState = nextState,
            nextReviewDate = timestamp + TEN_MINUTES_MILLIS
        )
    }

    private fun handleHard(
        currentState: CardState,
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Double,
        timestamp: Long
    ): SrsCalculationResult {
        val newEaseFactor = max(minEaseFactor, currentEaseFactor - 0.15)
        return when (currentState) {
            CardState.NEW, CardState.LEARNING -> SrsCalculationResult(
                repetitions = 0,
                intervalDays = 1,
                easeFactor = newEaseFactor,
                nextState = CardState.LEARNING,
                nextReviewDate = timestamp + ONE_DAY_MILLIS
            )
            CardState.RELEARNING -> SrsCalculationResult(
                repetitions = 0,
                intervalDays = 1,
                easeFactor = newEaseFactor,
                nextState = CardState.LEARNING,
                nextReviewDate = timestamp + ONE_DAY_MILLIS
            )
            CardState.REVIEW -> {
                val nextInterval = max(1, (currentIntervalDays * 1.2).roundToInt())
                SrsCalculationResult(
                    repetitions = currentRepetitions + 1,
                    intervalDays = nextInterval,
                    easeFactor = newEaseFactor,
                    nextState = CardState.REVIEW,
                    nextReviewDate = timestamp + (nextInterval * ONE_DAY_MILLIS)
                )
            }
        }
    }

    private fun handleGood(
        currentState: CardState,
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Double,
        timestamp: Long
    ): SrsCalculationResult {
        return when (currentState) {
            CardState.NEW, CardState.LEARNING, CardState.RELEARNING -> SrsCalculationResult(
                repetitions = 1,
                intervalDays = 1,
                easeFactor = currentEaseFactor,
                nextState = CardState.REVIEW,
                nextReviewDate = timestamp + ONE_DAY_MILLIS
            )
            CardState.REVIEW -> {
                val nextReps = currentRepetitions + 1
                val nextInterval = when (currentRepetitions) {
                    0 -> 1
                    1 -> 6
                    else -> max(1, (currentIntervalDays * currentEaseFactor).roundToInt())
                }
                SrsCalculationResult(
                    repetitions = nextReps,
                    intervalDays = nextInterval,
                    easeFactor = currentEaseFactor,
                    nextState = CardState.REVIEW,
                    nextReviewDate = timestamp + (nextInterval * ONE_DAY_MILLIS)
                )
            }
        }
    }

    private fun handleEasy(
        currentState: CardState,
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Double,
        timestamp: Long
    ): SrsCalculationResult {
        val newEaseFactor = currentEaseFactor + 0.15
        return when (currentState) {
            CardState.NEW, CardState.LEARNING -> SrsCalculationResult(
                repetitions = 1,
                intervalDays = 4,
                easeFactor = newEaseFactor,
                nextState = CardState.REVIEW,
                nextReviewDate = timestamp + (4 * ONE_DAY_MILLIS)
            )
            CardState.RELEARNING -> SrsCalculationResult(
                repetitions = 1,
                intervalDays = 2,
                easeFactor = newEaseFactor,
                nextState = CardState.REVIEW,
                nextReviewDate = timestamp + (2 * ONE_DAY_MILLIS)
            )
            CardState.REVIEW -> {
                val nextReps = currentRepetitions + 1
                val nextInterval = when (currentRepetitions) {
                    0 -> 2
                    1 -> 7
                    else -> max(1, (currentIntervalDays * currentEaseFactor * 1.3).roundToInt())
                }
                SrsCalculationResult(
                    repetitions = nextReps,
                    intervalDays = nextInterval,
                    easeFactor = newEaseFactor,
                    nextState = CardState.REVIEW,
                    nextReviewDate = timestamp + (nextInterval * ONE_DAY_MILLIS)
                )
            }
        }
    }
}
