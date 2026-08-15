package com.example.wallquote.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.style.TextStyleNormalizer
import com.example.wallquote.ui.editor.background.BackgroundPanel
import com.example.wallquote.ui.editor.background.BackgroundPreset
import com.example.wallquote.ui.editor.content.ContentPanel
import com.example.wallquote.ui.editor.preview.EditorPreview
import com.example.wallquote.ui.editor.style.StyleEditor
import com.example.wallquote.ui.editor.time.TimePanel

@Composable
fun EditorScreen(
    collectionId: Long?,
    onFinished: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showUnsaved by remember { mutableStateOf(false) }

    BackHandler {
        when {
            state.styleDraft != null -> viewModel.discardStyleDraft()
            state.selectedPanel != null -> viewModel.dismissPanel()
            else -> viewModel.requestClose()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Finish -> onFinished()
                EditorEvent.ShowUnsavedDialog -> showUnsaved = true
            }
        }
    }

    if (showUnsaved) {
        AlertDialog(
            onDismissRequest = { showUnsaved = false },
            title = { Text("有未保存的更改") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("保存、放弃修改，或继续编辑？")
                    TextButton(
                        onClick = {
                            showUnsaved = false
                            viewModel.saveAndFinish()
                        },
                    ) { Text("保存") }
                    TextButton(
                        onClick = {
                            showUnsaved = false
                            viewModel.discardAndFinish()
                        },
                    ) { Text("放弃修改") }
                    TextButton(onClick = { showUnsaved = false }) { Text("继续编辑") }
                }
            },
            confirmButton = {},
            dismissButton = {},
        )
    }

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("确定") }
            },
        )
    }

    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) { Text("加载中…") }
    } else {
        val density = LocalDensity.current
        val provisionalTextHeightPx = with(density) {
            val normalizedStyle = TextStyleNormalizer.normalize(state.previewTextStyle)
            (normalizedStyle.textSizeSp * normalizedStyle.lineHeightMultiplier * 3).sp.toPx()
        }
        val onTransformRequested: (QuoteTransform) -> Unit = { raw ->
            viewModel.updateTransformRequested(raw, provisionalTextHeightPx, density.density)
        }
        val draft = state.styleDraft
        if (draft != null) {
            StyleEditor(
                draft = draft,
                previewInput = state.toPreviewInput(),
                processedPhotoBitmap = state.processedPreviewBitmap,
                recentStyles = state.recentStyles,
                autoMatchAvailable = state.isAutoMatchAvailable,
                autoMatchLoading = state.autoMatchLoading,
                autoMatchSuggestion = state.autoMatchSuggestion,
                onWorkingStyleChange = viewModel::updateWorkingStyle,
                onApplyRecent = viewModel::applyWorkingStyle,
                onSelectTab = viewModel::selectStyleTab,
                onConfirm = viewModel::confirmStyleDraft,
                onDiscard = viewModel::discardStyleDraft,
                onRequestAutoMatch = viewModel::requestAutoMatch,
                onConfirmAutoMatch = viewModel::confirmAutoMatch,
                onUndoAutoMatch = viewModel::undoAutoMatch,
            )
        } else {
            EditorScaffold(
                selectedPanel = state.selectedPanel,
                onSelectPanel = viewModel::selectPanel,
                onDismissPanel = viewModel::dismissPanel,
                onClose = viewModel::requestClose,
                preview = {
                    EditorPreview(
                        state = state,
                        onTransformChange = onTransformRequested,
                        onViewportSizeChanged = viewModel::setPreviewViewportSize,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                panelContent = { panel ->
                    EditorPanelBody(
                        panel = panel,
                        state = state,
                        viewModel = viewModel,
                    )
                },
            )
        }
    }
}

@Composable
private fun EditorPanelBody(
    panel: EditorPanel,
    state: EditorUiState,
    viewModel: EditorViewModel,
) {
    when (panel) {
        EditorPanel.Time -> TimePanel(
            startMinute = state.startMinute,
            endMinute = state.endMinute,
            onStartSlotChange = viewModel::setStartHalfHourIndex,
            onEndSlotChange = viewModel::setEndHalfHourIndex,
        )
        EditorPanel.Background -> BackgroundPanel(
            state = state,
            onApplyPreset = { preset -> applyBackgroundPreset(viewModel, preset) },
            onSelectSolidKind = {
                viewModel.setSolidBackground(
                    (state.backgroundSpec as? BackgroundSpec.Solid)?.colorHex
                        ?: "#2E3440",
                )
            },
            onSelectGradientKind = {
                val g = state.backgroundSpec as? BackgroundSpec.Gradient
                viewModel.setGradientBackground(
                    startHex = g?.startColorHex ?: "#2E3440",
                    endHex = g?.endColorHex ?: "#5E81AC",
                    angleDegrees = g?.angleDegrees ?: 90f,
                )
            },
            onSelectPhotoKind = viewModel::selectPhotoKind,
            onSolidSelected = viewModel::setSolidBackground,
            onGradientChange = viewModel::updateGradient,
            onSwapGradient = viewModel::swapGradientColors,
            onPickPhoto = viewModel::onPhotoPickerLaunched,
            onPickCancelled = viewModel::onPhotoPickerCancelled,
            onPhotoPicked = viewModel::importPickedPhoto,
            onDimChange = viewModel::setPhotoDim,
            onBlurChange = viewModel::setPhotoBlur,
            onRemovePhoto = viewModel::removePhoto,
        )
        EditorPanel.Content -> ContentPanel(
            texts = state.texts,
            previewIndex = state.previewTextIndex,
            onAdd = viewModel::addTextLine,
            onUpdate = viewModel::updateText,
            onDelete = viewModel::deleteText,
            onSelectPreview = viewModel::selectPreviewIndex,
        )
        EditorPanel.Style -> Unit
    }
}

private fun applyBackgroundPreset(viewModel: EditorViewModel, preset: BackgroundPreset) {
    when (val spec = preset.spec) {
        is BackgroundSpec.Solid -> viewModel.setSolidBackground(spec.colorHex)
        is BackgroundSpec.Gradient -> viewModel.setGradientBackground(
            startHex = spec.startColorHex,
            endHex = spec.endColorHex,
            angleDegrees = spec.angleDegrees,
        )
        is BackgroundSpec.Photo -> Unit
    }
}
