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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EditorViewModelStyleDraftTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectPanel_style_opensDraftWithoutChangingFormalStyle() = runTest {
        val vm = createViewModel()
        val original = vm.uiState.value.textStyle
        vm.selectPanel(EditorPanel.Style)

        assertNull(vm.uiState.value.selectedPanel)
        assertEquals(original, vm.uiState.value.styleDraft?.originalStyle)
        assertEquals(original, vm.uiState.value.styleDraft?.workingStyle)
        assertEquals(original, vm.uiState.value.textStyle)
    }

    @Test
    fun updateWorkingStyle_previewsWithoutCommitting() = runTest {
        val vm = createViewModel()
        val original = vm.uiState.value.textStyle
        vm.selectPanel(EditorPanel.Style)
        vm.updateWorkingStyle { it.copy(colorHex = "#FF00AA", textSizeSp = 48f) }

        assertEquals("#FF00AA", vm.uiState.value.styleDraft?.workingStyle?.colorHex)
        assertEquals("#FF00AA", vm.uiState.value.previewTextStyle.colorHex)
        assertEquals(original, vm.uiState.value.textStyle)
    }

    @Test
    fun confirmStyleDraft_commitsWorkingStyleAndPushesRecent() = runTest {
        val recent = FakeRecentTextStyleRepository()
        val vm = createViewModel(recentRepo = recent)
        vm.selectPanel(EditorPanel.Style)
        vm.updateWorkingStyle { it.copy(colorHex = "#00FFAA") }
        vm.confirmStyleDraft()

        assertNull(vm.uiState.value.styleDraft)
        assertEquals("#00FFAA", vm.uiState.value.textStyle.colorHex)
        assertEquals("#00FFAA", recent.stored?.colorHex)
    }

    @Test
    fun discardStyleDraft_dropsWorkingStyle() = runTest {
        val recent = FakeRecentTextStyleRepository()
        val vm = createViewModel(recentRepo = recent)
        val original = vm.uiState.value.textStyle
        vm.selectPanel(EditorPanel.Style)
        vm.updateWorkingStyle { it.copy(colorHex = "#00FFAA") }
        vm.discardStyleDraft()

        assertNull(vm.uiState.value.styleDraft)
        assertEquals(original, vm.uiState.value.textStyle)
        assertNull(recent.stored)
    }

    @Test
    fun deleteText_lastLineIsNoOp() = runTest {
        val vm = createViewModel()
        val only = vm.uiState.value.texts.single()
        vm.deleteText(only.clientKey)
        assertEquals(1, vm.uiState.value.texts.size)
        assertEquals(only.clientKey, vm.uiState.value.texts.single().clientKey)
    }

    @Test
    fun deleteText_beforePreviewIndex_shiftsIndex() = runTest {
        val vm = createViewModel()
        vm.addTextLine()
        vm.addTextLine()
        val keys = vm.uiState.value.texts.map { it.clientKey }
        vm.selectPreviewIndex(2)
        vm.deleteText(keys[0])

        assertEquals(2, vm.uiState.value.texts.size)
        assertEquals(1, vm.uiState.value.previewTextIndex)
        assertEquals(keys[2], vm.uiState.value.texts[1].clientKey)
    }

    @Test
    fun deleteText_currentPreview_clampsIndex() = runTest {
        val vm = createViewModel()
        vm.addTextLine()
        val keys = vm.uiState.value.texts.map { it.clientKey }
        vm.selectPreviewIndex(1)
        vm.deleteText(keys[1])

        assertEquals(0, vm.uiState.value.previewTextIndex)
        assertEquals(keys[0], vm.uiState.value.texts.single().clientKey)
        assertNotEquals(keys[1], vm.uiState.value.texts.single().clientKey)
    }

    private class FakeRecentTextStyleRepository : RecentTextStyleRepository {
        var items: List<TextStyleConfig> = emptyList()
        val stored: TextStyleConfig? get() = items.firstOrNull()
        override suspend fun getAll(): List<TextStyleConfig> = items
        override suspend fun save(style: TextStyleConfig) {
            items = com.example.wallquote.domain.style.RecentTextStyles.push(style, items)
        }
        override suspend fun clear() {
            items = emptyList()
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

    private fun noopCollectionRepo() = object : CollectionRepository {
        override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(emptyList())
        override suspend fun getCollection(id: Long): CollectionConfig? = null
        override suspend fun upsertCollection(config: CollectionConfig): Long = error("unused")
        override suspend fun deleteCollection(id: Long) = Unit
    }

    private fun createViewModel(
        recentRepo: RecentTextStyleRepository = FakeRecentTextStyleRepository(),
    ): EditorViewModel {
        val collectionRepo = noopCollectionRepo()
        return EditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("collectionId" to -1L)),
            getCollectionUseCase = GetCollectionUseCase(collectionRepo),
            saveCollectionWithBackgroundUseCase = SaveCollectionWithBackgroundUseCase(
                repository = collectionRepo,
                assetStore = FakeAssetStore(),
            ),
            recentTextStyleRepository = recentRepo,
            assetStore = FakeAssetStore(),
            imageProcessor = FakeProcessor(),
            backgroundSampler = NoopSampler(),
        )
    }
}
