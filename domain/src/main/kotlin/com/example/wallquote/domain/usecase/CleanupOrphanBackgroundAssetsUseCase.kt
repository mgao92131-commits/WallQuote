package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.first

class CleanupOrphanBackgroundAssetsUseCase(
    private val repository: CollectionRepository,
    private val assetStore: BackgroundAssetStore,
) {
    suspend operator fun invoke() {
        val referenced = repository.observeOrderedCollections().first()
            .mapNotNull { config ->
                val photo = config.background as? BackgroundSpec.Photo ?: return@mapNotNull null
                photo.assetId.takeIf { BackgroundValidation.isUsablePhotoAssetId(it) }
            }
            .toSet()
        assetStore.cleanupOrphans(referenced)
    }
}
