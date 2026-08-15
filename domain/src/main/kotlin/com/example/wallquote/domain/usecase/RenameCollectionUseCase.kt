package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.repository.CollectionRepository

class RenameCollectionUseCase(
    private val repository: CollectionRepository,
) {
    suspend operator fun invoke(id: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotBlank()) { "收藏集名称不能为空" }
        val current = repository.getCollection(id) ?: error("收藏集不存在")
        repository.upsertCollection(current.copy(name = trimmed))
    }
}
