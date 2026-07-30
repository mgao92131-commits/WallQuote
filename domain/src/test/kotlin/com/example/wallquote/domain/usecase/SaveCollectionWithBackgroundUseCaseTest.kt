package com.example.wallquote.domain.usecase

import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.StagedBackgroundAsset
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SaveCollectionWithBackgroundUseCaseTest {

    private class FakeRepo : CollectionRepository {
        var failNext = false
        var lastSaved: CollectionConfig? = null
        override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(emptyList())
        override suspend fun getCollection(id: Long): CollectionConfig? = null
        override suspend fun upsertCollection(config: CollectionConfig): Long {
            if (failNext) {
                failNext = false
                throw IllegalStateException("db_failed")
            }
            lastSaved = config
            return if (config.id == 0L) 42L else config.id
        }
        override suspend fun deleteCollection(id: Long) = Unit
    }

    private class FakeAssets : BackgroundAssetStore {
        val formal = mutableSetOf<String>()
        val staging = mutableSetOf<String>()
        var deletePreviousFail = false
        var prepareCount = 0

        override suspend fun importToStaging(sourceUri: String, draftId: String): StagedBackgroundAsset {
            val token = "draft_01234567-89ab-cdef-0123-456789abcdef"
            staging += token
            return StagedBackgroundAsset(draftId, token)
        }

        override suspend fun prepareFormalAsset(stagedAsset: StagedBackgroundAsset): BackgroundAssetId {
            prepareCount++
            require(stagedAsset.stagingToken in staging)
            val id = "bg_${prepareCount.toString().padStart(32, '0')}"
            formal += id
            return BackgroundAssetId(id)
        }

        override suspend fun discardStaging(stagedAsset: StagedBackgroundAsset) {
            staging.remove(stagedAsset.stagingToken)
        }

        override suspend fun delete(assetId: BackgroundAssetId) {
            if (deletePreviousFail) {
                deletePreviousFail = false
                throw IllegalStateException("delete_failed")
            }
            formal.remove(assetId.value)
        }

        override suspend fun exists(assetId: BackgroundAssetId): Boolean = assetId.value in formal
        override suspend fun resolvePath(assetId: BackgroundAssetId): String? =
            assetId.value.takeIf { it in formal }?.let { "/files/$it.jpg" }
        override suspend fun resolveStagingPath(stagedAsset: StagedBackgroundAsset): String? =
            stagedAsset.stagingToken.takeIf { it in staging }?.let { "/cache/$it.tmp" }
        override suspend fun cleanupOrphans(referencedAssetIds: Set<String>) = Unit
    }

    private fun config(background: BackgroundSpec) = CollectionConfig(
        id = 0,
        name = "n",
        schedule = DailyTimeRange(0, 0),
        background = background,
        lines = listOf(QuoteLine(0, "hello", 0)),
        textStyle = TextStyleConfig(),
    )

    @Test
    fun prepareThenRoomSuccess_discardsStagingAndReturnsFormalId() = runBlocking {
        val repo = FakeRepo()
        val assets = FakeAssets()
        val staged = assets.importToStaging("content://x", "d1")
        val useCase = SaveCollectionWithBackgroundUseCase(repo, assets)
        val result = useCase(
            config = config(BackgroundSpec.Photo(assetId = staged.stagingToken, dimAmount = 0.3f)),
            previousBackground = null,
            stagedAsset = staged,
        )
        assertEquals(42L, result.collectionId)
        val photo = result.savedBackground as BackgroundSpec.Photo
        assertTrue(BackgroundValidationCompat.isFormal(photo.assetId))
        assertTrue(photo.assetId in assets.formal)
        assertFalse(staged.stagingToken in assets.staging)
        assertEquals(0.3f, photo.dimAmount, 0f)
    }

    @Test
    fun roomFailure_deletesNewFormal_keepsStaging() = runBlocking {
        val repo = FakeRepo().also { it.failNext = true }
        val assets = FakeAssets()
        val staged = assets.importToStaging("content://x", "d1")
        val useCase = SaveCollectionWithBackgroundUseCase(repo, assets)
        try {
            useCase(
                config = config(BackgroundSpec.Photo(assetId = staged.stagingToken)),
                previousBackground = null,
                stagedAsset = staged,
            )
            fail("expected failure")
        } catch (_: IllegalStateException) {
        }
        assertTrue(staged.stagingToken in assets.staging)
        assertTrue(assets.formal.isEmpty())
    }

    @Test
    fun roomFailureThenRetrySucceeds() = runBlocking {
        val repo = FakeRepo().also { it.failNext = true }
        val assets = FakeAssets()
        val staged = assets.importToStaging("content://x", "d1")
        val useCase = SaveCollectionWithBackgroundUseCase(repo, assets)
        runCatching {
            useCase(
                config = config(BackgroundSpec.Photo(assetId = staged.stagingToken)),
                previousBackground = null,
                stagedAsset = staged,
            )
        }
        assertTrue(staged.stagingToken in assets.staging)
        val result = useCase(
            config = config(BackgroundSpec.Photo(assetId = staged.stagingToken)),
            previousBackground = null,
            stagedAsset = staged,
        )
        assertTrue((result.savedBackground as BackgroundSpec.Photo).assetId in assets.formal)
        assertFalse(staged.stagingToken in assets.staging)
    }

    @Test
    fun previousDeleteFailure_doesNotRollbackSave() = runBlocking {
        val repo = FakeRepo()
        val assets = FakeAssets().also { it.deletePreviousFail = true }
        val oldId = "bg_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
        assets.formal += oldId
        val staged = assets.importToStaging("content://x", "d1")
        val useCase = SaveCollectionWithBackgroundUseCase(repo, assets)
        val result = useCase(
            config = config(BackgroundSpec.Photo(assetId = staged.stagingToken)),
            previousBackground = BackgroundSpec.Photo(assetId = oldId),
            stagedAsset = staged,
        )
        assertTrue((result.savedBackground as BackgroundSpec.Photo).assetId in assets.formal)
        // Old asset remains because delete failed; orphan cleaner will handle later.
        assertTrue(oldId in assets.formal)
    }

    private object BackgroundValidationCompat {
        fun isFormal(id: String) = id.matches(Regex("^bg_[0-9a-f]{32}$"))
    }
}
