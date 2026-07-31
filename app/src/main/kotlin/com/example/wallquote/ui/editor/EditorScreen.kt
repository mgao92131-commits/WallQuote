package com.example.wallquote.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.domain.automatch.TextStyleSuggestion
import com.example.wallquote.domain.layout.QuoteGestureTransformer
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.style.BuiltInTextStylePresets
import com.example.wallquote.domain.style.TextStyleNormalizer
import com.example.wallquote.ui.style.StyleSection
import com.example.wallquote.ui.style.StyleSectionContent
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    collectionId: Long?,
    onFinished: () -> Unit,
    onManageCustomStyles: () -> Unit = {},
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showUnsaved by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.requestClose()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.collectionId == null) "新建收藏集" else "编辑收藏集") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.requestClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveAndFinish() },
                        enabled = state.canSave,
                    ) {
                        Text("保存")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("加载中…") }
        } else {
            val density = LocalDensity.current
            // Single unified transform-update path (P4-013 follow-up): both the gesture drag in
            // EditorPreviewArea and the sliders in LayoutAdjustControls funnel their *raw*
            // transform through this same lambda, which delegates to
            // EditorViewModel.updateTransformRequested to clamp via
            // QuoteBlockLayoutCalculator.clampTransform. Previously only the gesture path
            // clamped against the rotated text bounding box; the sliders called
            // viewModel::updateTransform directly and only relied on the 0..1 slider range,
            // which does not account for rotation or text size.
            val provisionalTextHeightPx = with(density) {
                val normalizedStyle = TextStyleNormalizer.normalize(state.textStyle)
                (normalizedStyle.textSizeSp * normalizedStyle.lineHeightMultiplier * 3).sp.toPx()
            }
            val onTransformRequested: (QuoteTransform) -> Unit = { raw ->
                viewModel.updateTransformRequested(raw, provisionalTextHeightPx, density.density)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EditorPreviewArea(
                    state = state,
                    onLayoutAdjustToggle = viewModel::setLayoutAdjustEnabled,
                    onTransformChange = onTransformRequested,
                    onViewportSizeChanged = viewModel::setPreviewViewportSize,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                if (state.layoutAdjustEnabled) {
                    LayoutAdjustControls(
                        transform = state.transform,
                        onTransformChange = onTransformRequested,
                        onResetCenter = viewModel::resetCenter,
                        onResetRotation = viewModel::resetRotation,
                    )
                }
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text("收藏集名称") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                )
                EditorTabBar(
                    selected = state.selectedTab,
                    onSelect = viewModel::selectTab,
                )
                when (state.selectedTab) {
                    EditorTab.Time -> CircularTimePicker(
                        startMinute = state.startMinute,
                        endMinute = state.endMinute,
                        onStartSlotChange = viewModel::setStartHalfHourIndex,
                        onEndSlotChange = viewModel::setEndHalfHourIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                    EditorTab.Background -> BackgroundTabContent(
                        state = state,
                        onSolidSelected = viewModel::setSolidBackground,
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
                        onGradientChange = viewModel::updateGradient,
                        onSwapGradient = viewModel::swapGradientColors,
                        onPickPhoto = viewModel::onPhotoPickerLaunched,
                        onPickCancelled = viewModel::onPhotoPickerCancelled,
                        onPhotoPicked = viewModel::importPickedPhoto,
                        onDimChange = viewModel::setPhotoDim,
                        onBlurChange = viewModel::setPhotoBlur,
                        onRemovePhoto = viewModel::removePhoto,
                    )
                    EditorTab.Content -> ContentTabContent(
                        texts = state.texts,
                        previewIndex = state.previewTextIndex,
                        onAdd = viewModel::addTextLine,
                        onUpdate = viewModel::updateText,
                        onDelete = viewModel::deleteText,
                        onSelectPreview = viewModel::selectPreviewIndex,
                    )
                    EditorTab.Style -> StyleTabContent(
                        style = state.textStyle,
                        previewInputBase = state.toPreviewInput(),
                        processedPhotoBitmap = state.processedPreviewBitmap,
                        customStyles = state.customStyles,
                        autoMatchAvailable = state.isAutoMatchAvailable,
                        autoMatchLoading = state.autoMatchLoading,
                        autoMatchSuggestion = state.autoMatchSuggestion,
                        onStyleChange = viewModel::updateTextStyle,
                        onApplyPreset = viewModel::applyPreset,
                        onApplyCustomStyle = viewModel::applyCustomStyle,
                        onManageCustomStyles = onManageCustomStyles,
                        onSaveAsCustomStyle = viewModel::saveCurrentStyleAsCustom,
                        onRequestAutoMatch = viewModel::requestAutoMatch,
                        onConfirmAutoMatch = viewModel::confirmAutoMatch,
                        onUndoAutoMatch = viewModel::undoAutoMatch,
                    )
                    null -> Unit
                }
            }
        }
    }
}

