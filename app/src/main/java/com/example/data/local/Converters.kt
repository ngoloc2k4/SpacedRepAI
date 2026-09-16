package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.srs.CardState
import com.example.domain.srs.ReviewRating

class Converters {
    @TypeConverter
    fun fromCardState(value: CardState): String = value.name

    @TypeConverter
    fun toCardState(value: String): CardState {
        return try {
            CardState.valueOf(value)
        } catch (e: Exception) {
            CardState.NEW
        }
    }

    @TypeConverter
    fun fromReviewRating(value: ReviewRating): String = value.name

    @TypeConverter
    fun toReviewRating(value: String): ReviewRating {
        return try {
            ReviewRating.valueOf(value)
        } catch (e: Exception) {
            ReviewRating.GOOD
        }
    }
}
