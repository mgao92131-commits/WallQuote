package com.example.wallquote.ui.editor

import com.example.wallquote.domain.automatch.TextStyleSuggestion
import com.example.wallquote.domain.background.StagedBackgroundAsset
import com.example.wallquote.domain.editor.EditorDraft
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig

enum class EditorPanel {
    Time,
    Background,
    Content,
    Style,
}

enum class StyleEditorTab {
    Text,
    Border,
    Shadow,
    Block,
    Align,
}

data class StyleEditorDraft(
    val originalStyle: TextStyleConfig,
    val workingStyle: TextStyleConfig,
    val selectedTab: StyleEditorTab = StyleEditorTab.Text,
)

enum class BackgroundKind {
    Solid,
    Gradient,
    Photo,
}

sealed interface PhotoEditorState {
    data object Empty : PhotoEditorState
    data object Picking : PhotoEditorState
    data object Importing : PhotoEditorState
    data class Ready(
        val previewPath: String?,
        val isStaging: Boolean,
    ) : PhotoEditorState

    data class Failed(val message: String) : PhotoEditorState
}

data class EditorTextEntry(
    /** Database line id; 0 means not yet persisted. */
    val lineId: Long = 0,
    /** Stable Compose list key for new rows. */
    val clientKey: Long,
    val text: String,
)

data class EditorUiState(
    val collectionId: Long? = null,
    val name: String = "",
    val startMinute: Int = 0,
    val endMinute: Int = 0,
    val backgroundSpec: BackgroundSpec = BackgroundSpec.Solid("#2E3440"),
    val texts: List<EditorTextEntry> = emptyList(),
    val textStyle: TextStyleConfig = TextStyleConfig(),
    val transform: QuoteTransform = QuoteTransform(),
    val sortOrder: Int = 0,
    /** Currently expanded bottom panel; `null` means only the dock is visible. */
    val selectedPanel: EditorPanel? = null,
    val previewTextIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val draftId: String = "",
    val stagedBackground: StagedBackgroundAsset? = null,
    val photoEditorState: PhotoEditorState = PhotoEditorState.Empty,
    val resolvedPhotoPath: String? = null,
    /** Shared CenterCrop+Blur result for editor preview (matches wallpaper pipeline). */
    val processedPreviewBitmap: android.graphics.Bitmap? = null,
    /** Sampling + suggestion in progress for the "Auto Match" flow. */
    val autoMatchLoading: Boolean = false,
    /**
     * Suggestion currently being previewed. [textStyle] itself is never mutated by a pending
     * suggestion; use [previewTextStyle] for anything the user sees on screen.
     */
    val autoMatchSuggestion: TextStyleSuggestion? = null,
    /**
     * Last measured size (px) of the preview area, reported by `EditorPreview`.
     * Not part of [toDraft] (purely a UI layout fact, not saved content); used by
     * [EditorViewModel.updateTransformRequested] to clamp slider- and gesture-driven
     * transform edits identically (P4-013 follow-up). Zero until the preview area has
     * been measured at least once.
     */
    val previewViewportWidthPx: Int = 0,
    val previewViewportHeightPx: Int = 0,
    val styleDraft: StyleEditorDraft? = null,
    val recentStyles: List<TextStyleConfig> = emptyList(),
) {
    /** What the preview (and only the preview) should render. */
    val previewTextStyle: TextStyleConfig
        get() = autoMatchSuggestion?.style ?: styleDraft?.workingStyle ?: textStyle

    val canSave: Boolean
        get() = name.isNotBlank() && texts.any { it.text.isNotBlank() } && !isSaving &&
            photoEditorState !is PhotoEditorState.Importing

    val backgroundKind: BackgroundKind
        get() = when (backgroundSpec) {
            is BackgroundSpec.Solid -> BackgroundKind.Solid
            is BackgroundSpec.Gradient -> BackgroundKind.Gradient
            is BackgroundSpec.Photo -> BackgroundKind.Photo
        }

    /** Auto Match needs a decoded preview bitmap for photo backgrounds; solid/gradient are always ready. */
    val isAutoMatchAvailable: Boolean
        get() = when (backgroundSpec) {
            is BackgroundSpec.Photo -> processedPreviewBitmap != null
            else -> true
        }

    fun toPreviewInput(): QuoteRenderInput {
        val quoteLines = texts.mapIndexed { index, entry ->
            QuoteLine(id = entry.lineId, text = entry.text, displayOrder = index)
        }
        return QuoteRenderInput(
            background = backgroundSpec,
            lines = quoteLines,
            previewLineIndex = previewTextIndex,
            // The preview must show a pending Auto Match suggestion without mutating the formal
            // textStyle (see previewTextStyle / P4-005).
            textStyle = previewTextStyle,
            transform = transform,
        )
    }

    fun toDraft(): EditorDraft =
        EditorDraft(
            name = name.trim(),
            startMinute = startMinute,
            endMinute = endMinute,
            background = backgroundSpec,
            orderedLines = texts.map {
                EditorDraft.LineDraft(persistentLineId = it.lineId, text = it.text)
            },
            textStyle = textStyle,
            transform = transform,
        )
}
