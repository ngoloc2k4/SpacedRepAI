package com.example.domain.srs

data class SrsCalculationResult(
    val repetitions: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val nextState: CardState,
    val nextReviewDate: Long
)
