package com.example.wallquote.domain.repository

import com.example.wallquote.domain.model.CollectionConfig
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeOrderedCollections(): Flow<List<CollectionConfig>>

    suspend fun getCollection(id: Long): CollectionConfig?

    suspend fun upsertCollection(config: CollectionConfig): Long

    suspend fun deleteCollection(id: Long)
}
