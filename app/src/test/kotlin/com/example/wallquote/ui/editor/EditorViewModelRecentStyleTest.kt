package com.example.wallquote.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.example.wallquote.automatch.BackgroundSampler
import com.example.wallquote.domain.automatch.BackgroundSample
import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.StagedBackgroundAsset
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.repository.CollectionRepository
import com.example.wallquote.domain.repository.RecentTextStyleRepository
import com.example.wallquote.domain.usecase.GetCollectionUseCase
import com.example.wallquote.domain.usecase.SaveCollectionWithBackgroundUseCase
import com.example.wallquote.wallpaper.background.BackgroundImageProcessor
import com.example.wallquote.wallpaper.background.ProcessedBackgroundImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the "recently used text style" feature that replaced the custom-style library (see
 * DECISIONS.md, superseding D-021): new collections inherit it, existing collections never have
 * it forced onto them, and only a successful save updates it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EditorViewModelRecentStyleTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRecentTextStyleRepository(
        initial: TextStyleConfig? = null,
    ) : RecentTextStyleRepository {
        var stored: TextStyleConfig? = initial
        override suspend fun get(): TextStyleConfig? = stored
        override suspend fun save(style: TextStyleConfig) {
            stored = style
        }
        override suspend fun clear() {
            stored = null
        }
    }

    private class FakeAssetStore : BackgroundAssetStore {
        override suspend fun importToStaging(sourceUri: String, draftId: String): StagedBackgroundAsset =
            error("unused")
        override suspend fun prepareFormalAsset(stagedAsset: StagedBackgroundAsset): BackgroundAssetId =
            error("unused")
        override suspend fun discardStaging(stagedAsset: StagedBackgroundAsset) = Unit
        override suspend fun delete(assetId: BackgroundAssetId) = Unit
        override suspend fun exists(assetId: BackgroundAssetId): Boolean = false
        override suspend fun resolvePath(assetId: BackgroundAssetId): String? = null
        override suspend fun resolveStagingPath(stagedAsset: StagedBackgroundAsset): String? = null
        override suspend fun cleanupOrphans(referencedAssetIds: Set<String>) = Unit
    }

    private class FakeProcessor : BackgroundImageProcessor {
        override suspend fun process(
            sourcePath: String,
            targetWidth: Int,
            targetHeight: Int,
            blurRadiusDp: Float,
            density: Float,
        ): ProcessedBackgroundImage = error("unused")
    }

    private class NoopSampler : BackgroundSampler {
        override suspend fun sample(background: BackgroundSpec, photoBitmap: Bitmap?): BackgroundSample =
            error("unused")
    }

    private fun existingConfig(style: TextStyleConfig) = CollectionConfig(
        id = 1L,
        name = "既有收藏集",
        schedule = DailyTimeRange(0, 0),
        background = BackgroundSpec.Solid("#2E3440"),
        lines = listOf(QuoteLine(id = 1, text = "既有名言", displayOrder = 0)),
        textStyle = style,
    )

    private fun createViewModel(
        collectionId: Long? = null,
        recentRepo: RecentTextStyleRepository,
        collectionRepo: CollectionRepository,
        saveResult: Result<Long> = Result.success(1L),
    ): EditorViewModel {
        val saveUseCase = SaveCollectionWithBackgroundUseCase(
            repository = object : CollectionRepository by collectionRepo {
                override suspend fun upsertCollection(config: CollectionConfig): Long =
                    saveResult.getOrThrow()
            },
            assetStore = FakeAssetStore(),
        )
        return EditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("collectionId" to (collectionId ?: -1L))),
            getCollectionUseCase = GetCollectionUseCase(collectionRepo),
            saveCollectionWithBackgroundUseCase = saveUseCase,
            recentTextStyleRepository = recentRepo,
            assetStore = FakeAssetStore(),
            imageProcessor = FakeProcessor(),
            backgroundSampler = NoopSampler(),
        )
    }

    private fun noopCollectionRepo() = object : CollectionRepository {
        override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(emptyList())
        override suspend fun getCollection(id: Long): CollectionConfig? = null
        override suspend fun upsertCollection(config: CollectionConfig): Long = error("unused")
        override suspend fun deleteCollection(id: Long) = Unit
    }

    @Test
    fun newCollection_usesRecentStyle_whenOnePresent() = runTest {
        val recent = TextStyleConfig(colorHex = "#AABBCC", textSizeSp = 50f)
        val vm = createViewModel(
            collectionId = null,
            recentRepo = FakeRecentTextStyleRepository(initial = recent),
            collectionRepo = noopCollectionRepo(),
        )

        assertEquals(recent, vm.uiState.value.textStyle)
    }

    @Test
    fun newCollection_usesDefaultStyle_whenNoRecentStyle() = runTest {
        val vm = createViewModel(
            collectionId = null,
            recentRepo = FakeRecentTextStyleRepository(initial = null),
            collectionRepo = noopCollectionRepo(),
        )

        assertEquals(TextStyleConfig(), vm.uiState.value.textStyle)
    }

    @Test
    fun existingCollection_keepsItsOwnStyle_neverOverwrittenByRecent() = runTest {
        val ownStyle = TextStyleConfig(colorHex = "#334455")
        val recentStyle = TextStyleConfig(colorHex = "#AABBCC")
        val collectionRepo = object : CollectionRepository {
            override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(emptyList())
            override suspend fun getCollection(id: Long): CollectionConfig? = existingConfig(ownStyle)
            override suspend fun upsertCollection(config: CollectionConfig): Long = error("unused")
            override suspend fun deleteCollection(id: Long) = Unit
        }
        val vm = createViewModel(
            collectionId = 1L,
            recentRepo = FakeRecentTextStyleRepository(initial = recentStyle),
            collectionRepo = collectionRepo,
        )

        assertEquals(ownStyle, vm.uiState.value.textStyle)
    }

    @Test
    fun saveAndFinish_onSuccess_updatesRecentStyle() = runTest {
        val recentRepo = FakeRecentTextStyleRepository(initial = null)
        val vm = createViewModel(
            collectionId = null,
            recentRepo = recentRepo,
            collectionRepo = noopCollectionRepo(),
        )
        vm.setName("测试收藏集")
        vm.updateText(vm.uiState.value.texts.first().clientKey, "一句名言")
        vm.updateTextStyle { it.copy(colorHex = "#FEDCBA") }

        vm.saveAndFinish()

        assertEquals("#FEDCBA", recentRepo.stored?.colorHex)
    }

    @Test
    fun saveAndFinish_onFailure_doesNotUpdateRecentStyle() = runTest {
        val recentRepo = FakeRecentTextStyleRepository(initial = null)
        val vm = createViewModel(
            collectionId = null,
            recentRepo = recentRepo,
            collectionRepo = noopCollectionRepo(),
            saveResult = Result.failure(IllegalStateException("boom")),
        )
        vm.setName("测试收藏集")
        vm.updateText(vm.uiState.value.texts.first().clientKey, "一句名言")
        vm.updateTextStyle { it.copy(colorHex = "#FEDCBA") }

        vm.saveAndFinish()

        assertNull(recentRepo.stored)
    }

    @Test
    fun discardAndFinish_doesNotUpdateRecentStyle() = runTest {
        val recentRepo = FakeRecentTextStyleRepository(initial = null)
        val vm = createViewModel(
            collectionId = null,
            recentRepo = recentRepo,
            collectionRepo = noopCollectionRepo(),
        )
        vm.updateTextStyle { it.copy(colorHex = "#FEDCBA") }

        vm.discardAndFinish()

        assertNull(recentRepo.stored)
    }
}
