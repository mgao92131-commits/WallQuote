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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EditorViewModelBackgroundTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun importPickedPhoto_firstSelection_usesZeroDimAndBlur() = runTest {
        val store = RecordingAssetStore()
        val vm = createViewModel(assetStore = store)

        vm.importPickedPhoto("content://media/first")

        val photo = vm.uiState.value.backgroundSpec as BackgroundSpec.Photo
        assertEquals(0f, photo.dimAmount, 0f)
        assertEquals(0f, photo.blurRadiusDp, 0f)
        assertEquals(store.lastStaged!!.stagingToken, photo.assetId)
    }

    @Test
    fun importPickedPhoto_replacingHistoricalPhoto_resetsDimAndBlur() = runTest {
        val store = RecordingAssetStore()
        val existingAssetId = "bg_0123456789abcdef0123456789abcdef"
        val vm = createViewModel(
            collectionId = 1L,
            collectionRepo = existingCollectionRepo(
                BackgroundSpec.Photo(
                    assetId = existingAssetId,
                    dimAmount = 0.5f,
                    blurRadiusDp = 20f,
                ),
            ),
            assetStore = store,
        )
        val previous = vm.uiState.value.backgroundSpec as BackgroundSpec.Photo
        assertEquals(0.5f, previous.dimAmount, 0f)
        assertEquals(20f, previous.blurRadiusDp, 0f)

        vm.importPickedPhoto("content://media/replacement")

        val photo = vm.uiState.value.backgroundSpec as BackgroundSpec.Photo
        assertEquals(0f, photo.dimAmount, 0f)
        assertEquals(0f, photo.blurRadiusDp, 0f)
        assertEquals(store.lastStaged!!.stagingToken, photo.assetId)
    }

    @Test
    fun cancellingPhotoPicker_keepsPresetAndDoesNotMarkDirty() = runTest {
        val vm = createViewModel()
        val before = vm.uiState.value.backgroundSpec
        assertTrue(before is BackgroundSpec.Solid)
        assertFalse(vm.isDirty)

        vm.onPhotoPickerLaunched()
        vm.onPhotoPickerCancelled()

        assertEquals(before, vm.uiState.value.backgroundSpec)
        assertFalse(vm.isDirty)
        assertEquals(PhotoEditorState.Empty, vm.uiState.value.photoEditorState)
    }

    @Test
    fun switchingFromPhotoToPreset_discardsStagingAndClearsPreview() = runTest {
        val store = RecordingAssetStore()
        val vm = createViewModel(assetStore = store)

        vm.importPickedPhoto("content://media/photo")
        val staged = vm.uiState.value.stagedBackground
        assertNotNull(staged)
        assertNotNull(vm.uiState.value.resolvedPhotoPath)
        assertNotNull(vm.uiState.value.processedPreviewBitmap)

        vm.setSolidBackground("#000000")

        assertEquals(listOf(staged), store.discarded)
        assertNull(vm.uiState.value.stagedBackground)
        assertNull(vm.uiState.value.resolvedPhotoPath)
        assertNull(vm.uiState.value.processedPreviewBitmap)
        assertEquals(BackgroundSpec.Solid("#000000"), vm.uiState.value.backgroundSpec)
        assertEquals(PhotoEditorState.Empty, vm.uiState.value.photoEditorState)
    }

    private class RecordingAssetStore : BackgroundAssetStore {
        val discarded = mutableListOf<StagedBackgroundAsset>()
        var lastStaged: StagedBackgroundAsset? = null
        private var nextToken = 0

        override suspend fun importToStaging(sourceUri: String, draftId: String): StagedBackgroundAsset {
            nextToken += 1
            val token = "draft_01234567-89ab-cdef-0123-${nextToken.toString().padStart(12, '0')}"
            return StagedBackgroundAsset(draftId, token).also { lastStaged = it }
        }

        override suspend fun prepareFormalAsset(stagedAsset: StagedBackgroundAsset): BackgroundAssetId =
            error("unused")

        override suspend fun discardStaging(stagedAsset: StagedBackgroundAsset) {
            discarded += stagedAsset
        }

        override suspend fun delete(assetId: BackgroundAssetId) = Unit

        override suspend fun exists(assetId: BackgroundAssetId): Boolean = false

        override suspend fun resolvePath(assetId: BackgroundAssetId): String? =
            "/formal/${assetId.value}.jpg"

        override suspend fun resolveStagingPath(stagedAsset: StagedBackgroundAsset): String =
            "/staging/${stagedAsset.stagingToken}.jpg"

        override suspend fun cleanupOrphans(referencedAssetIds: Set<String>) = Unit
    }

    private class FakeProcessor : BackgroundImageProcessor {
        override suspend fun process(
            sourcePath: String,
            targetWidth: Int,
            targetHeight: Int,
            blurRadiusDp: Float,
            density: Float,
        ): ProcessedBackgroundImage {
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            return ProcessedBackgroundImage(
                bitmap = bitmap,
                processedWidth = 8,
                processedHeight = 8,
                effectiveBlurRadiusPx = 0,
            )
        }
    }

    private class FakeRecentTextStyleRepository : RecentTextStyleRepository {
        override suspend fun getAll(): List<TextStyleConfig> = emptyList()
        override suspend fun save(style: TextStyleConfig) = Unit
        override suspend fun clear() = Unit
    }

    private class NoopSampler : BackgroundSampler {
        override suspend fun sample(background: BackgroundSpec, photoBitmap: Bitmap?): BackgroundSample =
            error("unused")
    }

    private fun existingCollectionRepo(background: BackgroundSpec) = object : CollectionRepository {
        override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(emptyList())
        override suspend fun getCollection(id: Long): CollectionConfig? = CollectionConfig(
            id = 1L,
            name = "既有收藏集",
            schedule = DailyTimeRange(0, 0),
            background = background,
            lines = listOf(QuoteLine(id = 1, text = "既有名言", displayOrder = 0)),
            textStyle = TextStyleConfig(),
        )
        override suspend fun upsertCollection(config: CollectionConfig): Long = error("unused")
        override suspend fun deleteCollection(id: Long) = Unit
    }

    private fun noopCollectionRepo() = object : CollectionRepository {
        override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(emptyList())
        override suspend fun getCollection(id: Long): CollectionConfig? = null
        override suspend fun upsertCollection(config: CollectionConfig): Long = error("unused")
        override suspend fun deleteCollection(id: Long) = Unit
    }

    private fun createViewModel(
        collectionId: Long? = null,
        collectionRepo: CollectionRepository = noopCollectionRepo(),
        assetStore: BackgroundAssetStore = RecordingAssetStore(),
    ): EditorViewModel {
        val saveUseCase = SaveCollectionWithBackgroundUseCase(
            repository = collectionRepo,
            assetStore = assetStore,
        )
        return EditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("collectionId" to (collectionId ?: -1L))),
            getCollectionUseCase = GetCollectionUseCase(collectionRepo),
            saveCollectionWithBackgroundUseCase = saveUseCase,
            recentTextStyleRepository = FakeRecentTextStyleRepository(),
            assetStore = assetStore,
            imageProcessor = FakeProcessor(),
            backgroundSampler = NoopSampler(),
        )
    }
}