@Composable
private fun EditorTabBar(
    selected: EditorTab?,
    onSelect: (EditorTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        EditorTab.entries.forEach { tab ->
            FilterChip(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                label = {
                    Text(
                        when (tab) {
                            EditorTab.Time -> "时间"
                            EditorTab.Background -> "背景"
                            EditorTab.Content -> "内容"
                            EditorTab.Style -> "样式"
                        },
                    )
                },
            )
        }
    }
}

private fun EditorUiState.toPreviewInput(): QuoteRenderInput {
    val quoteLines = texts.mapIndexed { index, entry ->
        QuoteLine(id = entry.lineId, text = entry.text, displayOrder = index)
    }
    return QuoteRenderInput(
        background = backgroundSpec,
        lines = quoteLines,
        previewLineIndex = previewTextIndex,
        // The preview must show a pending Auto Match suggestion without mutating the formal
        // textStyle (see EditorUiState.previewTextStyle / P4-005).
        textStyle = previewTextStyle,
        transform = transform,
    )
}

/**
 * Wraps [QuotePreview] with an optional "layout adjust" overlay: a drag surface that moves the
 * quote's pivot, plus a toggle chip in the corner. Emits the *raw* (unclamped) transform via
 * [onTransformChange]; clamping against the rotated text bounding box happens once, in
 * [EditorViewModel.updateTransformRequested], shared with the [LayoutAdjustControls] sliders.
 */
@Composable
private fun EditorPreviewArea(
    state: EditorUiState,
    onLayoutAdjustToggle: (Boolean) -> Unit,
    onTransformChange: (QuoteTransform) -> Unit,
    onViewportSizeChanged: (widthPx: Int, heightPx: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestState = rememberUpdatedState(state)
    Box(
        modifier = modifier.onSizeChanged { onViewportSizeChanged(it.width, it.height) },
    ) {
        QuotePreview(
            state = state.toPreviewInput(),
            modifier = Modifier.fillMaxSize(),
            processedPhotoBitmap = state.processedPreviewBitmap,
        )
        if (state.layoutAdjustEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, _, rotation ->
                            val widthPx = size.width
                            val heightPx = size.height
                            if (widthPx <= 0 || heightPx <= 0) return@detectTransformGestures
                            val current = latestState.value
                            // `rotation` from detectTransformGestures is already in degrees; do
                            // not re-convert with Math.toDegrees (that treated it as radians and
                            // produced wildly exaggerated rotation). QuoteGestureTransformer is
                            // the shared domain helper for this pan+rotate -> QuoteTransform math.
                            val rawTransform = QuoteGestureTransformer.apply(
                                current = current.transform,
                                panX = pan.x,
                                panY = pan.y,
                                viewportWidth = widthPx,
                                viewportHeight = heightPx,
                                rotationDeltaDegrees = rotation,
                            )
                            onTransformChange(rawTransform)
                        }
                    },
            )
        }
        FilterChip(
            selected = state.layoutAdjustEnabled,
            onClick = { onLayoutAdjustToggle(!state.layoutAdjustEnabled) },
            label = { Text("调整布局") },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        )
    }
}

/**
 * Sliders for pivot position and rotation. Each `onValueChange` emits the *raw* candidate
 * transform (0..1 slider range for position); [onTransformChange] is expected to clamp it the
 * same way the drag gesture in [EditorPreviewArea] does (see
 * [EditorViewModel.updateTransformRequested]) so the block can't be dragged fully off-screen via
 * the sliders either.
 */
@Composable
private fun LayoutAdjustControls(
    transform: QuoteTransform,
    onTransformChange: (QuoteTransform) -> Unit,
    onResetCenter: () -> Unit,
    onResetRotation: () -> Unit,
) {
    val displayRotation = if (transform.rotationDegrees > 180f) transform.rotationDegrees - 360f else transform.rotationDegrees
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text("水平位置：${(transform.centerXFraction * 100).roundToInt()}%")
        Slider(
            value = transform.centerXFraction,
            onValueChange = { onTransformChange(transform.copy(centerXFraction = it)) },
            valueRange = 0f..1f,
        )
        Text("垂直位置：${(transform.centerYFraction * 100).roundToInt()}%")
        Slider(
            value = transform.centerYFraction,
            onValueChange = { onTransformChange(transform.copy(centerYFraction = it)) },
            valueRange = 0f..1f,
        )
        Text("旋转：${displayRotation.roundToInt()}°")
        Slider(
            value = displayRotation,
            onValueChange = { onTransformChange(transform.copy(rotationDegrees = it)) },
            valueRange = -180f..180f,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onResetCenter) { Text("恢复居中") }
            TextButton(onClick = onResetRotation) { Text("旋转归零") }
        }
    }
}

