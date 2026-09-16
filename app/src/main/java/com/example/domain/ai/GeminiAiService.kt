package com.example.domain.ai

import com.example.BuildConfig
import com.example.domain.srs.ReviewRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiAiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : AiService {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
    }

    private fun isKeyConfigured(key: String): Boolean {
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("placeholder")
    }

    override suspend fun generateCards(
        topic: String,
        count: Int,
        language: String
    ): Result<List<GeneratedCard>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyConfigured(apiKey)) {
            // Provide helpful fallback sample cards for instant UX preview
            return@withContext Result.success(getFallbackGeneratedCards(topic, count))
        }

        val prompt = """
            You are an expert educator and flashcard creator.
            Create exactly $count high-quality flashcards on the topic: "$topic".
            Each flashcard must have a concise, clear 'front' (question, concept, or prompt) and a 'back' (accurate answer or explanation).
            Return ONLY a valid JSON array of objects with keys "front" and "back". Do not include markdown code block markers or additional text.
            Example format:
            [
              {"front": "Question 1", "back": "Answer 1"},
              {"front": "Question 2", "back": "Answer 2"}
            ]
        """.trimIndent()

        try {
            val responseText = executeGeminiRequest(apiKey, prompt)
            val jsonString = cleanJsonOutput(responseText)
            val jsonArray = JSONArray(jsonString)
            val cards = mutableListOf<GeneratedCard>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val front = obj.optString("front", "").trim()
                val back = obj.optString("back", "").trim()
                if (front.isNotEmpty() && back.isNotEmpty()) {
                    cards.add(GeneratedCard(front = front, back = back))
                }
            }
            if (cards.isEmpty()) {
                Result.failure(Exception("Gemini returned empty flashcards"))
            } else {
                Result.success(cards)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun explainCard(
        front: String,
        back: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyConfigured(apiKey)) {
            return@withContext Result.success(
                "💡 Concept Breakdown:\n• Question/Prompt: $front\n• Answer: $back\n\n(To connect live Gemini AI explanations, configure GEMINI_API_KEY in the AI Studio Secrets panel)."
            )
        }

        val prompt = """
            You are a master tutor. Explain this flashcard in a concise, clear, and educational way:
            Front: "$front"
            Back: "$back"

            Provide:
            1. Why this answer is correct
            2. Real-world context, nuance, or practical example
            3. Common mistakes to avoid
            Keep it structured, under 180 words, formatted with bullet points.
        """.trimIndent()

        try {
            val responseText = executeGeminiRequest(apiKey, prompt)
            Result.success(responseText.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateMnemonic(
        front: String,
        back: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyConfigured(apiKey)) {
            return@withContext Result.success(
                "🧠 Memory Association:\n• Associate '$front' with an active visual image of '$back'.\n• Create an emotional or exaggerated story linking them together."
            )
        }

        val prompt = """
            You are a world-class memory athlete and mnemonic specialist.
            Create a highly memorable, vivid mnemonic hook or mental association trick for this flashcard:
            Front: "$front"
            Back: "$back"

            Use vivid sensory imagery, humorous associations, acronyms, or phonetic sound-alikes to make it stick effortlessly in long-term memory.
            Keep it under 100 words.
        """.trimIndent()

        try {
            val responseText = executeGeminiRequest(apiKey, prompt)
            Result.success(responseText.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun evaluateAnswer(
        front: String,
        expectedBack: String,
        userAnswer: String
    ): Result<AnswerEvaluation> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyConfigured(apiKey)) {
            // Local fallback comparison
            val isExact = userAnswer.trim().equals(expectedBack.trim(), ignoreCase = true)
            val status = if (isExact) EvaluationStatus.CORRECT else EvaluationStatus.PARTIALLY_CORRECT
            return@withContext Result.success(
                AnswerEvaluation(
                    status = status,
                    feedback = if (isExact) "Matches the expected answer." else "Compared locally. Expected: $expectedBack",
                    suggestedRating = if (isExact) ReviewRating.GOOD else ReviewRating.HARD
                )
            )
        }

        val prompt = """
            You are evaluating a student's answer to a flashcard.
            Front (Question): "$front"
            Expected Back (Answer): "$expectedBack"
            Student's Given Answer: "$userAnswer"

            Evaluate the student's answer based on semantic meaning, not just exact word matching.
            Return ONLY a valid JSON object with the following keys:
            - "status": "CORRECT" or "PARTIALLY_CORRECT" or "INCORRECT"
            - "feedback": 1-2 sentences of encouraging, constructive feedback explaining how close they were.
            - "suggestedRating": "EASY" or "GOOD" or "HARD" or "AGAIN"

            Do not include markdown markers.
        """.trimIndent()

        try {
            val responseText = executeGeminiRequest(apiKey, prompt)
            val jsonString = cleanJsonOutput(responseText)
            val json = JSONObject(jsonString)

            val statusStr = json.optString("status", "PARTIALLY_CORRECT")
            val feedback = json.optString("feedback", "Answer evaluated.")
            val ratingStr = json.optString("suggestedRating", "GOOD")

            val status = when (statusStr.uppercase()) {
                "CORRECT" -> EvaluationStatus.CORRECT
                "INCORRECT" -> EvaluationStatus.INCORRECT
                else -> EvaluationStatus.PARTIALLY_CORRECT
            }

            val suggestedRating = when (ratingStr.uppercase()) {
                "EASY" -> ReviewRating.EASY
                "GOOD" -> ReviewRating.GOOD
                "HARD" -> ReviewRating.HARD
                "AGAIN" -> ReviewRating.AGAIN
                else -> null
            }

            Result.success(AnswerEvaluation(status, feedback, suggestedRating))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateCardsFromImage(
        imageBase64: String,
        mimeType: String,
        count: Int,
        promptHint: String
    ): Result<List<GeneratedCard>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyConfigured(apiKey)) {
            val fallbackTopic = if (promptHint.isNotBlank()) promptHint else "Visual Notes"
            return@withContext Result.success(getFallbackGeneratedCards(fallbackTopic, count))
        }

        try {
            val url = "$BASE_URL?key=$apiKey"
            val prompt = """
                You are an expert educator and visual notes analyzer.
                Analyze the provided image (which may contain lecture notes, slides, diagrams, or textbook pages).
                ${if (promptHint.isNotBlank()) "User focus: $promptHint" else ""}
                Extract key facts, questions, and definitions into exactly $count high-quality flashcards.
                Each flashcard must have a concise 'front' (question/prompt) and an accurate 'back' (answer/explanation).
                Return ONLY a valid JSON array of objects with keys "front" and "back". Do not include markdown code block markers or extra text.
                Format: [{"front": "Q1", "back": "A1"}]
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", mimeType)
                            put("data", imageBase64)
                        })
                    })
                    partsArray.put(JSONObject().apply {
                        put("text", prompt)
                    })
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val rawResponse = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    throw Exception("Gemini API error (HTTP ${response.code}): $errBody")
                }
                val body = response.body?.string() ?: throw Exception("Empty response body from Gemini")
                val root = JSONObject(body)
                val candidates = root.getJSONArray("candidates")
                val first = candidates.getJSONObject(0)
                val content = first.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                parts.getJSONObject(0).getString("text")
            }

            val cleaned = cleanJsonOutput(rawResponse)
            val jsonArray = JSONArray(cleaned)
            val cards = mutableListOf<GeneratedCard>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val front = obj.optString("front", "").trim()
                val back = obj.optString("back", "").trim()
                if (front.isNotEmpty() && back.isNotEmpty()) {
                    cards.add(GeneratedCard(front = front, back = back))
                }
            }
            Result.success(cards)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!isKeyConfigured(apiKey)) {
            return@withContext Result.failure(Exception("Gemini API key is not configured."))
        }
        try {
            val response = executeGeminiRequest(apiKey, "Say 'OK' if you can read this message.")
            Result.success("Connected to Gemini successfully! Response: $response")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun executeGeminiRequest(apiKey: String, prompt: String): String {
        val url = "$BASE_URL?key=$apiKey"

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArray = JSONArray()
                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })
                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                throw Exception("Gemini API error (HTTP ${response.code}): $errBody")
            }
            val body = response.body?.string() ?: throw Exception("Empty response body from Gemini")
            val root = JSONObject(body)
            val candidates = root.getJSONArray("candidates")
            val first = candidates.getJSONObject(0)
            val content = first.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            val text = parts.getJSONObject(0).getString("text")
            return text
        }
    }

    private fun cleanJsonOutput(text: String): String {
        var trimmed = text.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json")
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```")
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```")
        }
        return trimmed.trim()
    }

    private fun getFallbackGeneratedCards(topic: String, count: Int): List<GeneratedCard> {
        val list = mutableListOf<GeneratedCard>()
        val safeCount = count.coerceIn(3, 15)
        for (i in 1..safeCount) {
            list.add(
                GeneratedCard(
                    front = "$topic — Core concept #$i",
                    back = "Key definition and essential principle for $topic (#$i)"
                )
            )
        }
        return list
    }
}
