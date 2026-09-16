package com.example.domain.ai

import com.example.domain.srs.ReviewRating

data class GeneratedCard(
    val front: String,
    val back: String
)

enum class EvaluationStatus {
    CORRECT,
    PARTIALLY_CORRECT,
    INCORRECT
}

data class AnswerEvaluation(
    val status: EvaluationStatus,
    val feedback: String,
    val suggestedRating: ReviewRating? = null
)

interface AiService {
    suspend fun generateCards(
        topic: String,
        count: Int = 5,
        language: String = "auto"
    ): Result<List<GeneratedCard>>

    suspend fun generateCardsFromImage(
        imageBase64: String,
        mimeType: String = "image/jpeg",
        count: Int = 5,
        promptHint: String = ""
    ): Result<List<GeneratedCard>>

    suspend fun explainCard(
        front: String,
        back: String
    ): Result<String>

    suspend fun generateMnemonic(
        front: String,
        back: String
    ): Result<String>

    suspend fun evaluateAnswer(
        front: String,
        expectedBack: String,
        userAnswer: String
    ): Result<AnswerEvaluation>

    suspend fun testConnection(): Result<String>
}