private val SolidBackgroundPresets = listOf(
    "#000000" to "纯黑",
    "#2E3440" to "暗夜紫",
    "#3B4252" to "灰蓝",
    "#BF616A" to "赤陶",
)

private val GradientPresets = listOf(
    Triple("#2E3440", "#5E81AC", 90f),
    Triple("#BF616A", "#EBCB8B", 45f),
    Triple("#A3BE8C", "#88C0D0", 135f),
)

@Composable
private fun BackgroundTabContent(
    state: EditorUiState,
    onSolidSelected: (String) -> Unit,
    onSelectSolidKind: () -> Unit,
    onSelectGradientKind: () -> Unit,
    onSelectPhotoKind: () -> Unit,
    onGradientChange: (startHex: String?, endHex: String?, angleDegrees: Float?) -> Unit,
    onSwapGradient: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickCancelled: () -> Unit,
    onPhotoPicked: (String) -> Unit,
    onDimChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) {
            onPickCancelled()
        } else {
            onPhotoPicked(uri.toString())
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.backgroundKind == BackgroundKind.Solid,
                onClick = onSelectSolidKind,
                label = { Text("纯色") },
            )
            FilterChip(
                selected = state.backgroundKind == BackgroundKind.Gradient,
                onClick = onSelectGradientKind,
                label = { Text("渐变") },
            )
            FilterChip(
                selected = state.backgroundKind == BackgroundKind.Photo,
                onClick = onSelectPhotoKind,
                label = { Text("图片") },
            )
        }

        when (val bg = state.backgroundSpec) {
            is BackgroundSpec.Solid -> {
                Text("预设颜色")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SolidBackgroundPresets.forEach { (hex, label) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .fillMaxWidth(0.2f)
                                    .clip(CircleShape)
                                    .background(parseColorHex(hex))
                                    .border(
                                        width = if (bg.colorHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    )
                                    .clickable { onSolidSelected(hex) },
                            )
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            is BackgroundSpec.Gradient -> {
                Text("渐变预设")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GradientPresets.forEach { (start, end, angle) ->
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .weight(1f)
                                .clip(CircleShape)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        listOf(parseColorHex(start), parseColorHex(end)),
                                    ),
                                )
                                .clickable {
                                    onGradientChange(start, end, angle)
                                },
                        )
                    }
                }
                Text("起始 / 结束颜色")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        bg.startColorHex to true,
                        bg.endColorHex to false,
                    ).forEach { (hex, isStart) ->
                        SolidBackgroundPresets.take(4).forEach { (preset, _) ->
                            Box(
                                modifier = Modifier
                                    .height(28.dp)
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(parseColorHex(preset))
                                    .border(
                                        width = if (hex.equals(preset, true)) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    )
                                    .clickable {
                                        if (isStart) onGradientChange(preset, null, null)
                                        else onGradientChange(null, preset, null)
                                    },
                            )
                        }
                    }
                }
                TextButton(onClick = onSwapGradient) { Text("交换颜色") }
                Text("角度：${bg.angleDegrees.toInt()}°")
                Slider(
                    value = bg.angleDegrees,
                    onValueChange = { onGradientChange(null, null, it) },
                    valueRange = 0f..360f,
                )
            }
            is BackgroundSpec.Photo -> {
                when (val photoState = state.photoEditorState) {
                    PhotoEditorState.Importing -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.height(24.dp))
                            Text("正在导入图片…", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    is PhotoEditorState.Failed -> Text(photoState.message)
                    else -> Unit
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onPickPhoto()
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    ) {
                        Text(if (state.resolvedPhotoPath == null) "选择图片" else "替换图片")
                    }
                    if (state.resolvedPhotoPath != null || state.stagedBackground != null) {
                        TextButton(onClick = onRemovePhoto) { Text("移除") }
                    }
                }
                Text("暗化：${(bg.dimAmount * 100).toInt()}%")
                Slider(
                    value = bg.dimAmount,
                    onValueChange = onDimChange,
                    valueRange = 0f..1f,
                )
                Text("模糊：${bg.blurRadiusDp.toInt()} dp")
                Slider(
                    value = bg.blurRadiusDp,
                    onValueChange = onBlurChange,
                    valueRange = 0f..25f,
                )
            }
        }
    }
}

