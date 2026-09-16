package com.example.domain.stats

import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.ReviewLogEntity
import com.example.domain.srs.CardState
import com.example.domain.srs.ReviewRating
import java.util.Calendar

data class StatsData(
    // Overview
    val totalCards: Int = 0,
    val totalDecks: Int = 0,
    val reviewsToday: Int = 0,
    val reviewsThisWeek: Int = 0,
    val totalStudyTimeFormatted: String = "0m",
    val currentStreakDays: Int = 0,

    // SRS Metrics
    val retentionRate: Float = 0f, // (Good + Easy + Hard) / Total Reviews
    val averageIntervalDays: Double = 0.0,
    val cardsDueNow: Int = 0,
    val matureCardsCount: Int = 0, // Interval >= 21 days
    val youngCardsCount: Int = 0,  // Interval in 1..20 days

    // Rating Breakdown
    val againCount: Int = 0,
    val hardCount: Int = 0,
    val goodCount: Int = 0,
    val easyCount: Int = 0,
    val againPercent: Float = 0f,
    val hardPercent: Float = 0f,
    val goodPercent: Float = 0f,
    val easyPercent: Float = 0f,

    // Card States
    val newCount: Int = 0,
    val learningCount: Int = 0,
    val reviewCount: Int = 0,
    val relearningCount: Int = 0,

    // Upcoming Forecast (Days from now)
    val forecastTomorrow: Int = 0,
    val forecastIn3Days: Int = 0,
    val forecastIn7Days: Int = 0,
    val forecastIn30Days: Int = 0
)

object StatsCalculator {

