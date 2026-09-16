package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppSettingsManager
import com.example.data.repository.FlashcardRepository
import com.example.data.security.SecureKeyStorage
import com.example.domain.ai.AiService
import com.example.domain.ai.UniversalAiService
import com.example.domain.audio.TtsManager
import com.example.domain.notification.ReminderManager
import com.example.domain.srs.ReviewScheduler
import com.example.domain.srs.Sm2Algorithm

/**
 * Dependency Injection container providing application-wide dependencies and singletons.
 */
interface AppContainer {
    val database: AppDatabase
    val repository: FlashcardRepository
    val appSettingsManager: AppSettingsManager
    val secureKeyStorage: SecureKeyStorage
    val aiService: AiService
    val ttsManager: TtsManager
    val reminderManager: ReminderManager
    val srsAlgorithm: Sm2Algorithm
    val reviewScheduler: ReviewScheduler
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }
    override val srsAlgorithm: Sm2Algorithm by lazy { Sm2Algorithm() }
    override val reviewScheduler: ReviewScheduler by lazy { ReviewScheduler() }
    override val repository: FlashcardRepository by lazy {
        FlashcardRepository(
            database = database,
            srsAlgorithm = srsAlgorithm,
            scheduler = reviewScheduler
        )
    }
    override val secureKeyStorage: SecureKeyStorage by lazy { SecureKeyStorage() }
    override val appSettingsManager: AppSettingsManager by lazy {
        AppSettingsManager(context, secureKeyStorage)
    }
    override val aiService: AiService by lazy { UniversalAiService(appSettingsManager) }
    override val ttsManager: TtsManager by lazy { TtsManager(context) }
    override val reminderManager: ReminderManager by lazy { ReminderManager(context) }
}
