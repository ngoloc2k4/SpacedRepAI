package com.example.domain.ai

import com.example.BuildConfig
import com.example.data.preferences.AiProvider
import com.example.data.preferences.AppSettings
import com.example.data.preferences.AppSettingsManager
import com.example.domain.srs.ReviewRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UniversalAiService(
    private val appSettingsManager: AppSettingsManager,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) : AiService {

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private fun getSettings(): AppSettings = appSettingsManager.settingsFlow.value

    private fun getEffectiveApiKey(settings: AppSettings): String {
        if (settings.aiApiKey.isNotBlank()) {
            return settings.aiApiKey.trim()
        }
        if (settings.aiProvider == AiProvider.GEMINI) {
            return try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Throwable) {
                ""
            }
        }
        return ""
    }

    private fun isKeyConfigured(key: String): Boolean {
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("placeholder")
    }

    override suspend fun generateCards(
        topic: String,
        count: Int,
        language: String
    ): Result<List<GeneratedCard>> = withContext(Dispatchers.IO) {
        val settings = getSettings()
        val apiKey = getEffectiveApiKey(settings)

        if (!isKeyConfigured(apiKey)) {
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
            val responseText = executePrompt(settings, apiKey, prompt)
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
                Result.failure(Exception("AI returned empty flashcards"))
            } else {
                Result.success(cards)
            }
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
        val settings = getSettings()
        val apiKey = getEffectiveApiKey(settings)

        if (!isKeyConfigured(apiKey)) {
            return@withContext Result.success(getFallbackImageCards(count, promptHint))
        }

        val hintInstruction = if (promptHint.isNotBlank()) {
            "Special focus for generation: \"$promptHint\"."
        } else {
            ""
        }

        val prompt = """
            You are an expert educator and flashcard creator with computer vision capabilities.
            Analyze the attached image (which may contain study notes, textbook pages, diagrams, vocabulary, formulas, or summaries).
            Extract key learning points and generate exactly $count high-yield flashcards.
            $hintInstruction
            Each flashcard must have a concise, clear 'front' (question or concept) and a 'back' (accurate explanation or answer).
            Return ONLY a valid JSON array of objects with keys "front" and "back". Do not include markdown code block markers or additional text.
            Example format:
            [
              {"front": "Question 1", "back": "Answer 1"},
              {"front": "Question 2", "back": "Answer 2"}
            ]
        """.trimIndent()

        try {
            val responseText = executeMultimodalPrompt(settings, apiKey, prompt, imageBase64, mimeType)
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
                Result.failure(Exception("AI did not detect enough legible concepts from image to create flashcards"))
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
        val settings = getSettings()
        val apiKey = getEffectiveApiKey(settings)
        if (!isKeyConfigured(apiKey)) {
            return@withContext Result.success(
                "💡 Concept Breakdown:\n• Question/Prompt: $front\n• Answer: $back\n\n(Configure an API key in Settings -> AI Providers to use live AI explanations with ${settings.aiProvider.displayName})."
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
            val responseText = executePrompt(settings, apiKey, prompt)
            Result.success(responseText.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun generateMnemonic(
        front: String,
        back: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val settings = getSettings()
        val apiKey = getEffectiveApiKey(settings)
        if (!isKeyConfigured(apiKey)) {
            return@withContext Result.success(
                "🧠 Memory Association:\n• Associate '$front' with an active visual image of '$back'.\n• Create an emotional or exaggerated mental image linking them together."
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
            val responseText = executePrompt(settings, apiKey, prompt)
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
        val settings = getSettings()
        val apiKey = getEffectiveApiKey(settings)
        if (!isKeyConfigured(apiKey)) {
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
            val responseText = executePrompt(settings, apiKey, prompt)
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

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val settings = getSettings()
        val apiKey = getEffectiveApiKey(settings)
        if (!isKeyConfigured(apiKey)) {
            return@withContext Result.failure(Exception("API Key is missing or empty. Please enter your API key in Settings."))
        }

        val prompt = "Reply with exactly: 'OK - Connected successfully to " + settings.aiProvider.displayName + " (" + settings.getEffectiveModel() + ")'"
        try {
            val response = executePrompt(settings, apiKey, prompt)
            Result.success(response.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // =========================================================================
    // Core Execution Logic: Handles both Gemini and OpenAI-compatible endpoints
    // =========================================================================

    private fun executePrompt(settings: AppSettings, apiKey: String, prompt: String): String {
        return if (settings.aiProvider == AiProvider.GEMINI) {
            executeGeminiRequest(settings, apiKey, prompt, null, null)
        } else {
            executeOpenAiCompatibleRequest(settings, apiKey, prompt, null, null)
        }
    }

    private fun executeMultimodalPrompt(
        settings: AppSettings,
        apiKey: String,
        prompt: String,
        imageBase64: String,
        mimeType: String
    ): String {
        return if (settings.aiProvider == AiProvider.GEMINI) {
            executeGeminiRequest(settings, apiKey, prompt, imageBase64, mimeType)
        } else {
            executeOpenAiCompatibleRequest(settings, apiKey, prompt, imageBase64, mimeType)
        }
    }

    private fun executeGeminiRequest(
        settings: AppSettings,
        apiKey: String,
        prompt: String,
        imageBase64: String?,
        mimeType: String?
    ): String {
        var url = settings.getEffectiveEndpoint()
        if (url.contains("{model}")) {
            url = url.replace("{model}", settings.getEffectiveModel())
        }
        val fullUrl = if (url.contains("?")) {
            "$url&key=$apiKey"
        } else {
            "$url?key=$apiKey"
        }

        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray()
            val contentObj = JSONObject().apply {
                val partsArray = JSONArray()

                // Text part
                partsArray.put(JSONObject().apply {
                    put("text", prompt)
                })

                // Optional Image part
                if (imageBase64 != null && mimeType != null) {
                    val inlineDataObj = JSONObject().apply {
                        put("mime_type", mimeType)
                        put("data", imageBase64)
                    }
                    partsArray.put(JSONObject().apply {
                        put("inline_data", inlineDataObj)
                    })
                }

                put("parts", partsArray)
            }
            contentsArray.put(contentObj)
            put("contents", contentsArray)
        }

        val method = settings.getEffectiveHttpMethod()
        val body = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder().url(fullUrl)

        when (method) {
            "GET" -> requestBuilder.get()
            "PUT" -> requestBuilder.put(body)
            else -> requestBuilder.post(body)
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw Exception("${settings.aiProvider.displayName} Error (HTTP ${response.code}): $responseBody")
            }
            val root = JSONObject(responseBody)
            val candidates = root.optJSONArray("candidates")
                ?: throw Exception("No candidates returned from Gemini: $responseBody")
            val first = candidates.getJSONObject(0)
            val content = first.getJSONObject("content")
            val parts = content.getJSONArray("parts")
            return parts.getJSONObject(0).getString("text")
        }
    }

    private fun executeOpenAiCompatibleRequest(
        settings: AppSettings,
        apiKey: String,
        prompt: String,
        imageBase64: String?,
        mimeType: String?
    ): String {
        val url = settings.getEffectiveEndpoint()
        val method = settings.getEffectiveHttpMethod()

        val requestJson = JSONObject().apply {
            put("model", settings.getEffectiveModel())
            val messagesArray = JSONArray()

            // System instructions
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", "You are an expert AI flashcard generation assistant. Return high-quality, valid JSON as requested.")
            })

            // User prompt (text or multimodal)
            val userMsg = JSONObject().apply {
                put("role", "user")
                if (imageBase64 != null && mimeType != null) {
                    val contentParts = JSONArray()
                    contentParts.put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                    contentParts.put(JSONObject().apply {
                        put("type", "image_url")
                        val imgUrlObj = JSONObject().apply {
                            put("url", "data:$mimeType;base64,$imageBase64")
                        }
                        put("image_url", imgUrlObj)
                    })
                    put("content", contentParts)
                } else {
                    put("content", prompt)
                }
            }
            messagesArray.put(userMsg)
            put("messages", messagesArray)
            put("temperature", 0.6)
        }

        val body: RequestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val requestBuilder = Request.Builder().url(url)

        when (method) {
            "GET" -> requestBuilder.get()
            "PUT" -> requestBuilder.put(body)
            else -> requestBuilder.post(body)
        }

        // Authorization header
        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        // OpenRouter-specific recommended headers
        if (settings.aiProvider == AiProvider.OPENROUTER) {
            requestBuilder.header("HTTP-Referer", "https://aistudio.google.com")
            requestBuilder.header("X-Title", "AI Flashcards SRS")
        }

        // Parse custom user headers if provided
        parseAndApplyCustomHeaders(requestBuilder, settings.aiCustomHeaders)

        client.newCall(requestBuilder.build()).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw Exception("${settings.aiProvider.displayName} Error (HTTP ${response.code}): $responseBody")
            }
            val root = JSONObject(responseBody)
            val choices = root.optJSONArray("choices")
                ?: throw Exception("No choices in AI response: $responseBody")
            val first = choices.getJSONObject(0)
            val message = first.getJSONObject("message")
            return message.getString("content")
        }
    }

    private fun parseAndApplyCustomHeaders(builder: Request.Builder, headersRaw: String) {
        if (headersRaw.isBlank()) return
        try {
            if (headersRaw.trim().startsWith("{")) {
                val json = JSONObject(headersRaw)
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    builder.header(key, json.getString(key))
                }
            } else {
                val lines = headersRaw.lines()
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.contains(":")) {
                        val parts = trimmed.split(":", limit = 2)
                        builder.header(parts[0].trim(), parts[1].trim())
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore malformed custom headers gracefully
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
        trimmed = trimmed.trim()

        // If extra commentary exists before '[' or '{', extract the json slice
        val firstArray = trimmed.indexOf('[')
        val lastArray = trimmed.lastIndexOf(']')
        if (firstArray != -1 && lastArray != -1 && lastArray > firstArray) {
            return trimmed.substring(firstArray, lastArray + 1)
        }

        val firstObj = trimmed.indexOf('{')
        val lastObj = trimmed.lastIndexOf('}')
        if (firstObj != -1 && lastObj != -1 && lastObj > firstObj) {
            return trimmed.substring(firstObj, lastObj + 1)
        }

        return trimmed
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

    private fun getFallbackImageCards(count: Int, hint: String): List<GeneratedCard> {
        val list = mutableListOf<GeneratedCard>()
        val safeCount = count.coerceIn(3, 10)
        val subject = if (hint.isNotBlank()) hint else "Visual Notes"
        for (i in 1..safeCount) {
            list.add(
                GeneratedCard(
                    front = "$subject — Card #$i from Image",
                    back = "Key definition and extracted insight from visual notes #$i. (Add API key in Settings -> AI Provider to connect live vision model)"
                )
            )
        }
        return list
    }
}
