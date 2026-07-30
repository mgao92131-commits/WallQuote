package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.repository.CollectionRepository

class DeleteCollectionUseCase(
    private val repository: CollectionRepository,
    private val assetStore: BackgroundAssetStore,
) {
    suspend operator fun invoke(id: Long) {
        val existing = repository.getCollection(id)
        repository.deleteCollection(id)
        val photo = existing?.background as? BackgroundSpec.Photo ?: return
        if (BackgroundValidation.isUsablePhotoAssetId(photo.assetId)) {
            runCatching { assetStore.delete(BackgroundAssetId(photo.assetId)) }
        }
    }
}
