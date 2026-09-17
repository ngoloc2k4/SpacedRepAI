package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AiProvider(
    val id: String,
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val defaultMethod: String
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
        defaultModel = "gemini-2.5-flash",
        defaultMethod = "POST"
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultEndpoint = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "meta-llama/llama-3.3-70b-instruct",
        defaultMethod = "POST"
    ),
    GROQ(
        id = "groq",
        displayName = "Groq Cloud",
        defaultEndpoint = "https://api.groq.com/openai/v1/chat/completions",
        defaultModel = "llama-3.3-70b-versatile",
        defaultMethod = "POST"
    ),
    NVIDIA(
        id = "nvidia",
        displayName = "NVIDIA NIM",
        defaultEndpoint = "https://integrate.api.nvidia.com/v1/chat/completions",
        defaultModel = "meta/llama-3.1-70b-instruct",
        defaultMethod = "POST"
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom (OpenAI Compatible)",
        defaultEndpoint = "https://api.openai.com/v1/chat/completions",
        defaultModel = "gpt-4o-mini",
        defaultMethod = "POST"
    );

    companion object {
        fun fromId(id: String): AiProvider = entries.find { it.id.equals(id, ignoreCase = true) } ?: GEMINI
    }
}

data class AppSettings(
    val dailyNewCardsLimit: Int = 20,
    val dailyReviewCardsLimit: Int = 100,
    val cardTimerSeconds: Int = 0, // 0 = disabled
    val autoSpeakFront: Boolean = false,
    val autoSpeakBack: Boolean = false,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val hapticEnabled: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val aiProvider: AiProvider = AiProvider.GEMINI,
    val aiEndpoint: String = "",
    val aiHttpMethod: String = "POST",
    val aiModel: String = "",
    val aiApiKey: String = "",
    val aiCustomHeaders: String = ""
) {
    fun getEffectiveEndpoint(): String = if (aiEndpoint.isNotBlank()) aiEndpoint.trim() else aiProvider.defaultEndpoint
    fun getEffectiveModel(): String = if (aiModel.isNotBlank()) aiModel.trim() else aiProvider.defaultModel
    fun getEffectiveHttpMethod(): String = if (aiHttpMethod.isNotBlank()) aiHttpMethod.trim().uppercase() else aiProvider.defaultMethod
}

class AppSettingsManager(context: Context) {

    private val prefs: SharedPreferences = createPreferences(context)

