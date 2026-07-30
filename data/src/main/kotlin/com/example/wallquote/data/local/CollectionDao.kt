package com.example.wallquote.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class CollectionWithLines(
    @Embedded val collection: CollectionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "collectionId",
    )
    val lines: List<CollectionTextLineEntity>,
)

@Dao
interface CollectionDao {
    @Transaction
    @Query("SELECT * FROM collections ORDER BY sortOrder ASC, id ASC")
    fun observeAllWithLines(): Flow<List<CollectionWithLines>>

    @Transaction
    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getWithLines(id: Long): CollectionWithLines?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(entity: CollectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLines(lines: List<CollectionTextLineEntity>)

    @Query("DELETE FROM collection_text_lines WHERE collectionId = :collectionId")
    suspend fun deleteLinesForCollection(collectionId: Long)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM collections")
    suspend fun maxSortOrder(): Int
}
