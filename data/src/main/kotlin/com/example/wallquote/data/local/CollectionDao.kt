package com.example.wallquote.data.local

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
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

    @Insert
    suspend fun insertCollection(entity: CollectionEntity): Long

    @Update
    suspend fun updateCollection(entity: CollectionEntity)

    @Insert
    suspend fun insertLine(line: CollectionTextLineEntity): Long

    @Update
    suspend fun updateLine(line: CollectionTextLineEntity)

    @Query("SELECT id FROM collection_text_lines WHERE collectionId = :collectionId")
    suspend fun getLineIdsForCollection(collectionId: Long): List<Long>

    @Query(
        "DELETE FROM collection_text_lines WHERE collectionId = :collectionId AND id IN (:lineIds)",
    )
    suspend fun deleteLinesByIds(collectionId: Long, lineIds: List<Long>)

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun deleteCollection(id: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM collections")
    suspend fun maxSortOrder(): Int

    @Query("SELECT COUNT(*) FROM collection_text_lines WHERE collectionId = :collectionId")
    suspend fun countLinesForCollection(collectionId: Long): Int

    @Transaction
    suspend fun saveCollectionWithLines(
        collection: CollectionEntity,
        lines: List<CollectionTextLineEntity>,
    ): Long {
        val id = if (collection.id == 0L) {
            insertCollection(collection)
        } else {
            updateCollection(collection)
            collection.id
        }

        val incomingPersistedIds = lines.mapNotNull { line -> line.id.takeIf { it != 0L } }.toSet()
        val existingIds = getLineIdsForCollection(id)
        val removed = existingIds.filter { it !in incomingPersistedIds }
        if (removed.isNotEmpty()) {
            deleteLinesByIds(id, removed)
        }

        lines.sortedBy { it.displayOrder }.forEach { line ->
            val row = line.copy(collectionId = id)
            if (row.id == 0L) {
                insertLine(row)
            } else {
                updateLine(row)
            }
        }
        return id
    }
}
