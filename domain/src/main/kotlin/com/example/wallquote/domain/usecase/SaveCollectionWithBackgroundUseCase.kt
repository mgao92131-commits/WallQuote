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
 * Saves a collection and reconciles photo asset files.
 *
 * Order: prepare formal file (keep staging) → Room upsert → discard staging →
 * best-effort delete previous formal asset. Room failure deletes the new formal file
 * and keeps staging so the user can retry without re-picking.
 */
class SaveCollectionWithBackgroundUseCase(
    private val repository: CollectionRepository,
    private val assetStore: BackgroundAssetStore,
    private val onCleanupFailure: ((String, Throwable) -> Unit)? = null,
) {
    suspend operator fun invoke(
        config: CollectionConfig,
        previousBackground: BackgroundSpec?,
        stagedAsset: StagedBackgroundAsset?,
    ): SaveCollectionResult {
        var preparedFormalId: String? = null
        val preparedBackground = when {
            stagedAsset != null -> {
                val formal = assetStore.prepareFormalAsset(stagedAsset)
                preparedFormalId = formal.value
                val photo = config.background as? BackgroundSpec.Photo
                    ?: BackgroundSpec.Photo(assetId = formal.value)
                BackgroundValidation.normalize(photo.copy(assetId = formal.value))
            }
            else -> BackgroundValidation.normalize(config.background)
        }

        val toSave = config.copy(background = preparedBackground)
        CollectionValidation.validateForSave(toSave)?.let { error ->
            deletePreparedFormal(preparedFormalId)
            throw IllegalArgumentException(error)
        }

        // Formal photos must use a valid asset id after prepare/normalize.
        if (preparedBackground is BackgroundSpec.Photo &&
            !BackgroundValidation.isValidAssetId(preparedBackground.assetId)
        ) {
            deletePreparedFormal(preparedFormalId)
            throw IllegalArgumentException("图片资产无效，请重新选择")
        }

        val previousAssetId = (previousBackground as? BackgroundSpec.Photo)
            ?.assetId
            ?.takeIf { BackgroundValidation.isValidAssetId(it) }

        val collectionId = try {
            repository.upsertCollection(toSave)
        } catch (error: Throwable) {
            deletePreparedFormal(preparedFormalId)
            throw error
        }

        // After Room success, never roll back the new formal asset.
        if (stagedAsset != null) {
            runCatching { assetStore.discardStaging(stagedAsset) }
                .onFailure { onCleanupFailure?.invoke("discard_staging", it) }
        }

        val newAssetId = (preparedBackground as? BackgroundSpec.Photo)?.assetId
        if (previousAssetId != null && previousAssetId != newAssetId) {
            runCatching { assetStore.delete(BackgroundAssetId(previousAssetId)) }
                .onFailure { onCleanupFailure?.invoke("delete_previous_asset", it) }
        }

        return SaveCollectionResult(
            collectionId = collectionId,
            savedBackground = preparedBackground,
        )
    }

    private suspend fun deletePreparedFormal(assetId: String?) {
        if (assetId == null) return
        runCatching { assetStore.delete(BackgroundAssetId(assetId)) }
    }
}
