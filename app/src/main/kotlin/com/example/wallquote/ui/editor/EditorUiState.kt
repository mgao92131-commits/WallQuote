package com.example.wallquote.ui.editor

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.TextStyleConfig

enum class EditorTab {
    Time,
    Background,
    Content,
    Style,
}

data class EditorTextEntry(
    val localId: Long,
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
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val sortOrder: Int = 0,
    val selectedTab: EditorTab? = EditorTab.Content,
    val previewTextIndex: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasPersistedOnce: Boolean = false,
) {
    val schedule: DailyTimeRange
        get() = DailyTimeRange(startMinute, endMinute)
}
