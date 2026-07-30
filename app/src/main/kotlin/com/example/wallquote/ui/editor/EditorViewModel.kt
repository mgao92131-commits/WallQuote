package com.example.wallquote.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.domain.CollectionDefaults
import com.example.wallquote.domain.CollectionValidation
import com.example.wallquote.domain.editor.EditorDraft
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.time.minuteFromHalfHourIndex
import com.example.wallquote.domain.usecase.GetCollectionUseCase
import com.example.wallquote.domain.usecase.SaveCollectionUseCase
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

    private var originalDraft: EditorDraft? = null
    private var nextClientKey = 1L

    val isDirty: Boolean
        get() {
            val baseline = originalDraft ?: return false
            return _uiState.value.toDraft() != baseline
        }

    init {
        if (argCollectionId != null) {
            loadExisting(argCollectionId)
        } else {
            applyState(buildNewEditorState())
        }
    }

    private fun buildNewEditorState(): EditorUiState {
        val entries = CollectionDefaults.defaultLines.map { line ->
            EditorTextEntry(
                lineId = 0,
                clientKey = nextClientKey++,
                text = line.text,
            )
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
            selectedTab = EditorTab.Content,
        )
    }

    private fun applyState(state: EditorUiState) {
        _uiState.value = state
        originalDraft = state.toDraft()
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            runCatching { getCollectionUseCase(id) }
                .onSuccess { config ->
                    if (config == null) {
                        _events.emit(EditorEvent.Finish)
                    } else {
                        applyState(config.toEditorState())
                    }
                }
                .onFailure {
                    _uiState.update { s ->
                        s.copy(isLoading = false, errorMessage = "加载失败，请返回重试")
                    }
                }
        }
    }

    private fun CollectionConfig.toEditorState(): EditorUiState {
        val entries = lines.sortedBy { it.displayOrder }.map { line ->
            EditorTextEntry(
                lineId = line.id,
                clientKey = line.id.takeIf { it != 0L } ?: nextClientKey++,
                text = line.text,
            )
        }
        return EditorUiState(
            collectionId = id,
            name = name,
            startMinute = schedule.startMinuteOfDay,
            endMinute = schedule.endMinuteOfDay,
            backgroundSpec = background,
            texts = entries.ifEmpty {
                listOf(EditorTextEntry(lineId = 0, clientKey = nextClientKey++, text = ""))
            },
            textStyle = textStyle,
            transform = transform,
            sortOrder = sortOrder,
            isLoading = false,
            previewTextIndex = 0,
            selectedTab = EditorTab.Content,
        )
    }

    fun selectTab(tab: EditorTab?) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun setStartHalfHourIndex(index: Int) {
        _uiState.update { it.copy(startMinute = minuteFromHalfHourIndex(index)) }
    }

    fun setEndHalfHourIndex(index: Int) {
        _uiState.update { it.copy(endMinute = minuteFromHalfHourIndex(index)) }
    }

    fun setSolidBackground(hex: String) {
        _uiState.update { it.copy(backgroundSpec = BackgroundSpec.Solid(hex)) }
    }

    fun addTextLine() {
        _uiState.update { state ->
            val newLine = EditorTextEntry(lineId = 0, clientKey = nextClientKey++, text = "")
            state.copy(
                texts = state.texts + newLine,
                previewTextIndex = state.texts.size,
            )
        }
    }

    fun updateText(clientKey: Long, text: String) {
        _uiState.update { state ->
            state.copy(
                texts = state.texts.map {
                    if (it.clientKey == clientKey) it.copy(text = text) else it
                },
            )
        }
    }

    fun deleteText(clientKey: Long) {
        _uiState.update { state ->
            val filtered = state.texts.filterNot { it.clientKey == clientKey }
            val safe = filtered.ifEmpty {
                listOf(EditorTextEntry(lineId = 0, clientKey = nextClientKey++, text = ""))
            }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveAndFinish() {
        viewModelScope.launch {
            val state = _uiState.value
            val config = state.toCollectionConfig()
            val validationError = CollectionValidation.validateForSave(config)
            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError) }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching { saveCollectionUseCase(config) }
                .onSuccess { id ->
                    _uiState.update {
                        it.copy(collectionId = id, isSaving = false, errorMessage = null)
                    }
                    originalDraft = _uiState.value.toDraft()
                    _events.emit(EditorEvent.Finish)
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "保存失败，请重试")
                    }
                }
        }
    }

    fun requestClose() {
        viewModelScope.launch {
            if (isDirty) {
                _events.emit(EditorEvent.ShowUnsavedDialog)
            } else {
                _events.emit(EditorEvent.Finish)
            }
        }
    }

    fun discardAndFinish() {
        viewModelScope.launch { _events.emit(EditorEvent.Finish) }
    }

    private fun EditorUiState.toCollectionConfig(): CollectionConfig {
        val nonBlank = texts
            .mapIndexed { index, entry ->
                QuoteLine(
                    id = entry.lineId,
                    text = entry.text.trim(),
                    displayOrder = index,
                )
            }
            .filter { it.text.isNotBlank() }
        return CollectionConfig(
            id = collectionId ?: 0,
            name = name.trim(),
            schedule = DailyTimeRange(startMinute, endMinute),
            background = backgroundSpec,
            lines = nonBlank,
            textStyle = textStyle,
            transform = transform,
            sortOrder = sortOrder,
        )
    }
}

sealed interface EditorEvent {
    data object Finish : EditorEvent
    data object ShowUnsavedDialog : EditorEvent
}
