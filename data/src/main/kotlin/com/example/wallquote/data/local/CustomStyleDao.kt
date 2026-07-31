package com.example.wallquote.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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

    // P4-014: uniqueness is checked against the normalized (trimmed + lowercased) name so it
    // matches the unique index on `normalizedName`, instead of relying on `lower(name)` which
    // diverged from the (previously case-sensitive) DB index.
    @Query(
        "SELECT COUNT(*) FROM custom_styles WHERE normalizedName = :normalizedName " +
            "AND (:excludingId IS NULL OR id != :excludingId)",
    )
    suspend fun countByNormalizedName(normalizedName: String, excludingId: Long?): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM custom_styles")
    suspend fun maxSortOrder(): Int

    @Query("UPDATE custom_styles SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: Long, sortOrder: Int)

    /**
     * P4-014: reorder must be atomic — either every style's `sortOrder` is updated to match
     * [orderedIds]' position, or none are, so a crash/cancellation mid-reorder can never leave
     * two styles with the same `sortOrder` or an inconsistent ordering.
     */
    @Transaction
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id ->
            updateSortOrder(id, index)
        }
    }
}
