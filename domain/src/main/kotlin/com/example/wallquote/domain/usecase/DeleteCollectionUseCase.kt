package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.repository.CollectionRepository

class DeleteCollectionUseCase(
    private val repository: CollectionRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteCollection(id)
}
