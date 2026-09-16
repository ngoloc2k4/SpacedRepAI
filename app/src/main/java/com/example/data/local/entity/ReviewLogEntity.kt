package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.srs.CardState
import com.example.domain.srs.ReviewRating

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cardId"]),
        Index(value = ["reviewedAt"])
    ]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val rating: ReviewRating,
    val reviewedAt: Long,
    val intervalBefore: Int,
    val intervalAfter: Int,
    val easeFactorBefore: Double,
    val easeFactorAfter: Double,
    val stateBefore: CardState,
    val stateAfter: CardState
)
