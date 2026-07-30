package com.example.wallquote.domain.background

/**
 * App-private background image assets. Implementations live outside `:domain`.
 * Paths and Android Uri/Bitmap types must not leak into this module.
 */
@JvmInline
value class BackgroundAssetId(val value: String)

data class StagedBackgroundAsset(
    val draftId: String,
    val stagingToken: String,
)

interface BackgroundAssetStore {
    suspend fun importToStaging(
        sourceUri: String,
        draftId: String,
    ): StagedBackgroundAsset

    /**
     * Creates a formal file from staging without deleting the staging file.
     * On failure, any partial formal file is removed and staging remains usable.
     */
    suspend fun prepareFormalAsset(stagedAsset: StagedBackgroundAsset): BackgroundAssetId

    suspend fun discardStaging(stagedAsset: StagedBackgroundAsset)

    suspend fun delete(assetId: BackgroundAssetId)

    suspend fun exists(assetId: BackgroundAssetId): Boolean

    /** Absolute path string for decoding; null if missing. Domain callers treat as opaque. */
    suspend fun resolvePath(assetId: BackgroundAssetId): String?

    suspend fun resolveStagingPath(stagedAsset: StagedBackgroundAsset): String?

    suspend fun cleanupOrphans(referencedAssetIds: Set<String>)
}
