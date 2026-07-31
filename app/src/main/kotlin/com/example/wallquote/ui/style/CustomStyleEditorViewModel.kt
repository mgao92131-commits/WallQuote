package com.example.wallquote.ui.style

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.usecase.GetCustomStyleUseCase
import com.example.wallquote.domain.usecase.SaveCustomStyleUseCase
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

/** Comparable snapshot for dirty-state detection, mirroring [com.example.wallquote.domain.editor.EditorDraft]. */
data class CustomStyleDraft(
    val name: String,
    val style: TextStyleConfig,
)

data class CustomStyleEditorUiState(
    val id: Long? = null,
    val name: String = "",
    val style: TextStyleConfig = TextStyleConfig(),
    val selectedSection: StyleSection = StyleSection.Text,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSave: Boolean
        get() = name.isNotBlank() && !isSaving && !isLoading

    fun toDraft(): CustomStyleDraft = CustomStyleDraft(name = name.trim(), style = style)
}

sealed interface CustomStyleEditorEvent {
    data object Finish : CustomStyleEditorEvent
    data object ShowUnsavedDialog : CustomStyleEditorEvent
}

@HiltViewModel
class CustomStyleEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCustomStyleUseCase: GetCustomStyleUseCase,
    private val saveCustomStyleUseCase: SaveCustomStyleUseCase,
) : ViewModel() {

    private val styleId: Long? = savedStateHandle.get<Long>("styleId")?.takeIf { it >= 0 }

    private val _uiState = MutableStateFlow(CustomStyleEditorUiState(isLoading = styleId != null))
    val uiState: StateFlow<CustomStyleEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CustomStyleEditorEvent>()
    val events: SharedFlow<CustomStyleEditorEvent> = _events.asSharedFlow()

    /** Snapshot taken right after load (or at creation, for a new style) to detect unsaved edits. */
    private var originalDraft: CustomStyleDraft? = null

    val isDirty: Boolean
        get() {
            val baseline = originalDraft ?: return false
            return _uiState.value.toDraft() != baseline
        }

    init {
        val id = styleId
        if (id != null) {
            viewModelScope.launch {
                val existing = runCatching { getCustomStyleUseCase(id) }.getOrNull()
                if (existing == null) {
                    _events.emit(CustomStyleEditorEvent.Finish)
                } else {
                    _uiState.update {
                        it.copy(
                            id = existing.id,
                            name = existing.name,
                            style = existing.style,
                            isLoading = false,
                        )
                    }
                    originalDraft = _uiState.value.toDraft()
                }
            }
        } else {
            originalDraft = _uiState.value.toDraft()
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, errorMessage = null) }
    }

    fun selectSection(section: StyleSection) {
        _uiState.update { it.copy(selectedSection = section) }
    }

    fun updateStyle(transform: (TextStyleConfig) -> TextStyleConfig) {
        _uiState.update { it.copy(style = transform(it.style)) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Saves and, on success, finishes the screen; on failure the draft/error stay so the user can retry. */
    fun saveAndFinish() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.name.isBlank()) {
                _uiState.update { it.copy(errorMessage = "样式名称不能为空") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                saveCustomStyleUseCase(
                    CustomTextStyle(id = state.id ?: 0, name = state.name, style = state.style),
                )
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false) }
                originalDraft = _uiState.value.toDraft()
                _events.emit(CustomStyleEditorEvent.Finish)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message?.takeIf { msg -> msg.isNotBlank() } ?: "保存失败，请重试",
                    )
                }
            }
        }
    }

    /** Back press / nav-up: prompt to save if dirty, otherwise finish immediately. */
    fun requestClose() {
        viewModelScope.launch {
            if (isDirty) {
                _events.emit(CustomStyleEditorEvent.ShowUnsavedDialog)
            } else {
                _events.emit(CustomStyleEditorEvent.Finish)
            }
        }
    }

    fun discardAndFinish() {
        viewModelScope.launch { _events.emit(CustomStyleEditorEvent.Finish) }
    }
}
