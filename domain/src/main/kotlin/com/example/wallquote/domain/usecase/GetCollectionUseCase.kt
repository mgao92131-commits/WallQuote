package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.repository.CollectionRepository

class GetCollectionUseCase(
    private val repository: CollectionRepository,
) {
    suspend operator fun invoke(id: Long): CollectionConfig? = repository.getCollection(id)
}
