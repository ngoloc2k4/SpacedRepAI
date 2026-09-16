package com.example.domain.srs

import com.example.data.local.entity.CardEntity

data class ReviewSession(
    val deckId: Long,
    val deckName: String,
    val queue: List<CardEntity>,
    val currentIndex: Int = 0,
    val reviewedCount: Int = 0,
    val againCount: Int = 0,
    val hardCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val endTimeMillis: Long? = null
) {
    val currentCard: CardEntity?
        get() = if (currentIndex in queue.indices) queue[currentIndex] else null

    val totalQueueSize: Int
        get() = queue.size

    val isCompleted: Boolean
        get() = queue.isEmpty() || currentIndex >= queue.size

    val progress: Float
        get() = if (queue.isEmpty()) 0f else (currentIndex.toFloat() / queue.size.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    val correctCount: Int
        get() = hardCount + goodCount + easyCount

    val totalDurationMillis: Long
        get() = ((endTimeMillis ?: System.currentTimeMillis()) - startTimeMillis).coerceAtLeast(0)

    fun formatDuration(): String {
        val totalSeconds = (totalDurationMillis / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    }

    /**
     * Immutable state transition when a rating is applied to current card.
     * If rating is AGAIN, re-appends the card to the end of queue for same-session retention.
     */
    fun applyRating(rating: ReviewRating): ReviewSession {
        val card = currentCard ?: return this
        val newQueue = queue.toMutableList()

        if (rating == ReviewRating.AGAIN) {
            newQueue.add(card)
        }

        val nextIndex = currentIndex + 1
        val isFinished = nextIndex >= newQueue.size
        val end = if (isFinished) System.currentTimeMillis() else null

        return copy(
            queue = newQueue,
            currentIndex = nextIndex,
            reviewedCount = reviewedCount + 1,
            againCount = if (rating == ReviewRating.AGAIN) againCount + 1 else againCount,
            hardCount = if (rating == ReviewRating.HARD) hardCount + 1 else hardCount,
            goodCount = if (rating == ReviewRating.GOOD) goodCount + 1 else goodCount,
            easyCount = if (rating == ReviewRating.EASY) easyCount + 1 else easyCount,
            endTimeMillis = end
        )
    }
}
