package com.example.domain.srs

/**
 * Abstraction for Spaced Repetition algorithms (e.g. SM-2, FSRS, Leitner).
 */
interface SrsAlgorithm {
    fun calculateNextReview(
        currentState: CardState,
        currentRepetitions: Int,
        currentIntervalDays: Int,
        currentEaseFactor: Double,
        rating: ReviewRating,
        reviewTimestampMillis: Long
    ): SrsCalculationResult
}