    private fun createPreferences(context: Context): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "app_settings_encrypted_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.values()[0],
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)
        }
    }

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        val providerId = prefs.getString("key_ai_provider", AiProvider.GEMINI.id) ?: AiProvider.GEMINI.id
        return AppSettings(
            dailyNewCardsLimit = prefs.getInt("key_new_limit", 20),
            dailyReviewCardsLimit = prefs.getInt("key_review_limit", 100),
            cardTimerSeconds = prefs.getInt("key_timer_seconds", 0),
            autoSpeakFront = prefs.getBoolean("key_auto_speak_front", false),
            autoSpeakBack = prefs.getBoolean("key_auto_speak_back", false),
            speechRate = prefs.getFloat("key_speech_rate", 1.0f),
            speechPitch = prefs.getFloat("key_speech_pitch", 1.0f),
            hapticEnabled = prefs.getBoolean("key_haptic_enabled", true),
            reminderEnabled = prefs.getBoolean("key_reminder_enabled", false),
            reminderHour = prefs.getInt("key_reminder_hour", 20),
            reminderMinute = prefs.getInt("key_reminder_minute", 0),
            aiProvider = AiProvider.fromId(providerId),
            aiEndpoint = prefs.getString("key_ai_endpoint", "") ?: "",
            aiHttpMethod = prefs.getString("key_ai_http_method", "POST") ?: "POST",
            aiModel = prefs.getString("key_ai_model", "") ?: "",
            aiApiKey = prefs.getString("key_ai_api_key", "") ?: "",
            aiCustomHeaders = prefs.getString("key_ai_custom_headers", "") ?: ""
        )
    }

    fun updateDailyNewCardsLimit(limit: Int) {
        prefs.edit().putInt("key_new_limit", limit).apply()
        _settingsFlow.value = _settingsFlow.value.copy(dailyNewCardsLimit = limit)
    }

    fun updateDailyReviewCardsLimit(limit: Int) {
        prefs.edit().putInt("key_review_limit", limit).apply()
        _settingsFlow.value = _settingsFlow.value.copy(dailyReviewCardsLimit = limit)
    }

    fun updateCardTimerSeconds(seconds: Int) {
        prefs.edit().putInt("key_timer_seconds", seconds).apply()
        _settingsFlow.value = _settingsFlow.value.copy(cardTimerSeconds = seconds)
    }

    fun updateAutoSpeakFront(enabled: Boolean) {
        prefs.edit().putBoolean("key_auto_speak_front", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoSpeakFront = enabled)
    }

    fun updateAutoSpeakBack(enabled: Boolean) {
        prefs.edit().putBoolean("key_auto_speak_back", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(autoSpeakBack = enabled)
    }

    fun updateSpeechRate(rate: Float) {
        prefs.edit().putFloat("key_speech_rate", rate).apply()
        _settingsFlow.value = _settingsFlow.value.copy(speechRate = rate)
    }

    fun updateSpeechPitch(pitch: Float) {
        prefs.edit().putFloat("key_speech_pitch", pitch).apply()
        _settingsFlow.value = _settingsFlow.value.copy(speechPitch = pitch)
    }

    fun updateHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("key_haptic_enabled", enabled).apply()
        _settingsFlow.value = _settingsFlow.value.copy(hapticEnabled = enabled)
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean("key_reminder_enabled", enabled)
            .putInt("key_reminder_hour", hour)
            .putInt("key_reminder_minute", minute)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            reminderEnabled = enabled,
            reminderHour = hour,
            reminderMinute = minute
        )
    }

    fun updateAiProvider(provider: AiProvider) {
        prefs.edit().putString("key_ai_provider", provider.id).apply()
        _settingsFlow.value = _settingsFlow.value.copy(aiProvider = provider)
    }

    fun updateAiEndpoint(endpoint: String) {
        prefs.edit().putString("key_ai_endpoint", endpoint).apply()
        _settingsFlow.value = _settingsFlow.value.copy(aiEndpoint = endpoint)
    }

    fun updateAiHttpMethod(method: String) {
        val normalized = method.trim().uppercase()
        prefs.edit().putString("key_ai_http_method", normalized).apply()
        _settingsFlow.value = _settingsFlow.value.copy(aiHttpMethod = normalized)
    }

    fun updateAiModel(model: String) {
        prefs.edit().putString("key_ai_model", model).apply()
        _settingsFlow.value = _settingsFlow.value.copy(aiModel = model)
    }

    fun updateAiApiKey(apiKey: String) {
        prefs.edit().putString("key_ai_api_key", apiKey).apply()
        _settingsFlow.value = _settingsFlow.value.copy(aiApiKey = apiKey)
    }

    fun updateAiCustomHeaders(headers: String) {
        prefs.edit().putString("key_ai_custom_headers", headers).apply()
        _settingsFlow.value = _settingsFlow.value.copy(aiCustomHeaders = headers)
    }

    fun resetAiToProviderDefaults(provider: AiProvider) {
        prefs.edit()
            .putString("key_ai_provider", provider.id)
            .putString("key_ai_endpoint", provider.defaultEndpoint)
            .putString("key_ai_http_method", provider.defaultMethod)
            .putString("key_ai_model", provider.defaultModel)
            .apply()
        _settingsFlow.value = _settingsFlow.value.copy(
            aiProvider = provider,
            aiEndpoint = provider.defaultEndpoint,
            aiHttpMethod = provider.defaultMethod,
            aiModel = provider.defaultModel
        )
    }
}
