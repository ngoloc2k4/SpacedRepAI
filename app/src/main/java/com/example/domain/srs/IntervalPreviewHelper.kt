package com.example.domain.srs

import com.example.data.local.entity.CardEntity

data class IntervalPreview(
    val rating: ReviewRating,
    val label: String,
    val intervalText: String,
    val nextIntervalDays: Int,
    val nextState: CardState
)

object IntervalPreviewHelper {

    fun getPreviewsForCard(
        card: CardEntity,
        algorithm: SrsAlgorithm,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Map<ReviewRating, IntervalPreview> {
        return ReviewRating.entries.associateWith { rating ->
            val result = algorithm.calculateNextReview(
                currentState = card.state,
                currentRepetitions = card.repetitions,
                currentIntervalDays = card.intervalDays,
                currentEaseFactor = card.easeFactor,
                rating = rating,
                reviewTimestampMillis = currentTimeMillis
            )

            val text = when {
                rating == ReviewRating.AGAIN || result.intervalDays == 0 -> "10m"
                result.intervalDays == 1 -> "1d"
                result.intervalDays in 2..29 -> "${result.intervalDays}d"
                result.intervalDays in 30..364 -> {
                    val months = (result.intervalDays / 30.0).let { if (it < 1.5) "1mo" else "${it.toInt()}mo" }
                    months
                }
                else -> {
                    val years = result.intervalDays / 365
                    "${years}y"
                }
            }

            IntervalPreview(
                rating = rating,
                label = rating.name.lowercase().replaceFirstChar { it.uppercase() },
                intervalText = text,
                nextIntervalDays = result.intervalDays,
                nextState = result.nextState
            )
        }
    }
}
