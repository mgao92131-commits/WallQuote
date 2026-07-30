package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.CollectionValidation
import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.background.StagedBackgroundAsset
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.repository.CollectionRepository

/**
 * Saves a collection and reconciles photo asset files (commit staging / delete previous).
 * File IO and Room are not a single atomic transaction; failures compensate by deleting
 * newly committed assets and leaving the previous formal asset intact.
 */
class SaveCollectionWithBackgroundUseCase(
    private val repository: CollectionRepository,
    private val assetStore: BackgroundAssetStore,
) {
    suspend operator fun invoke(
        config: CollectionConfig,
        previousBackground: BackgroundSpec?,
        stagedAsset: StagedBackgroundAsset?,
    ): Long {
        val preparedBackground = when {
            stagedAsset != null -> {
                val committed = assetStore.commit(stagedAsset)
                val photo = config.background as? BackgroundSpec.Photo
                    ?: BackgroundSpec.Photo(assetId = committed.value)
                BackgroundValidation.normalize(
                    photo.copy(assetId = committed.value),
                )
            }
            else -> BackgroundValidation.normalize(config.background)
        }

        val toSave = config.copy(background = preparedBackground)
        CollectionValidation.validateForSave(toSave)?.let { error ->
            if (stagedAsset != null && preparedBackground is BackgroundSpec.Photo) {
                assetStore.delete(BackgroundAssetId(preparedBackground.assetId))
            }
            throw IllegalArgumentException(error)
        }

        val newAssetId = (preparedBackground as? BackgroundSpec.Photo)?.assetId
        val previousAssetId = (previousBackground as? BackgroundSpec.Photo)
            ?.assetId
            ?.takeIf { BackgroundValidation.isUsablePhotoAssetId(it) }

        return try {
            val id = repository.upsertCollection(toSave)
            if (newAssetId != null && previousAssetId != null && previousAssetId != newAssetId) {
                assetStore.delete(BackgroundAssetId(previousAssetId))
            }
            if (preparedBackground !is BackgroundSpec.Photo && previousAssetId != null) {
                assetStore.delete(BackgroundAssetId(previousAssetId))
            }
            id
        } catch (error: Throwable) {
            if (stagedAsset != null && newAssetId != null) {
                runCatching { assetStore.delete(BackgroundAssetId(newAssetId)) }
            }
            throw error
        }
    }
}
