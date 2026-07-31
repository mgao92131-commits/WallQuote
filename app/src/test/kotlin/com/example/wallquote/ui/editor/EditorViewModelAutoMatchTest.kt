package com.example.wallquote.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.example.wallquote.automatch.BackgroundSampler
import com.example.wallquote.domain.automatch.BackgroundSample
import com.example.wallquote.domain.automatch.Contrast
import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.StagedBackgroundAsset
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.repository.CollectionRepository
import com.example.wallquote.domain.repository.CustomStyleRepository
import com.example.wallquote.domain.usecase.CreateStyleFromCollectionUseCase
import com.example.wallquote.domain.usecase.GetCollectionUseCase
import com.example.wallquote.domain.usecase.ObserveCustomStylesUseCase
import com.example.wallquote.domain.usecase.SaveCollectionWithBackgroundUseCase
import com.example.wallquote.wallpaper.background.BackgroundImageProcessor
import com.example.wallquote.wallpaper.background.ProcessedBackgroundImage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * P4-004 / P4-005: Auto Match preview semantics and stale-result rejection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EditorViewModelAutoMatchTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class ControllableSampler : BackgroundSampler {
        val gate = CompletableDeferred<Unit>()
        var sample: BackgroundSample = BackgroundSample(
            averageColorArgb = 0xFFFFFFFFL,
            dominantColorArgb = 0xFFFFFFFFL,
            averageLuminance = 1f,
            contrastWithWhite = Contrast.contrastWithWhite(0xFFFFFFFFL),
            contrastWithBlack = Contrast.contrastWithBlack(0xFFFFFFFFL),
            visualComplexity = 0f,
        )

        override suspend fun sample(background: BackgroundSpec, photoBitmap: Bitmap?): BackgroundSample {
            gate.await()
            return sample
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

    private fun createViewModel(sampler: ControllableSampler): EditorViewModel {
        val collectionRepo = object : CollectionRepository {
            override fun observeOrderedCollections(): Flow<List<CollectionConfig>> = flowOf(emptyList())
            override suspend fun getCollection(id: Long): CollectionConfig? = null
            override suspend fun upsertCollection(config: CollectionConfig): Long = 1L
            override suspend fun deleteCollection(id: Long) = Unit
        }
        val customRepo = object : CustomStyleRepository {
            override fun observeOrdered(): Flow<List<CustomTextStyle>> = MutableStateFlow(emptyList())
            override suspend fun getById(id: Long): CustomTextStyle? = null
            override suspend fun save(style: CustomTextStyle): Long = 1L
            override suspend fun delete(id: Long) = Unit
            override suspend fun reorder(orderedIds: List<Long>) = Unit
            override suspend fun existsName(name: String, excludingId: Long?): Boolean = false
        }
        val assetStore = FakeAssetStore()
        return EditorViewModel(
            savedStateHandle = SavedStateHandle(mapOf("collectionId" to -1L)),
            getCollectionUseCase = GetCollectionUseCase(collectionRepo),
            saveCollectionWithBackgroundUseCase = SaveCollectionWithBackgroundUseCase(collectionRepo, assetStore),
            observeCustomStylesUseCase = ObserveCustomStylesUseCase(customRepo),
            createStyleFromCollectionUseCase = CreateStyleFromCollectionUseCase(customRepo),
            assetStore = assetStore,
            imageProcessor = FakeProcessor(),
            backgroundSampler = sampler,
        )
    }

    @Test
    fun requestAutoMatch_doesNotMutateFormalTextStyleUntilConfirm() = runTest {
        val sampler = ControllableSampler()
        val vm = createViewModel(sampler)
        val original = vm.uiState.value.textStyle

        vm.requestAutoMatch()
        assertTrue(vm.uiState.value.autoMatchLoading)
        assertEquals(original, vm.uiState.value.textStyle)

        sampler.gate.complete(Unit)
        val suggestion = vm.uiState.value.autoMatchSuggestion
        assertTrue(suggestion != null)
        assertEquals(original, vm.uiState.value.textStyle)
        assertEquals(suggestion!!.style, vm.uiState.value.previewTextStyle)

        vm.undoAutoMatch()
        assertNull(vm.uiState.value.autoMatchSuggestion)
        assertEquals(original, vm.uiState.value.textStyle)
    }

    @Test
    fun confirmAutoMatch_writesFormalStyle() = runTest {
        val sampler = ControllableSampler()
        val vm = createViewModel(sampler)
        val original = vm.uiState.value.textStyle
        vm.requestAutoMatch()
        sampler.gate.complete(Unit)
        val suggested = vm.uiState.value.autoMatchSuggestion!!.style
        assertNotEquals(original.colorHex, suggested.colorHex)

        vm.confirmAutoMatch()
        assertNull(vm.uiState.value.autoMatchSuggestion)
        assertEquals(suggested, vm.uiState.value.textStyle)
        assertEquals(suggested, vm.uiState.value.previewTextStyle)
    }

    @Test
    fun staleResult_ignoredAfterBackgroundChange() = runTest {
        val sampler = ControllableSampler()
        val vm = createViewModel(sampler)
        val original = vm.uiState.value.textStyle

        vm.requestAutoMatch()
        vm.setSolidBackground("#112233")
        sampler.gate.complete(Unit)

        assertNull(vm.uiState.value.autoMatchSuggestion)
        assertEquals(original, vm.uiState.value.textStyle)
        assertTrue(!vm.uiState.value.autoMatchLoading)
    }

    @Test
    fun manualStyleEdit_clearsPendingSuggestion() = runTest {
        val sampler = ControllableSampler()
        val vm = createViewModel(sampler)
        vm.requestAutoMatch()
        sampler.gate.complete(Unit)
        assertTrue(vm.uiState.value.autoMatchSuggestion != null)

        vm.updateTextStyle { it.copy(textSizeSp = 40f) }
        assertNull(vm.uiState.value.autoMatchSuggestion)
        assertEquals(40f, vm.uiState.value.textStyle.textSizeSp, 0.01f)
    }

    @Test
    fun toDraft_usesFormalStyleNotUnconfirmedSuggestion() = runTest {
        val sampler = ControllableSampler()
        val vm = createViewModel(sampler)
        val original = vm.uiState.value.textStyle
        vm.requestAutoMatch()
        sampler.gate.complete(Unit)
        assertTrue(vm.uiState.value.autoMatchSuggestion != null)

        assertEquals(original, vm.uiState.value.toDraft().textStyle)
    }
}
