package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow

class ObserveOrderedCollectionsUseCase(
    private val repository: CollectionRepository,
) {
    operator fun invoke(): Flow<List<CollectionConfig>> = repository.observeOrderedCollections()
}
