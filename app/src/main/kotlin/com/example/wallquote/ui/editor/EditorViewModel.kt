package com.example.wallquote.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.domain.CollectionDefaults
import com.example.wallquote.domain.CollectionValidation
import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.editor.EditorDraft
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.PhotoScaleMode
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.time.minuteFromHalfHourIndex
import com.example.wallquote.domain.usecase.GetCollectionUseCase
import com.example.wallquote.domain.usecase.SaveCollectionWithBackgroundUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCollectionUseCase: GetCollectionUseCase,
    private val saveCollectionWithBackgroundUseCase: SaveCollectionWithBackgroundUseCase,
    private val assetStore: BackgroundAssetStore,
) : ViewModel() {

    private val argCollectionId: Long? =
        savedStateHandle.get<Long>("collectionId")?.takeIf { it >= 0 }

    private val _uiState = MutableStateFlow(EditorUiState(draftId = UUID.randomUUID().toString()))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    private var originalDraft: EditorDraft? = null
    private var previousBackground: BackgroundSpec? = null
    /** Negative keys for unsaved lines; persisted lines use positive DB ids as clientKey. */
    private var nextClientKey = -1L

    private fun newClientKey(): Long {
        val key = nextClientKey
        nextClientKey -= 1
        return key
    }

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
                clientKey = newClientKey(),
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
            draftId = UUID.randomUUID().toString(),
        )
    }

    private fun applyState(state: EditorUiState) {
        _uiState.value = state
        originalDraft = state.toDraft()
        previousBackground = state.backgroundSpec
        refreshPhotoPreview(state)
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
                clientKey = if (line.id != 0L) line.id else newClientKey(),
                text = line.text,
            )
        }
        val photoState = when (val bg = background) {
            is BackgroundSpec.Photo -> PhotoEditorState.Ready(previewPath = null, isStaging = false)
            else -> PhotoEditorState.Empty
        }
        return EditorUiState(
            collectionId = id,
            name = name,
            startMinute = schedule.startMinuteOfDay,
            endMinute = schedule.endMinuteOfDay,
            backgroundSpec = background,
            texts = entries.ifEmpty {
                listOf(EditorTextEntry(lineId = 0, clientKey = newClientKey(), text = ""))
            },
            textStyle = textStyle,
            transform = transform,
            sortOrder = sortOrder,
            isLoading = false,
            previewTextIndex = 0,
            selectedTab = EditorTab.Content,
            draftId = UUID.randomUUID().toString(),
            photoEditorState = photoState,
        )
    }

    private fun refreshPhotoPreview(state: EditorUiState = _uiState.value) {
        viewModelScope.launch {
            val path = when {
                state.stagedBackground != null ->
                    assetStore.resolveStagingPath(state.stagedBackground)
                state.backgroundSpec is BackgroundSpec.Photo -> {
                    val id = (state.backgroundSpec as BackgroundSpec.Photo).assetId
                    if (BackgroundValidation.isUsablePhotoAssetId(id)) {
                        assetStore.resolvePath(BackgroundAssetId(id))
                    } else {
                        null
                    }
                }
                else -> null
            }
            _uiState.update { current ->
                val photoState = when (current.backgroundSpec) {
                    is BackgroundSpec.Photo -> {
                        if (path == null && current.stagedBackground == null) {
                            PhotoEditorState.Failed("图片缺失，请重新选择")
                        } else {
                            PhotoEditorState.Ready(
                                previewPath = path,
                                isStaging = current.stagedBackground != null,
                            )
                        }
                    }
                    else -> current.photoEditorState
                }
                current.copy(resolvedPhotoPath = path, photoEditorState = photoState)
            }
        }
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
        discardStagingAsync()
        _uiState.update {
            it.copy(
                backgroundSpec = BackgroundSpec.Solid(hex),
                stagedBackground = null,
                resolvedPhotoPath = null,
                photoEditorState = PhotoEditorState.Empty,
            )
        }
    }

    fun setGradientBackground(
        startHex: String = "#2E3440",
        endHex: String = "#5E81AC",
        angleDegrees: Float = 90f,
    ) {
        discardStagingAsync()
        _uiState.update {
            it.copy(
                backgroundSpec = BackgroundSpec.Gradient(startHex, endHex, angleDegrees),
                stagedBackground = null,
                resolvedPhotoPath = null,
                photoEditorState = PhotoEditorState.Empty,
            )
        }
    }

    fun updateGradient(
        startHex: String? = null,
        endHex: String? = null,
        angleDegrees: Float? = null,
    ) {
        _uiState.update { state ->
            val current = state.backgroundSpec as? BackgroundSpec.Gradient
                ?: BackgroundSpec.Gradient("#2E3440", "#5E81AC", 90f)
            state.copy(
                backgroundSpec = current.copy(
                    startColorHex = startHex ?: current.startColorHex,
                    endColorHex = endHex ?: current.endColorHex,
                    angleDegrees = angleDegrees ?: current.angleDegrees,
                ),
            )
        }
    }

    fun swapGradientColors() {
        _uiState.update { state ->
            val current = state.backgroundSpec as? BackgroundSpec.Gradient ?: return@update state
            state.copy(
                backgroundSpec = current.copy(
                    startColorHex = current.endColorHex,
                    endColorHex = current.startColorHex,
                ),
            )
        }
    }

    fun selectPhotoKind() {
        _uiState.update { state ->
            when (state.backgroundSpec) {
                is BackgroundSpec.Photo -> state
                else -> state.copy(
                    backgroundSpec = BackgroundSpec.Photo(assetId = ""),
                    photoEditorState = PhotoEditorState.Empty,
                    resolvedPhotoPath = null,
                )
            }
        }
    }

    fun onPhotoPickerLaunched() {
        _uiState.update { it.copy(photoEditorState = PhotoEditorState.Picking) }
    }

    fun onPhotoPickerCancelled() {
        _uiState.update { state ->
            val photoState = when (state.backgroundSpec) {
                is BackgroundSpec.Photo -> {
                    if (state.resolvedPhotoPath != null || state.stagedBackground != null) {
                        PhotoEditorState.Ready(
                            previewPath = state.resolvedPhotoPath,
                            isStaging = state.stagedBackground != null,
                        )
                    } else {
                        PhotoEditorState.Empty
                    }
                }
                else -> PhotoEditorState.Empty
            }
            state.copy(photoEditorState = photoState)
        }
    }

    fun importPickedPhoto(uri: String) {
        viewModelScope.launch {
            val draftId = _uiState.value.draftId
            val previousDim = (_uiState.value.backgroundSpec as? BackgroundSpec.Photo)?.dimAmount ?: 0.25f
            val previousBlur = (_uiState.value.backgroundSpec as? BackgroundSpec.Photo)?.blurRadiusDp ?: 0f
            discardStagingAsync(wait = true)
            _uiState.update { it.copy(photoEditorState = PhotoEditorState.Importing, errorMessage = null) }
            runCatching {
                assetStore.importToStaging(uri, draftId)
            }.onSuccess { staged ->
                val path = assetStore.resolveStagingPath(staged)
                _uiState.update {
                    it.copy(
                        backgroundSpec = BackgroundSpec.Photo(
                            assetId = staged.stagingToken,
                            dimAmount = previousDim,
                            blurRadiusDp = previousBlur,
                            scaleMode = PhotoScaleMode.CenterCrop,
                        ),
                        stagedBackground = staged,
                        resolvedPhotoPath = path,
                        photoEditorState = PhotoEditorState.Ready(previewPath = path, isStaging = true),
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        photoEditorState = PhotoEditorState.Failed("图片导入失败，请重试"),
                        errorMessage = "图片导入失败，请重试",
                    )
                }
            }
        }
    }

    fun setPhotoDim(dimAmount: Float) {
        _uiState.update { state ->
            val photo = state.backgroundSpec as? BackgroundSpec.Photo ?: return@update state
            state.copy(
                backgroundSpec = photo.copy(
                    dimAmount = dimAmount.coerceIn(0f, 1f),
                ),
            )
        }
    }

    fun setPhotoBlur(blurRadiusDp: Float) {
        _uiState.update { state ->
            val photo = state.backgroundSpec as? BackgroundSpec.Photo ?: return@update state
            state.copy(
                backgroundSpec = photo.copy(
                    blurRadiusDp = blurRadiusDp.coerceIn(0f, 25f),
                ),
            )
        }
    }

    fun removePhoto() {
        discardStagingAsync()
        _uiState.update {
            it.copy(
                backgroundSpec = BackgroundSpec.Solid(CollectionDefaults.DEFAULT_SOLID_HEX),
                stagedBackground = null,
                resolvedPhotoPath = null,
                photoEditorState = PhotoEditorState.Empty,
            )
        }
    }

    fun addTextLine() {
        _uiState.update { state ->
            val newLine = EditorTextEntry(lineId = 0, clientKey = newClientKey(), text = "")
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
                listOf(EditorTextEntry(lineId = 0, clientKey = newClientKey(), text = ""))
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
            if (config.name.isBlank() || config.lines.none { it.text.isNotBlank() }) {
                _uiState.update {
                    it.copy(errorMessage = CollectionValidation.validateForSave(config) ?: "无法保存")
                }
                return@launch
            }
            if (state.stagedBackground == null) {
                BackgroundValidation.validate(config.background)?.let { error ->
                    _uiState.update { it.copy(errorMessage = error) }
                    return@launch
                }
            } else if (config.background !is BackgroundSpec.Photo) {
                _uiState.update { it.copy(errorMessage = "请先选择图片") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                saveCollectionWithBackgroundUseCase(
                    config = config,
                    previousBackground = previousBackground,
                    stagedAsset = state.stagedBackground,
                )
            }.onSuccess { id ->
                _uiState.update {
                    it.copy(
                        collectionId = id,
                        isSaving = false,
                        errorMessage = null,
                        stagedBackground = null,
                    )
                }
                previousBackground = _uiState.value.backgroundSpec
                originalDraft = _uiState.value.toDraft()
                _events.emit(EditorEvent.Finish)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message?.takeIf { msg -> msg.isNotBlank() }
                            ?: "保存失败，请重试",
                    )
                }
            }
        }
    }

    fun requestClose() {
        viewModelScope.launch {
            if (isDirty) {
                _events.emit(EditorEvent.ShowUnsavedDialog)
            } else {
                discardStagingAsync(wait = true)
                _events.emit(EditorEvent.Finish)
            }
        }
    }

    fun discardAndFinish() {
        viewModelScope.launch {
            discardStagingAsync(wait = true)
            _events.emit(EditorEvent.Finish)
        }
    }

    override fun onCleared() {
        // Do not delete staging here if user might return; orphan cleanup handles leftovers.
        super.onCleared()
    }

    private fun discardStagingAsync(wait: Boolean = false) {
        val staged = _uiState.value.stagedBackground ?: return
        val job = viewModelScope.launch {
            runCatching { assetStore.discardStaging(staged) }
        }
        if (wait) {
            // Best-effort; discard is fast IO.
        }
        @Suppress("UNUSED_VARIABLE")
        val ignored = job
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
            background = BackgroundValidation.normalize(backgroundSpec),
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
