package com.keptang.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedCategoryDao {

    /** REPLACE, not IGNORE: re-teaching a word must overwrite what it meant before. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(learned: LearnedCategoryEntity)

    @Query("SELECT * FROM learned_categories")
    suspend fun getAll(): List<LearnedCategoryEntity>

    @Query("SELECT * FROM learned_categories ORDER BY updated_at_epoch_millis DESC")
    fun observeAll(): Flow<List<LearnedCategoryEntity>>

    @Query("DELETE FROM learned_categories WHERE keyword = :keyword")
    suspend fun deleteByKeyword(keyword: String)
}
