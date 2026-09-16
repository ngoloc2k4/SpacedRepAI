package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY id ASC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards ORDER BY id ASC")
    suspend fun getAllCardsSnapshot(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id ASC")
    fun getCardsForDeck(deckId: Long): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id ASC")
    suspend fun getCardsForDeckSnapshot(deckId: Long): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    fun getCardById(id: Long): Flow<CardEntity?>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    fun getCardCountForDeck(deckId: Long): Flow<Int>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id ASC LIMIT :limit OFFSET :offset")
    fun getCardsForDeckPaged(deckId: Long, limit: Int, offset: Int): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun getCardsForDeckPagedSnapshot(deckId: Long, limit: Int, offset: Int): List<CardEntity>

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND (front LIKE '%' || :query || '%' OR back LIKE '%' || :query || '%') ORDER BY id ASC LIMIT :limit OFFSET :offset")
    suspend fun searchCardsPaged(deckId: Long, query: String, limit: Int, offset: Int): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>): List<Long>

    @Update
    suspend fun updateCard(card: CardEntity)

    @Delete
    suspend fun deleteCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)
}
