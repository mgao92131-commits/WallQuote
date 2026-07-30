package com.example.wallquote.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.domain.CollectionDefaults
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.usecase.GetCollectionUseCase
import com.example.wallquote.domain.usecase.SaveCollectionUseCase
import com.example.wallquote.ui.util.snapToHalfHour
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCollectionUseCase: GetCollectionUseCase,
    private val saveCollectionUseCase: SaveCollectionUseCase,
) : ViewModel() {

    private val argCollectionId: Long? =
        savedStateHandle.get<Long>("collectionId")?.takeIf { it >= 0 }

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    private var nextLocalTextId = 1L

    init {
        if (argCollectionId != null) {
            loadExisting(argCollectionId)
        } else {
            _uiState.value = defaultNewState()
        }
    }

    private fun defaultNewState(): EditorUiState {
        val entries = CollectionDefaults.defaultTexts.map { text ->
            EditorTextEntry(localId = nextLocalTextId++, text = text)
        }
        return EditorUiState(
            collectionId = null,
            name = CollectionDefaults.NEW_COLLECTION_NAME,
            startMinute = 0,
            endMinute = 0,
            backgroundSpec = BackgroundSpec.Solid(CollectionDefaults.DEFAULT_SOLID_HEX),
            texts = entries,
            textStyle = TextStyleConfig(),
            isLoading = false,
            hasPersistedOnce = false,
            selectedTab = EditorTab.Content,
        )
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            val config = getCollectionUseCase(id)
            if (config == null) {
                _events.emit(EditorEvent.Finish)
                return@launch
            }
            _uiState.value = config.toEditorState()
        }
    }

    private fun CollectionConfig.toEditorState(): EditorUiState {
        val entries = texts.map { text ->
            EditorTextEntry(localId = nextLocalTextId++, text = text)
        }
        return EditorUiState(
            collectionId = id,
            name = name,
            startMinute = schedule.startMinuteOfDay,
            endMinute = schedule.endMinuteOfDay,
            backgroundSpec = background,
            texts = entries.ifEmpty { listOf(EditorTextEntry(nextLocalTextId++, "")) },
            textStyle = textStyle,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotation,
            sortOrder = sortOrder,
            isLoading = false,
            hasPersistedOnce = true,
            previewTextIndex = 0,
            selectedTab = EditorTab.Content,
        )
    }

    fun selectTab(tab: EditorTab?) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun setStartMinute(minute: Int) {
        _uiState.update { it.copy(startMinute = snapToHalfHour(minute)) }
    }

    fun setEndMinute(minute: Int) {
        _uiState.update { it.copy(endMinute = snapToHalfHour(minute)) }
    }

    fun setSolidBackground(hex: String) {
        _uiState.update { it.copy(backgroundSpec = BackgroundSpec.Solid(hex)) }
    }

    fun addTextLine() {
        _uiState.update { state ->
            val newLine = EditorTextEntry(nextLocalTextId++, "")
            state.copy(
                texts = state.texts + newLine,
                previewTextIndex = state.texts.size,
            )
        }
    }

    fun updateText(localId: Long, text: String) {
        _uiState.update { state ->
            state.copy(
                texts = state.texts.map { if (it.localId == localId) it.copy(text = text) else it },
            )
        }
    }

    fun deleteText(localId: Long) {
        _uiState.update { state ->
            val filtered = state.texts.filterNot { it.localId == localId }
            val safe = filtered.ifEmpty { listOf(EditorTextEntry(nextLocalTextId++, "")) }
            val newIndex = state.previewTextIndex.coerceIn(0, (safe.size - 1).coerceAtLeast(0))
            state.copy(texts = safe, previewTextIndex = newIndex)
        }
    }

    fun selectPreviewIndex(index: Int) {
        _uiState.update { it.copy(previewTextIndex = index) }
    }

    fun updateTextStyle(transform: (TextStyleConfig) -> TextStyleConfig) {
        _uiState.update { it.copy(textStyle = transform(it.textStyle)) }
    }

    fun saveAndFinish() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.name.isBlank()) return@launch
            _uiState.update { it.copy(isSaving = true) }
            val config = state.toCollectionConfig()
            val id = saveCollectionUseCase(config)
            _uiState.update { it.copy(collectionId = id, isSaving = false, hasPersistedOnce = true) }
            _events.emit(EditorEvent.Finish)
        }
    }

    fun requestClose() {
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.hasPersistedOnce && state.isDirtyComparedToNew()) {
                _events.emit(EditorEvent.ShowDiscardDialog)
            } else {
                _events.emit(EditorEvent.Finish)
            }
        }
    }

    private fun EditorUiState.isDirtyComparedToNew(): Boolean {
        val baseline = defaultNewState()
        return name != baseline.name ||
            texts != baseline.texts ||
            backgroundSpec != baseline.backgroundSpec ||
            textStyle != baseline.textStyle ||
            startMinute != baseline.startMinute ||
            endMinute != baseline.endMinute
    }

    private fun EditorUiState.toCollectionConfig(): CollectionConfig {
        val lines = texts.map { it.text }.filter { it.isNotBlank() }.ifEmpty { listOf("") }
        return CollectionConfig(
            id = collectionId ?: 0,
            name = name.trim(),
            schedule = DailyTimeRange(startMinute, endMinute),
            background = backgroundSpec,
            texts = lines,
            textStyle = textStyle,
            offsetX = offsetX,
            offsetY = offsetY,
            rotation = rotation,
            sortOrder = sortOrder,
        )
    }
}

sealed interface EditorEvent {
    data object Finish : EditorEvent
    data object ShowDiscardDialog : EditorEvent
}
