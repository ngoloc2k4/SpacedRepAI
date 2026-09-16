package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.data.preferences.AppSettingsManager
import com.example.data.repository.FlashcardRepository
import com.example.di.AppContainer
import com.example.di.DefaultAppContainer
import com.example.domain.ai.AiService
import com.example.domain.audio.TtsManager
import com.example.domain.notification.ReminderManager
import com.example.domain.srs.ReviewScheduler
import com.example.domain.srs.Sm2Algorithm
import com.example.util.CrashReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FlashcardApplication : Application() {
    lateinit var container: AppContainer

    val database: AppDatabase get() = container.database
    val srsAlgorithm: Sm2Algorithm get() = container.srsAlgorithm
    val reviewScheduler: ReviewScheduler get() = container.reviewScheduler
    val repository: FlashcardRepository get() = container.repository
    val appSettingsManager: AppSettingsManager get() = container.appSettingsManager
    val aiService: AiService get() = container.aiService
    val ttsManager: TtsManager get() = container.ttsManager
    val reminderManager: ReminderManager get() = container.reminderManager

    override fun onCreate() {
        super.onCreate()
        CrashReporter.initUncaughtExceptionHandler()
        container = DefaultAppContainer(this)
        seedStarterDeckIfEmpty()
    }

    override fun onTerminate() {
        super.onTerminate()
        ttsManager.shutdown()
    }

    private fun seedStarterDeckIfEmpty() {
        CoroutineScope(Dispatchers.IO).launch {
            val existing = database.deckDao().getAllDecks().firstOrNull()
            if (existing.isNullOrEmpty()) {
                val deckId = database.deckDao().insertDeck(
                    DeckEntity(
                        name = "Spaced Repetition & Kotlin",
                        description = "Starter deck demonstrating SM-2 memory intervals and card states"
                    )
                )
                database.cardDao().insertCards(
                    listOf(
                        CardEntity(
                            deckId = deckId,
                            front = "What is the core principle of Spaced Repetition (SRS)?",
                            back = "Reviewing cards at scientifically spaced intervals right before forgetting occurs, shifting recall into long-term memory."
                        ),
                        CardEntity(
                            deckId = deckId,
                            front = "What is the role of Ease Factor (EF) in SM-2?",
                            back = "A multiplier (default 2.5, min 1.3) that determines how fast the next review interval grows upon successful recall."
                        ),
                        CardEntity(
                            deckId = deckId,
                            front = "What happens when you rate a card as 'Again' in SM-2?",
                            back = "Repetitions reset to 0, interval drops to 0 for same-day re-learning, Ease Factor decreases, and card transitions to RELEARNING."
                        )
                    )
                )
            }
        }
    }
}
