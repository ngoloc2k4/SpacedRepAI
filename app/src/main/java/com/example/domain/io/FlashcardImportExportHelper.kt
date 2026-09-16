package com.example.domain.io

import com.example.data.local.entity.CardEntity
import com.example.data.local.entity.DeckEntity
import com.example.domain.srs.CardState
import org.json.JSONArray
import org.json.JSONObject

data class CardBackupDto(
    val front: String,
    val back: String,
    val state: String = CardState.NEW.name,
    val repetitions: Int = 0,
    val intervalDays: Int = 0,
    val easeFactor: Double = 2.5
)

data class DeckBackupDto(
    val name: String,
    val description: String = "",
    val cards: List<CardBackupDto> = emptyList()
)

data class FlashcardBackupDto(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val app: String = "SRS Flashcards",
    val decks: List<DeckBackupDto> = emptyList()
)

object FlashcardImportExportHelper {

    fun exportToJson(decksWithCards: List<Pair<DeckEntity, List<CardEntity>>>): String {
        val root = JSONObject().apply {
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("app", "SRS Flashcards")

            val decksArray = JSONArray()
            for ((deck, cards) in decksWithCards) {
                val deckObj = JSONObject().apply {
                    put("name", deck.name)
                    put("description", deck.description)

                    val cardsArray = JSONArray()
                    for (card in cards) {
                        val cardObj = JSONObject().apply {
                            put("front", card.front)
                            put("back", card.back)
                            put("state", card.state.name)
                            put("repetitions", card.repetitions)
                            put("intervalDays", card.intervalDays)
                            put("easeFactor", card.easeFactor)
                        }
                        cardsArray.put(cardObj)
                    }
                    put("cards", cardsArray)
                }
                decksArray.put(deckObj)
            }
            put("decks", decksArray)
        }

        return root.toString(2)
    }

    fun parseImportJson(jsonString: String): Result<FlashcardBackupDto> {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("Empty JSON content"))
            }

            val root = JSONObject(trimmed)
            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val app = root.optString("app", "SRS Flashcards")

            val decksArray = root.optJSONArray("decks")
                ?: return Result.failure(IllegalArgumentException("Missing 'decks' array in JSON root"))

            val decks = mutableListOf<DeckBackupDto>()
            for (i in 0 until decksArray.length()) {
                val deckObj = decksArray.getJSONObject(i)
                val deckName = deckObj.optString("name", "Imported Deck")
                val deckDesc = deckObj.optString("description", "")

                val cardsArray = deckObj.optJSONArray("cards") ?: JSONArray()
                val cards = mutableListOf<CardBackupDto>()
                for (j in 0 until cardsArray.length()) {
                    val cardObj = cardsArray.getJSONObject(j)
                    val front = cardObj.optString("front", "").trim()
                    val back = cardObj.optString("back", "").trim()
                    if (front.isNotEmpty() && back.isNotEmpty()) {
                        cards.add(
                            CardBackupDto(
                                front = front,
                                back = back,
                                state = cardObj.optString("state", CardState.NEW.name),
                                repetitions = cardObj.optInt("repetitions", 0),
                                intervalDays = cardObj.optInt("intervalDays", 0),
                                easeFactor = cardObj.optDouble("easeFactor", 2.5)
                            )
                        )
                    }
                }
                decks.add(DeckBackupDto(name = deckName, description = deckDesc, cards = cards))
            }

            if (decks.isEmpty() || decks.all { it.cards.isEmpty() }) {
                Result.failure(IllegalArgumentException("No valid flashcards found in JSON"))
            } else {
                Result.success(FlashcardBackupDto(version, exportedAt, app, decks))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
