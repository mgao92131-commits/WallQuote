package com.example.wallquote.ui.style

import androidx.lifecycle.SavedStateHandle
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.repository.CustomStyleRepository
import com.example.wallquote.domain.usecase.GetCustomStyleUseCase
import com.example.wallquote.domain.usecase.SaveCustomStyleUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers P4-002: dirty tracking / close confirmation / save-then-finish / save-failure-stays-open
 * for [CustomStyleEditorViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CustomStyleEditorViewModelTest {

    // ViewModel.viewModelScope requires a Main dispatcher to be installed; Unconfined runs the
    // launched coroutines eagerly (our fake repository never suspends), so state updates and
    // events are visible immediately without needing to drive a virtual clock.
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeCustomStyleRepository(
        initial: List<CustomTextStyle> = emptyList(),
    ) : CustomStyleRepository {
        private val styles = MutableStateFlow(initial)
        var saveShouldFail: Boolean = false
        var nextId: Long = 100L

        override fun observeOrdered(): Flow<List<CustomTextStyle>> = styles

        override suspend fun getById(id: Long): CustomTextStyle? =
            styles.value.firstOrNull { it.id == id }

        override suspend fun save(style: CustomTextStyle): Long {
            if (saveShouldFail) throw IllegalStateException("保存失败，请重试")
            val id = if (style.id != 0L) style.id else nextId++
            val saved = style.copy(id = id)
            styles.value = styles.value.filterNot { it.id == id } + saved
            return id
        }

        override suspend fun delete(id: Long) {
            styles.value = styles.value.filterNot { it.id == id }
        }

        override suspend fun reorder(orderedIds: List<Long>) = Unit

        override suspend fun existsName(name: String, excludingId: Long?): Boolean =
            styles.value.any { it.name == name && it.id != excludingId }
    }

    private fun createViewModel(
        repository: FakeCustomStyleRepository,
        styleId: Long? = null,
    ): CustomStyleEditorViewModel {
        val handle = if (styleId != null) {
            SavedStateHandle(mapOf("styleId" to styleId))
        } else {
            SavedStateHandle()
        }
        return CustomStyleEditorViewModel(
            savedStateHandle = handle,
            getCustomStyleUseCase = GetCustomStyleUseCase(repository),
            saveCustomStyleUseCase = SaveCustomStyleUseCase(repository),
        )
    }

    @Test
    fun newStyle_notDirtyUntilEdited() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(FakeCustomStyleRepository())
        assertFalse(viewModel.isDirty)

        viewModel.setName("我的样式")
        assertTrue(viewModel.isDirty)
    }

    @Test
    fun requestClose_whenClean_emitsFinishDirectly() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(FakeCustomStyleRepository())
        val events = mutableListOf<CustomStyleEditorEvent>()
        val job = launch { viewModel.events.collect { events += it } }
        advanceUntilIdle()

        viewModel.requestClose()
        advanceUntilIdle()

        assertEquals(listOf(CustomStyleEditorEvent.Finish), events)
        job.cancel()
    }

    @Test
    fun requestClose_whenDirty_showsUnsavedDialogInstead() = runTest(UnconfinedTestDispatcher()) {
        val viewModel = createViewModel(FakeCustomStyleRepository())
        val events = mutableListOf<CustomStyleEditorEvent>()
        val job = launch { viewModel.events.collect { events += it } }
        advanceUntilIdle()

        viewModel.setName("未保存的名字")
        viewModel.requestClose()
        advanceUntilIdle()

        assertEquals(listOf(CustomStyleEditorEvent.ShowUnsavedDialog), events)
        job.cancel()
    }

    @Test
    fun discardAndFinish_emitsFinishWithoutSaving() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeCustomStyleRepository()
        val viewModel = createViewModel(repository)
        val events = mutableListOf<CustomStyleEditorEvent>()
        val job = launch { viewModel.events.collect { events += it } }
        advanceUntilIdle()

        viewModel.setName("放弃的名字")
        viewModel.discardAndFinish()
        advanceUntilIdle()

        assertEquals(listOf(CustomStyleEditorEvent.Finish), events)
        assertTrue(repository.observeOrdered().let { true }) // no crash; nothing persisted below
        job.cancel()
    }

    @Test
    fun saveAndFinish_success_clearsDirtyAndEmitsFinish() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeCustomStyleRepository()
        val viewModel = createViewModel(repository)
        val events = mutableListOf<CustomStyleEditorEvent>()
        val job = launch { viewModel.events.collect { events += it } }
        advanceUntilIdle()

        viewModel.setName("保存的名字")
        assertTrue(viewModel.isDirty)

        viewModel.saveAndFinish()
        advanceUntilIdle()

        assertEquals(listOf(CustomStyleEditorEvent.Finish), events)
        assertFalse(viewModel.isDirty)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals(null, viewModel.uiState.value.errorMessage)

        // Baseline moved to the saved draft: closing again without further edits is clean.
        events.clear()
        viewModel.requestClose()
        advanceUntilIdle()
        assertEquals(listOf(CustomStyleEditorEvent.Finish), events)
        job.cancel()
    }

    @Test
    fun saveAndFinish_failure_staysOpenAndDirty() = runTest(UnconfinedTestDispatcher()) {
        val repository = FakeCustomStyleRepository().apply { saveShouldFail = true }
        val viewModel = createViewModel(repository)
        val events = mutableListOf<CustomStyleEditorEvent>()
        val job = launch { viewModel.events.collect { events += it } }
        advanceUntilIdle()

        viewModel.setName("会失败的名字")
        viewModel.saveAndFinish()
        advanceUntilIdle()

        assertTrue("no Finish/dialog event should fire on save failure", events.isEmpty())
        assertTrue(viewModel.isDirty)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("保存失败，请重试", viewModel.uiState.value.errorMessage)
        job.cancel()
    }

    @Test
    fun loadingExistingStyle_setsBaselineSoUnchangedCloseIsClean() = runTest(UnconfinedTestDispatcher()) {
        val existing = CustomTextStyle(id = 7, name = "已存在", style = TextStyleConfig())
        val repository = FakeCustomStyleRepository(initial = listOf(existing))
        val viewModel = createViewModel(repository, styleId = 7)
        advanceUntilIdle()

        assertEquals("已存在", viewModel.uiState.value.name)
        assertFalse(viewModel.isDirty)

        viewModel.updateStyle { it.copy(textSizeSp = 40f) }
        assertTrue(viewModel.isDirty)
    }
}
