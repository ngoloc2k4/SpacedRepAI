package com.example.domain.srs

import com.example.data.local.entity.CardEntity

data class DailyQueue(
    val learningCards: List<CardEntity>,
    val dueCards: List<CardEntity>,
    val newCards: List<CardEntity>
) {
    val totalCount: Int
        get() = learningCards.size + dueCards.size + newCards.size

    val isEmpty: Boolean
        get() = totalCount == 0

    /**
     * Flattens into an ordered study list:
     * 1. Learning cards (urgent / in progress)
     * 2. Due cards (scheduled for retention)
     * 3. New cards (fresh knowledge)
     */
    fun toStudyList(): List<CardEntity> {
        return learningCards + dueCards + newCards
    }
}

class ReviewScheduler(
    val defaultMaxNewCards: Int = 20,
    val defaultMaxReviewCards: Int = 100
) {

    fun getLearningCards(
        cards: List<CardEntity>,
        currentTime: Long = System.currentTimeMillis()
    ): List<CardEntity> {
        return cards
            .filter { it.state == CardState.LEARNING && it.nextReviewDate <= currentTime }
            .sortedBy { it.nextReviewDate }
    }

    fun getDueCards(
        cards: List<CardEntity>,
        currentTime: Long = System.currentTimeMillis()
    ): List<CardEntity> {
        return cards
            .filter { (it.state == CardState.REVIEW || it.state == CardState.RELEARNING) && it.nextReviewDate <= currentTime }
            .sortedBy { it.nextReviewDate }
    }

    fun getNewCards(cards: List<CardEntity>): List<CardEntity> {
        return cards
            .filter { it.state == CardState.NEW }
            .sortedBy { it.id }
    }

    fun buildDailyQueue(
        cards: List<CardEntity>,
        maxNewCards: Int = defaultMaxNewCards,
        maxReviewCards: Int = defaultMaxReviewCards,
        currentTime: Long = System.currentTimeMillis()
    ): DailyQueue {
        val learning = getLearningCards(cards, currentTime)
        val due = getDueCards(cards, currentTime).take(maxReviewCards)
        val newCards = getNewCards(cards).take(maxNewCards)

        return DailyQueue(
            learningCards = learning,
            dueCards = due,
            newCards = newCards
        )
    }
}
