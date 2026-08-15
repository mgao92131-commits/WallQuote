package com.example.wallquote.ui.editor

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.automatch.BackgroundSampler
import com.example.wallquote.domain.CollectionDefaults
import com.example.wallquote.domain.CollectionValidation
import com.example.wallquote.domain.automatch.AutoStyleMatcher
import com.example.wallquote.domain.automatch.autoMatchKey
import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.background.StagedBackgroundAsset
import com.example.wallquote.domain.editor.EditorDraft
import com.example.wallquote.domain.layout.MeasuredQuoteText
import com.example.wallquote.domain.layout.QuoteBlockLayoutCalculator
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.PhotoScaleMode
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.repository.RecentTextStyleRepository
import com.example.wallquote.domain.style.BuiltInTextStylePresets
import com.example.wallquote.domain.style.QuoteTransformNormalizer
import com.example.wallquote.domain.style.TextStyleNormalizer
import com.example.wallquote.domain.time.minuteFromHalfHourIndex
import com.example.wallquote.domain.usecase.GetCollectionUseCase
import com.example.wallquote.domain.usecase.SaveCollectionWithBackgroundUseCase
import com.example.wallquote.wallpaper.background.BackgroundImageProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
    private val recentTextStyleRepository: RecentTextStyleRepository,
    private val assetStore: BackgroundAssetStore,
    private val imageProcessor: BackgroundImageProcessor,
    private val backgroundSampler: BackgroundSampler,
) : ViewModel() {

    private val argCollectionId: Long? =
        savedStateHandle.get<Long>("collectionId")?.takeIf { it >= 0 }

    private val _uiState = MutableStateFlow(EditorUiState(draftId = UUID.randomUUID().toString()))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    private var originalDraft: EditorDraft? = null
    private var previousBackground: BackgroundSpec? = null
    private var nextClientKey = -1L
    private var photoImportGeneration = 0L
    private var previewProcessJob: Job? = null

    /**
     * Bumped whenever the background (or its dim/blur) changes or a new Auto Match request
     * starts, so an in-flight sampling result can detect it has become stale and be dropped
     * instead of being applied on top of a background/style the user already moved on from.
     */
    private var autoMatchGeneration = 0L

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
            viewModelScope.launch {
                val recentStyle = runCatching { recentTextStyleRepository.get() }.getOrNull()
                applyState(buildNewEditorState(recentStyle ?: TextStyleConfig()))
            }
        }
    }

    /** New collections inherit the [initialStyle] (the recently-used style, or default). */
    private fun buildNewEditorState(initialStyle: TextStyleConfig): EditorUiState {
        val entries = CollectionDefaults.defaultLines.map { line ->
            EditorTextEntry(lineId = 0, clientKey = newClientKey(), text = line.text)
        }
        return EditorUiState(
            collectionId = null,
            name = CollectionDefaults.NEW_COLLECTION_NAME,
            startMinute = 0,
            endMinute = 0,
            backgroundSpec = BackgroundSpec.Solid(CollectionDefaults.DEFAULT_SOLID_HEX),
            texts = entries,
            textStyle = initialStyle,
            isLoading = false,
            selectedPanel = null,
            draftId = UUID.randomUUID().toString(),
        )
    }

    private fun applyState(state: EditorUiState) {
        _uiState.value = state
        originalDraft = state.toDraft()
        previousBackground = state.backgroundSpec
        refreshPhotoPreview()
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
        val photoState = when (background) {
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
            selectedPanel = null,
            draftId = UUID.randomUUID().toString(),
            photoEditorState = photoState,
        )
    }

    private fun refreshPhotoPreview() {
        viewModelScope.launch {
            val state = _uiState.value
            val path = when {
                state.stagedBackground != null ->
                    assetStore.resolveStagingPath(state.stagedBackground)
                state.backgroundSpec is BackgroundSpec.Photo -> {
                    val id = (state.backgroundSpec as BackgroundSpec.Photo).assetId
                    when {
                        BackgroundValidation.isValidAssetId(id) ->
                            assetStore.resolvePath(BackgroundAssetId(id))
                        BackgroundValidation.isValidStagingToken(id) && state.stagedBackground != null ->
                            assetStore.resolveStagingPath(state.stagedBackground)
                        else -> null
                    }
                }
                else -> null
            }
            _uiState.update { current ->
                val photoState = when (val bg = current.backgroundSpec) {
                    is BackgroundSpec.Photo -> {
                        if (path == null && current.stagedBackground == null &&
                            !BackgroundValidation.isValidStagingToken(bg.assetId)
                        ) {
                            PhotoEditorState.Failed("图片缺失，请重新选择")
                        } else {
                            PhotoEditorState.Ready(
                                previewPath = path,
                                isStaging = current.stagedBackground != null,
                            )
                        }
                    }
                    else -> PhotoEditorState.Empty
                }
                current.copy(resolvedPhotoPath = path, photoEditorState = photoState)
            }
            requestProcessedPreview()
        }
    }

    private fun requestProcessedPreview() {
        previewProcessJob?.cancel()
        val state = _uiState.value
        val photo = state.backgroundSpec as? BackgroundSpec.Photo ?: run {
            _uiState.update { it.copy(processedPreviewBitmap = null) }
            return
        }
        val path = state.resolvedPhotoPath ?: return
        previewProcessJob = viewModelScope.launch {
            runCatching {
                imageProcessor.process(
                    sourcePath = path,
                    targetWidth = PREVIEW_WIDTH,
                    targetHeight = PREVIEW_HEIGHT,
                    blurRadiusDp = photo.blurRadiusDp,
                    density = 2f,
                )
            }.onSuccess { processed ->
                _uiState.update { it.copy(processedPreviewBitmap = processed.bitmap) }
            }.onFailure {
                _uiState.update { it.copy(processedPreviewBitmap = null) }
            }
        }
    }

    /**
     * Marks any pending/previewed Auto Match suggestion as stale: bumps [autoMatchGeneration] so
     * an in-flight sampling result is dropped on arrival, and clears a suggestion already being
     * previewed since it was computed for a background that no longer applies. Also clears
     * [EditorUiState.autoMatchLoading] immediately (rather than leaving it to the now-superseded
     * request) since that request's completion will see the generation mismatch and skip
     * updating loading state itself, which would otherwise leave the spinner stuck on.
     */
    private fun invalidateAutoMatch() {
        autoMatchGeneration++
        _uiState.update { it.copy(autoMatchSuggestion = null, autoMatchLoading = false) }
    }

    fun selectPanel(panel: EditorPanel) {
        if (panel == EditorPanel.Style) {
            startStyleDraft()
            return
        }
        _uiState.update { current ->
            current.copy(selectedPanel = if (current.selectedPanel == panel) null else panel)
        }
    }

    fun dismissPanel() {
        _uiState.update { it.copy(selectedPanel = null) }
    }

    fun startStyleDraft() {
        viewModelScope.launch {
            invalidateAutoMatch()
            val recents = runCatching { recentTextStyleRepository.getAll() }.getOrElse { emptyList() }
            _uiState.update { state ->
                state.copy(
                    selectedPanel = null,
                    recentStyles = recents,
                    styleDraft = StyleEditorDraft(
                        originalStyle = state.textStyle,
                        workingStyle = state.textStyle,
                    ),
                )
            }
        }
    }

    fun updateWorkingStyle(transform: (TextStyleConfig) -> TextStyleConfig) {
        invalidateAutoMatch()
        _uiState.update { state ->
            val draft = state.styleDraft ?: return@update state
            state.copy(styleDraft = draft.copy(workingStyle = transform(draft.workingStyle)))
        }
    }

    fun applyWorkingStyle(style: TextStyleConfig) {
        invalidateAutoMatch()
        _uiState.update { state ->
            val draft = state.styleDraft ?: return@update state
            state.copy(styleDraft = draft.copy(workingStyle = style))
        }
    }

    fun selectStyleTab(tab: StyleEditorTab) {
        _uiState.update { state ->
            val draft = state.styleDraft ?: return@update state
            state.copy(styleDraft = draft.copy(selectedTab = tab))
        }
    }

    fun confirmStyleDraft() {
        viewModelScope.launch {
            val draft = _uiState.value.styleDraft ?: return@launch
            invalidateAutoMatch()
            _uiState.update {
                it.copy(textStyle = draft.workingStyle, styleDraft = null)
            }
            runCatching { recentTextStyleRepository.save(draft.workingStyle) }
            val recents = runCatching { recentTextStyleRepository.getAll() }.getOrElse { emptyList() }
            _uiState.update { it.copy(recentStyles = recents) }
        }
    }

    fun discardStyleDraft() {
        invalidateAutoMatch()
        _uiState.update { it.copy(styleDraft = null) }
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
        invalidateAutoMatch()
        viewModelScope.launch {
            discardCurrentStaging()
            _uiState.update {
                it.copy(
                    backgroundSpec = BackgroundSpec.Solid(hex),
                    stagedBackground = null,
                    resolvedPhotoPath = null,
                    processedPreviewBitmap = null,
                    photoEditorState = PhotoEditorState.Empty,
                )
            }
        }
    }

    fun setGradientBackground(
        startHex: String = "#2E3440",
        endHex: String = "#5E81AC",
        angleDegrees: Float = 90f,
    ) {
        invalidateAutoMatch()
        viewModelScope.launch {
            discardCurrentStaging()
            _uiState.update {
                it.copy(
                    backgroundSpec = BackgroundSpec.Gradient(startHex, endHex, angleDegrees),
                    stagedBackground = null,
                    resolvedPhotoPath = null,
                    processedPreviewBitmap = null,
                    photoEditorState = PhotoEditorState.Empty,
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
            val generation = ++photoImportGeneration
            val oldStaging = _uiState.value.stagedBackground
            val draftId = _uiState.value.draftId
            _uiState.update { it.copy(photoEditorState = PhotoEditorState.Importing, errorMessage = null) }

            runCatching {
                assetStore.importToStaging(uri, draftId)
            }.onSuccess { newStaging ->
                if (generation != photoImportGeneration) {
                    runCatching { assetStore.discardStaging(newStaging) }
                    return@launch
                }
                val path = assetStore.resolveStagingPath(newStaging)
                invalidateAutoMatch()
                _uiState.update {
                    it.copy(
                        backgroundSpec = BackgroundSpec.Photo(
                            assetId = newStaging.stagingToken,
                            dimAmount = 0f,
                            blurRadiusDp = 0f,
                            scaleMode = PhotoScaleMode.CenterCrop,
                        ),
                        stagedBackground = newStaging,
                        resolvedPhotoPath = path,
                        photoEditorState = PhotoEditorState.Ready(previewPath = path, isStaging = true),
                    )
                }
                if (oldStaging != null && oldStaging != newStaging) {
                    runCatching { assetStore.discardStaging(oldStaging) }
                }
                requestProcessedPreview()
            }.onFailure {
                if (generation != photoImportGeneration) return@launch
                _uiState.update { state ->
                    val restored = when {
                        state.stagedBackground != null || state.resolvedPhotoPath != null ->
                            PhotoEditorState.Ready(
                                previewPath = state.resolvedPhotoPath,
                                isStaging = state.stagedBackground != null,
                            )
                        else -> PhotoEditorState.Failed("图片导入失败，请重试")
                    }
                    state.copy(
                        photoEditorState = restored,
                        errorMessage = "图片导入失败，请重试",
                    )
                }
            }
        }
    }

    fun addTextLine() {
        _uiState.update { state ->
            val newLine = EditorTextEntry(lineId = 0, clientKey = newClientKey(), text = "")
            state.copy(texts = state.texts + newLine, previewTextIndex = state.texts.size)
        }
    }

    fun updateText(clientKey: Long, text: String) {
        _uiState.update { state ->
            val index = state.texts.indexOfFirst { it.clientKey == clientKey }
            state.copy(
                texts = state.texts.map {
                    if (it.clientKey == clientKey) it.copy(text = text) else it
                },
                previewTextIndex = if (index >= 0) index else state.previewTextIndex,
            )
        }
    }

    fun deleteText(clientKey: Long) {
        _uiState.update { state ->
            if (state.texts.size <= 1) return@update state
            val deletedIndex = state.texts.indexOfFirst { it.clientKey == clientKey }
            if (deletedIndex < 0) return@update state
            val filtered = state.texts.filterNot { it.clientKey == clientKey }
            val newIndex = when {
                state.previewTextIndex > deletedIndex -> state.previewTextIndex - 1
                state.previewTextIndex == deletedIndex ->
                    state.previewTextIndex.coerceIn(0, filtered.lastIndex)
                else -> state.previewTextIndex
            }
            state.copy(texts = filtered, previewTextIndex = newIndex.coerceIn(0, filtered.lastIndex))
        }
    }

    fun selectPreviewIndex(index: Int) {
        _uiState.update { it.copy(previewTextIndex = index) }
    }

    fun updateTextStyle(transform: (TextStyleConfig) -> TextStyleConfig) {
        invalidateAutoMatch()
        _uiState.update { it.copy(textStyle = transform(it.textStyle)) }
    }

    /** Fully replaces the current (formal) style, e.g. from a built-in preset or custom style. */
    fun applyTextStyle(style: TextStyleConfig) {
        invalidateAutoMatch()
        _uiState.update { it.copy(textStyle = style) }
    }

    fun applyPreset(id: String) {
        val preset = BuiltInTextStylePresets.findById(id) ?: return
        applyTextStyle(preset.style)
    }

    fun updateTransform(transform: QuoteTransform) {
        val normalized = QuoteTransformNormalizer.normalize(
            centerXFraction = transform.centerXFraction,
            centerYFraction = transform.centerYFraction,
            rotationDegrees = transform.rotationDegrees,
        )
        _uiState.update { it.copy(transform = normalized) }
    }

    /** Records the preview area's measured size so [updateTransformRequested] can clamp against it. */
    fun setPreviewViewportSize(widthPx: Int, heightPx: Int) {
        if (widthPx <= 0 || heightPx <= 0) return
        _uiState.update { it.copy(previewViewportWidthPx = widthPx, previewViewportHeightPx = heightPx) }
    }

    /**
     * Single entry point for both gesture-driven (pan/rotate on the preview) transform edits
     * (P4-013 follow-up). [rawTransform] is the unclamped candidate; when the preview viewport
     * hasn't been measured yet, this falls back to just normalizing it.
     */
    fun updateTransformRequested(
        rawTransform: QuoteTransform,
        provisionalTextHeightPx: Float,
        density: Float,
    ) {
        val state = _uiState.value
        val width = state.previewViewportWidthPx
        val height = state.previewViewportHeightPx
        if (width <= 0 || height <= 0) {
            updateTransform(rawTransform)
            return
        }
        val style = TextStyleNormalizer.normalize(state.textStyle)
        val clamped = QuoteBlockLayoutCalculator.clampTransform(
            surfaceWidth = width,
            surfaceHeight = height,
            transform = rawTransform,
            measuredText = MeasuredQuoteText(
                widthPx = width * 0.84f,
                heightPx = provisionalTextHeightPx,
            ),
            style = style,
            density = density,
        )
        updateTransform(clamped)
    }

    fun resetCenter() {
        _uiState.update {
            it.copy(transform = it.transform.copy(centerXFraction = 0.5f, centerYFraction = 0.5f))
        }
    }

    fun resetRotation() {
        _uiState.update { it.copy(transform = it.transform.copy(rotationDegrees = 0f)) }
    }

    /**
     * Samples the current background and computes a contrast-safe suggestion for the preview
     * only ([EditorUiState.previewTextStyle]); the formal [EditorUiState.textStyle] is left
     * untouched until [confirmAutoMatch]. The result is dropped if, by the time sampling
     * completes, the request generation, background (including dim/blur), or Auto Match
     * baseline ([EditorUiState.autoMatchBaselineStyle]) have moved on.
     */
    fun requestAutoMatch() {
        val state = _uiState.value
        if (!state.isAutoMatchAvailable || state.autoMatchLoading) return
        val generation = ++autoMatchGeneration
        val requestBackgroundKey = state.backgroundSpec.autoMatchKey()
        val baseline = state.autoMatchBaselineStyle
        viewModelScope.launch {
            _uiState.update { it.copy(autoMatchLoading = true, errorMessage = null) }
            val sample = runCatching {
                backgroundSampler.sample(state.backgroundSpec, state.processedPreviewBitmap)
            }.getOrNull()

            val current = _uiState.value
            val stale = generation != autoMatchGeneration ||
                current.backgroundSpec.autoMatchKey() != requestBackgroundKey ||
                current.autoMatchBaselineStyle != baseline
            if (stale) {
                // Only clear the loading flag if this is still the most recent request; an even
                // newer request's own loading state must not be clobbered.
                if (generation == autoMatchGeneration) {
                    _uiState.update { it.copy(autoMatchLoading = false) }
                }
                return@launch
            }
            if (sample == null) {
                _uiState.update {
                    it.copy(autoMatchLoading = false, errorMessage = "自动匹配失败，请重试")
                }
                return@launch
            }
            val suggestion = AutoStyleMatcher.suggest(sample, baseline)
            _uiState.update { it.copy(autoMatchLoading = false, autoMatchSuggestion = suggestion) }
        }
    }

    /** Applies the previewed suggestion to the formal style and clears the pending preview. */
    fun confirmAutoMatch() {
        _uiState.update { state ->
            val suggestion = state.autoMatchSuggestion ?: return@update state
            val draft = state.styleDraft
            if (draft != null) {
                state.copy(
                    styleDraft = draft.copy(workingStyle = suggestion.style),
                    autoMatchSuggestion = null,
                )
            } else {
                state.copy(textStyle = suggestion.style, autoMatchSuggestion = null)
            }
        }
    }

    /** Discards the previewed suggestion; [EditorUiState.textStyle] was never mutated, so there is nothing to restore. */
    fun undoAutoMatch() {
        _uiState.update { it.copy(autoMatchSuggestion = null) }
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
            }.onSuccess { result ->
                val formalPath = when (val bg = result.savedBackground) {
                    is BackgroundSpec.Photo ->
                        assetStore.resolvePath(BackgroundAssetId(bg.assetId))
                    else -> null
                }
                _uiState.update {
                    it.copy(
                        collectionId = result.collectionId,
                        backgroundSpec = result.savedBackground,
                        stagedBackground = null,
                        resolvedPhotoPath = formalPath,
                        photoEditorState = when (result.savedBackground) {
                            is BackgroundSpec.Photo ->
                                PhotoEditorState.Ready(formalPath, isStaging = false)
                            else -> PhotoEditorState.Empty
                        },
                        isSaving = false,
                        errorMessage = null,
                    )
                }
                previousBackground = result.savedBackground
                originalDraft = _uiState.value.toDraft()
                requestProcessedPreview()
                // Recent-style memory is best-effort: a DataStore write failure must not fail
                // the (already-committed) save flow.
                runCatching { recentTextStyleRepository.save(config.textStyle) }
                _events.emit(EditorEvent.Finish)
            }.onFailure { error ->
                // Keep staging and editor draft for retry.
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
                discardCurrentStaging()
                _events.emit(EditorEvent.Finish)
            }
        }
    }

    fun discardAndFinish() {
        viewModelScope.launch {
            discardCurrentStaging()
            _events.emit(EditorEvent.Finish)
        }
    }

    private suspend fun discardCurrentStaging() {
        val staged = _uiState.value.stagedBackground ?: return
        runCatching { assetStore.discardStaging(staged) }
        _uiState.update { it.copy(stagedBackground = null) }
    }

    private fun EditorUiState.toCollectionConfig(): CollectionConfig {
        val nonBlank = texts
            .mapIndexed { index, entry ->
                QuoteLine(id = entry.lineId, text = entry.text.trim(), displayOrder = index)
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

    companion object {
        private const val PREVIEW_WIDTH = 720
        private const val PREVIEW_HEIGHT = 1280
    }
}

sealed interface EditorEvent {
    data object Finish : EditorEvent
    data object ShowUnsavedDialog : EditorEvent
}