    private const val ONE_DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun calculate(
        cards: List<CardEntity>,
        logs: List<ReviewLogEntity>,
        decksCount: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): StatsData {
        if (cards.isEmpty() && logs.isEmpty()) {
            return StatsData(totalDecks = decksCount)
        }

        // 1. Time boundaries (start of today, start of week)
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        // Start of week (Monday)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        val startOfWeek = startOfToday - daysFromMonday * ONE_DAY_MILLIS

        // 2. Overview from logs
        val reviewsToday = logs.count { it.reviewedAt >= startOfToday }
        val reviewsThisWeek = logs.count { it.reviewedAt >= startOfWeek }

        // Approx 8 seconds per flashcard review
        val estimatedTotalSeconds = logs.size * 8
        val studyMinutes = estimatedTotalSeconds / 60
        val studyHours = studyMinutes / 60
        val studyTimeFormatted = if (studyHours > 0) {
            "${studyHours}h ${studyMinutes % 60}m"
        } else {
            "${studyMinutes}m"
        }

        // Streak calculation (consecutive days with at least 1 review)
        val streak = calculateStreak(logs, currentTimeMillis)

        // 3. Rating breakdown
        val totalReviews = logs.size
        var again = 0
        var hard = 0
        var good = 0
        var easy = 0

        for (log in logs) {
            when (log.rating) {
                ReviewRating.AGAIN -> again++
                ReviewRating.HARD -> hard++
                ReviewRating.GOOD -> good++
                ReviewRating.EASY -> easy++
            }
        }

        val againPct = if (totalReviews > 0) (again.toFloat() / totalReviews) * 100f else 0f
        val hardPct = if (totalReviews > 0) (hard.toFloat() / totalReviews) * 100f else 0f
        val goodPct = if (totalReviews > 0) (good.toFloat() / totalReviews) * 100f else 0f
        val easyPct = if (totalReviews > 0) (easy.toFloat() / totalReviews) * 100f else 0f

        val retentionRate = if (totalReviews > 0) {
            ((hard + good + easy).toFloat() / totalReviews) * 100f
        } else 0f

        // 4. Card state and intervals
        var newCards = 0
        var learningCards = 0
        var reviewCards = 0
        var relearningCards = 0
        var matureCards = 0
        var youngCards = 0
        var totalIntervalDays = 0L
        var reviewCardCount = 0
        var dueNow = 0

        // Forecast counters
        val tomorrowThreshold = currentTimeMillis + ONE_DAY_MILLIS
        val in3DaysThreshold = currentTimeMillis + 3 * ONE_DAY_MILLIS
        val in7DaysThreshold = currentTimeMillis + 7 * ONE_DAY_MILLIS
        val in30DaysThreshold = currentTimeMillis + 30 * ONE_DAY_MILLIS

        var dueTomorrow = 0
        var dueIn3 = 0
        var dueIn7 = 0
        var dueIn30 = 0

        for (card in cards) {
            when (card.state) {
                CardState.NEW -> newCards++
                CardState.LEARNING -> learningCards++
                CardState.REVIEW -> {
                    reviewCards++
                    totalIntervalDays += card.intervalDays
                    reviewCardCount++
                    if (card.intervalDays >= 21) {
                        matureCards++
                    } else if (card.intervalDays > 0) {
                        youngCards++
                    }
                }
                CardState.RELEARNING -> relearningCards++
            }

            if (card.nextReviewDate <= currentTimeMillis) {
                dueNow++
            }

            // Forecast check
            val date = card.nextReviewDate
            if (date in (currentTimeMillis + 1)..tomorrowThreshold) dueTomorrow++
            if (date in (currentTimeMillis + 1)..in3DaysThreshold) dueIn3++
            if (date in (currentTimeMillis + 1)..in7DaysThreshold) dueIn7++
            if (date in (currentTimeMillis + 1)..in30DaysThreshold) dueIn30++
            val reviewDate = card.nextReviewDate
            if (reviewDate in (currentTimeMillis + 1)..tomorrowThreshold) dueTomorrow++
            if (reviewDate in (currentTimeMillis + 1)..in3DaysThreshold) dueIn3++
            if (reviewDate in (currentTimeMillis + 1)..in7DaysThreshold) dueIn7++
            if (reviewDate in (currentTimeMillis + 1)..in30DaysThreshold) dueIn30++
        }

        val avgInterval = if (reviewCardCount > 0) {
            totalIntervalDays.toDouble() / reviewCardCount
        } else 0.0

        return StatsData(
            totalCards = cards.size,
            totalDecks = decksCount,
            reviewsToday = reviewsToday,
            reviewsThisWeek = reviewsThisWeek,
            totalStudyTimeFormatted = studyTimeFormatted,
            currentStreakDays = streak,
            retentionRate = retentionRate,
            averageIntervalDays = avgInterval,
            cardsDueNow = dueNow,
            matureCardsCount = matureCards,
            youngCardsCount = youngCards,
            againCount = again,
            hardCount = hard,
            goodCount = good,
            easyCount = easy,
            againPercent = againPct,
            hardPercent = hardPct,
            goodPercent = goodPct,
            easyPercent = easyPct,
            newCount = newCards,
            learningCount = learningCards,
            reviewCount = reviewCards,
            relearningCount = relearningCards,
            forecastTomorrow = dueTomorrow,
            forecastIn3Days = dueIn3,
            forecastIn7Days = dueIn7,
            forecastIn30Days = dueIn30
        )
    }

    private fun calculateStreak(logs: List<ReviewLogEntity>, currentTimeMillis: Long): Int {
        if (logs.isEmpty()) return 0

        val cal = Calendar.getInstance()
        val reviewDays = logs.map { log ->
            cal.timeInMillis = log.reviewedAt
            val year = cal.get(Calendar.YEAR)
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            year * 1000 + dayOfYear
        }.toSet()

        cal.timeInMillis = currentTimeMillis
        val currentYear = cal.get(Calendar.YEAR)
        val currentDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        var checkKey = currentYear * 1000 + currentDayOfYear

        var streak = 0
        // If today has reviews, start from today. Otherwise, check if yesterday had reviews to maintain streak.
        if (reviewDays.contains(checkKey)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        } else {
            // Check yesterday
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
            if (!reviewDays.contains(checkKey)) {
                return 0
            }
        }

        // Count consecutive days backward
        while (reviewDays.contains(checkKey)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
            checkKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        }

        return streak
    }
}
