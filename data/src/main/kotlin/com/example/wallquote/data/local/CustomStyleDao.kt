package com.example.wallquote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomStyleDao {
    @Query("SELECT * FROM custom_styles ORDER BY sortOrder ASC, id ASC")
    fun observeOrdered(): Flow<List<CustomStyleEntity>>

    @Query("SELECT * FROM custom_styles WHERE id = :id")
    suspend fun getById(id: Long): CustomStyleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CustomStyleEntity): Long

    @Update
    suspend fun update(entity: CustomStyleEntity): Int

    @Query("DELETE FROM custom_styles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM custom_styles WHERE lower(name) = lower(:name) AND (:excludingId IS NULL OR id != :excludingId)")
    suspend fun countByName(name: String, excludingId: Long?): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM custom_styles")
    suspend fun maxSortOrder(): Int

    @Query("UPDATE custom_styles SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)
}
