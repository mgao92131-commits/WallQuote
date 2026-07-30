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

class SaveFailedException(message: String) : IllegalStateException(message)

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
    suspend fun updateCollection(entity: CollectionEntity): Int

    @Insert
    suspend fun insertLine(line: CollectionTextLineEntity): Long

    @Query(
        """
        UPDATE collection_text_lines
        SET text = :text, displayOrder = :displayOrder
        WHERE id = :id AND collectionId = :collectionId
        """,
    )
    suspend fun updateOwnedLine(
        id: Long,
        collectionId: Long,
        text: String,
        displayOrder: Int,
    ): Int

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
            val updated = updateCollection(collection)
            if (updated != 1) {
                throw SaveFailedException("Collection ${collection.id} was not updated (rows=$updated)")
            }
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
                val updated = updateOwnedLine(
                    id = row.id,
                    collectionId = id,
                    text = row.text,
                    displayOrder = row.displayOrder,
                )
                if (updated != 1) {
                    throw SaveFailedException(
                        "Quote line ${row.id} was not updated for collection $id (rows=$updated)",
                    )
                }
            }
        }
        return id
    }
}
