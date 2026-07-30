package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.repository.CollectionRepository

class SaveCollectionUseCase(
    private val repository: CollectionRepository,
) {
    suspend operator fun invoke(config: CollectionConfig): Long = repository.upsertCollection(config)
}
