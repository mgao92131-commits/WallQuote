package com.example.wallquote.ui.editor

import com.example.wallquote.domain.editor.EditorDraft
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig

enum class EditorTab {
    Time,
    Background,
    Content,
    Style,
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
    val selectedTab: EditorTab? = EditorTab.Content,
    val previewTextIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && texts.any { it.text.isNotBlank() } && !isSaving

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
