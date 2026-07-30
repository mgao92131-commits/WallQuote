package com.example.wallquote.data.repository

import com.example.wallquote.data.local.CollectionDao
import com.example.wallquote.data.local.toDomain
import com.example.wallquote.data.local.toEntity
import com.example.wallquote.data.local.toLineEntities
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val dao: CollectionDao,
) : CollectionRepository {

    override fun observeOrderedCollections(): Flow<List<CollectionConfig>> =
        dao.observeAllWithLines().map { list -> list.map { it.toDomain() } }

    override suspend fun getCollection(id: Long): CollectionConfig? =
        dao.getWithLines(id)?.toDomain()

    override suspend fun upsertCollection(config: CollectionConfig): Long {
        val entity = config.toEntity()
        val id = if (entity.id == 0L) {
            val sortOrder = if (config.sortOrder == 0) dao.maxSortOrder() + 1 else config.sortOrder
            dao.insertCollection(entity.copy(sortOrder = sortOrder))
        } else {
            dao.insertCollection(entity)
            entity.id
        }
        dao.deleteLinesForCollection(id)
        val lines = config.copy(id = id).toLineEntities(id)
        if (lines.isNotEmpty()) {
            dao.insertLines(lines)
        }
        return id
    }

    override suspend fun deleteCollection(id: Long) {
        dao.deleteCollection(id)
    }
}
