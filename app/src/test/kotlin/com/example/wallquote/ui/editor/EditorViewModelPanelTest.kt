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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EditorViewModelPanelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun newCollection_startsWithNoPanelOpen() = runTest {
        val vm = createViewModel(collectionId = null)
        assertNull(vm.uiState.value.selectedPanel)
    }

    @Test
    fun existingCollection_startsWithNoPanelOpen() = runTest {
        val vm = createViewModel(collectionId = 1L, collectionRepo = existingCollectionRepo())
        assertNull(vm.uiState.value.selectedPanel)
    }

    @Test
    fun selectPanel_opensThenTogglesClosed() = runTest {
        val vm = createViewModel(collectionId = null)
        vm.selectPanel(EditorPanel.Background)
        assertEquals(EditorPanel.Background, vm.uiState.value.selectedPanel)

        vm.selectPanel(EditorPanel.Background)
        assertNull(vm.uiState.value.selectedPanel)
    }

    @Test
    fun selectPanel_switchesBetweenPanels() = runTest {
        val vm = createViewModel(collectionId = null)
        vm.selectPanel(EditorPanel.Time)
        vm.selectPanel(EditorPanel.Content)
        assertEquals(EditorPanel.Content, vm.uiState.value.selectedPanel)
    }

    @Test
    fun dismissPanel_clearsSelection() = runTest {
        val vm = createViewModel(collectionId = null)
        vm.selectPanel(EditorPanel.Style)
        vm.dismissPanel()
        assertNull(vm.uiState.value.selectedPanel)
    }

    private class FakeRecentTextStyleRepository : RecentTextStyleRepository {
        override suspend fun getAll(): List<TextStyleConfig> = emptyList()
        override suspend fun save(style: TextStyleConfig) = Unit
        override suspend fun clear() = Unit
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

    private fun existingCollectionRepo() = object : CollectionRepository {
        override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(emptyList())
        override suspend fun getCollection(id: Long): CollectionConfig? = CollectionConfig(
            id = 1L,
            name = "既有收藏集",
            schedule = DailyTimeRange(0, 0),
            background = BackgroundSpec.Solid("#2E3440"),
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
        collectionId: Long?,
        collectionRepo: CollectionRepository = noopCollectionRepo(),
    ): EditorViewModel {
        val saveUseCase = SaveCollectionWithBackgroundUseCase(
            repository = collectionRepo,
            assetStore = FakeAssetStore(),
        )
        return EditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("collectionId" to (collectionId ?: -1L))),
            getCollectionUseCase = GetCollectionUseCase(collectionRepo),
            saveCollectionWithBackgroundUseCase = saveUseCase,
            recentTextStyleRepository = FakeRecentTextStyleRepository(),
            assetStore = FakeAssetStore(),
            imageProcessor = FakeProcessor(),
            backgroundSampler = NoopSampler(),
        )
    }
}
