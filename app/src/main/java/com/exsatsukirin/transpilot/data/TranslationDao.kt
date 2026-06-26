package com.exsatsukirin.transpilot.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM translations ORDER BY timestamp DESC")
    fun getAll(): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translations WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<List<TranslationRecord>>

    @Query("SELECT * FROM translations WHERE sourceText LIKE '%' || :query || '%' OR translatedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<TranslationRecord>>

    @Insert
    suspend fun insert(record: TranslationRecord): Long

    @Update
    suspend fun update(record: TranslationRecord)

    @Delete
    suspend fun delete(record: TranslationRecord)

    @Query("DELETE FROM translations")
    suspend fun deleteAll()
}