@Composable
private fun ContentTabContent(
    texts: List<EditorTextEntry>,
    previewIndex: Int,
    onAdd: () -> Unit,
    onUpdate: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onSelectPreview: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(texts, key = { _, item -> item.clientKey }) { index, entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = entry.text,
                        onValueChange = { onUpdate(entry.clientKey, it) },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectPreview(index) }
                            .then(
                                if (index == previewIndex) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    Modifier
                                },
                            ),
                        label = { Text("名言 ${index + 1}") },
                    )
                    IconButton(onClick = { onDelete(entry.clientKey) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除行")
                    }
                }
            }
        }
        Button(onClick = onAdd) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("添加一行")
        }
    }
}

@Composable
private fun StyleTabContent(
    style: TextStyleConfig,
    previewInputBase: QuoteRenderInput,
    processedPhotoBitmap: Bitmap?,
    customStyles: List<CustomTextStyle>,
    autoMatchAvailable: Boolean,
    autoMatchLoading: Boolean,
    autoMatchSuggestion: TextStyleSuggestion?,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
    onApplyPreset: (String) -> Unit,
    onApplyCustomStyle: (Long) -> Unit,
    onManageCustomStyles: () -> Unit,
    onSaveAsCustomStyle: (String) -> Unit,
    onRequestAutoMatch: () -> Unit,
    onConfirmAutoMatch: () -> Unit,
    onUndoAutoMatch: () -> Unit,
) {
    var section by remember { mutableStateOf(StyleSection.Text) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var saveAsName by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("预设样式", style = MaterialTheme.typography.titleSmall)
            // Visual cards (P4-010): each renders the real QuotePreview with the current
            // background/text/photo so the user can compare presets against their own content,
            // rather than an abstract FilterChip label.
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BuiltInTextStylePresets.all, key = { it.id }) { preset ->
                    PresetStyleCard(
                        displayName = preset.displayName,
                        selected = style == preset.style,
                        previewInput = previewInputBase.copy(textStyle = preset.style),
                        processedPhotoBitmap = processedPhotoBitmap,
                        onClick = { onApplyPreset(preset.id) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("自定义样式", style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(onClick = { showSaveAsDialog = true }) { Text("另存为") }
                    TextButton(onClick = onManageCustomStyles) { Text("管理") }
                }
            }
            if (customStyles.isEmpty()) {
                Text("暂无自定义样式", style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(customStyles, key = { it.id }) { custom ->
                        FilterChip(
                            selected = style == custom.style,
                            onClick = { onApplyCustomStyle(custom.id) },
                            label = { Text(custom.name) },
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("自动匹配背景", style = MaterialTheme.typography.titleSmall)
            Button(onClick = onRequestAutoMatch, enabled = autoMatchAvailable && !autoMatchLoading) {
                if (autoMatchLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Text("正在分析…", modifier = Modifier.padding(start = 8.dp))
                } else {
                    Text(if (autoMatchAvailable) "自动匹配文字样式" else "等待图片处理完成…")
                }
            }
            if (autoMatchSuggestion != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(autoMatchSuggestion.reason, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = onConfirmAutoMatch) { Text("应用") }
                            TextButton(onClick = onUndoAutoMatch) { Text("撤销") }
                        }
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleSection.entries.forEach { s ->
                FilterChip(
                    selected = section == s,
                    onClick = { section = s },
                    label = { Text(s.label) },
                )
            }
        }

        StyleSectionContent(section = section, style = style, onStyleChange = onStyleChange)
    }

    if (showSaveAsDialog) {
        AlertDialog(
            onDismissRequest = { showSaveAsDialog = false },
            title = { Text("另存为自定义样式") },
            text = {
                OutlinedTextField(
                    value = saveAsName,
                    onValueChange = { saveAsName = it },
                    label = { Text("样式名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSaveAsCustomStyle(saveAsName)
                        showSaveAsDialog = false
                        saveAsName = ""
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveAsDialog = false }) { Text("取消") }
            },
        )
    }
}

/** ~120dp-tall preview card for a built-in style preset (P4-010). */
@Composable
private fun PresetStyleCard(
    displayName: String,
    selected: Boolean,
    previewInput: QuoteRenderInput,
    processedPhotoBitmap: Bitmap?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(96.dp)
            .height(120.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            QuotePreview(
                state = previewInput,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                processedPhotoBitmap = processedPhotoBitmap,
            )
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 2.dp),
            )
        }
    }
}

