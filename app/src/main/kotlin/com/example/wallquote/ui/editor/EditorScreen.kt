package com.example.wallquote.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.domain.automatch.TextStyleSuggestion
import com.example.wallquote.domain.layout.MeasuredQuoteText
import com.example.wallquote.domain.layout.QuoteBlockLayoutCalculator
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.style.BuiltInTextStylePresets
import com.example.wallquote.domain.style.QuoteTransformNormalizer
import com.example.wallquote.domain.style.TextStyleNormalizer
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                EditorPreviewArea(
                    state = state,
                    onLayoutAdjustToggle = viewModel::setLayoutAdjustEnabled,
                    onTransformChange = viewModel::updateTransform,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                if (state.layoutAdjustEnabled) {
                    LayoutAdjustControls(
                        transform = state.transform,
                        onTransformChange = viewModel::updateTransform,
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
        textStyle = textStyle,
        transform = transform,
    )
}

/**
 * Wraps [QuotePreview] with an optional "layout adjust" overlay: a drag surface that moves the
 * quote's pivot, clamped via [QuoteBlockLayoutCalculator.clampTransform] so it can't drag fully
 * off-screen, plus a toggle chip in the corner.
 */
@Composable
private fun EditorPreviewArea(
    state: EditorUiState,
    onLayoutAdjustToggle: (Boolean) -> Unit,
    onTransformChange: (QuoteTransform) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestState = rememberUpdatedState(state)
    val density = LocalDensity.current
    Box(modifier = modifier) {
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
                            val style = TextStyleNormalizer.normalize(current.textStyle)
                            val dxFraction = pan.x / widthPx
                            val dyFraction = pan.y / heightPx
                            val rawTransform = QuoteTransformNormalizer.normalize(
                                centerXFraction = current.transform.centerXFraction + dxFraction,
                                centerYFraction = current.transform.centerYFraction + dyFraction,
                                rotationDegrees = current.transform.rotationDegrees +
                                    Math.toDegrees(rotation.toDouble()).toFloat(),
                            )
                            val provisionalHeightPx = with(density) {
                                (style.textSizeSp * style.lineHeightMultiplier * 3).sp.toPx()
                            }
                            val clamped = QuoteBlockLayoutCalculator.clampTransform(
                                surfaceWidth = widthPx,
                                surfaceHeight = heightPx,
                                transform = rawTransform,
                                measuredText = MeasuredQuoteText(
                                    widthPx = widthPx * 0.84f,
                                    heightPx = provisionalHeightPx,
                                ),
                                style = style,
                                density = density.density,
                            )
                            onTransformChange(clamped)
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

private enum class StyleSection(val label: String) {
    Text("文字"),
    Block("文字块"),
    Border("边框"),
    Shadow("阴影"),
    Align("对齐"),
}

private val TextColorPresets = listOf("#FFFFFF", "#1A1A1A", "#FFD54F", "#A3BE8C", "#88C0D0")
private val BlockColorPresets = listOf("#000000", "#FFFFFF", "#2E3440", "#3B4252")
private val ShadowColorPresets = listOf("#000000", "#FFFFFF")
private val FontFamilyOptions = listOf(
    SystemFontFamily.Serif to "衬线",
    SystemFontFamily.SansSerif to "无衬线",
    SystemFontFamily.Monospace to "等宽",
    SystemFontFamily.Cursive to "手写体",
)

@Composable
private fun StyleTabContent(
    style: TextStyleConfig,
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BuiltInTextStylePresets.all, key = { it.id }) { preset ->
                    FilterChip(
                        selected = style == preset.style,
                        onClick = { onApplyPreset(preset.id) },
                        label = { Text(preset.displayName) },
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

        when (section) {
            StyleSection.Text -> TextStyleTextSection(style, onStyleChange)
            StyleSection.Block -> TextStyleBlockSection(style, onStyleChange)
            StyleSection.Border -> TextStyleBorderSection(style, onStyleChange)
            StyleSection.Shadow -> TextStyleShadowSection(style, onStyleChange)
            StyleSection.Align -> TextStyleAlignSection(style, onStyleChange)
        }
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

@Composable
private fun ColorSwatchRow(
    selectedHex: String?,
    colors: List<String>,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { hex ->
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(32.dp)
                    .clip(CircleShape)
                    .background(parseColorHex(hex))
                    .border(
                        width = if (selectedHex.equals(hex, ignoreCase = true)) 3.dp else 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(hex) },
            )
        }
    }
}

@Composable
private fun HexColorField(
    label: String,
    hex: String,
    onHexChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = hex,
        onValueChange = onHexChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TextStyleTextSection(
    style: TextStyleConfig,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("文字颜色")
        ColorSwatchRow(
            selectedHex = style.colorHex,
            colors = TextColorPresets,
            onSelect = { hex -> onStyleChange { it.copy(colorHex = hex) } },
        )
        HexColorField("自定义颜色 (#RRGGBB)", style.colorHex) { hex ->
            onStyleChange { it.copy(colorHex = hex) }
        }

        Text("不透明度：${(style.textAlpha * 100).roundToInt()}%")
        Slider(
            value = style.textAlpha,
            onValueChange = { v -> onStyleChange { it.copy(textAlpha = v) } },
            valueRange = 0f..1f,
        )

        Text("字号：${style.textSizeSp.roundToInt()} sp")
        Slider(
            value = style.textSizeSp,
            onValueChange = { v -> onStyleChange { it.copy(textSizeSp = v) } },
            valueRange = 12f..96f,
        )

        Text("字体")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FontFamilyOptions.forEach { (family, label) ->
                FilterChip(
                    selected = style.fontFamily == family,
                    onClick = { onStyleChange { it.copy(fontFamily = family) } },
                    label = { Text(label) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = style.isBold,
                onClick = { onStyleChange { it.copy(isBold = !it.isBold) } },
                label = { Text("粗体") },
            )
            FilterChip(
                selected = style.isItalic,
                onClick = { onStyleChange { it.copy(isItalic = !it.isItalic) } },
                label = { Text("斜体") },
            )
        }

        Text("字间距：${"%.2f".format(style.letterSpacingEm)} em")
        Slider(
            value = style.letterSpacingEm,
            onValueChange = { v -> onStyleChange { it.copy(letterSpacingEm = v) } },
            valueRange = -0.05f..0.5f,
        )

        Text("行高：${"%.2f".format(style.lineHeightMultiplier)}×")
        Slider(
            value = style.lineHeightMultiplier,
            onValueChange = { v -> onStyleChange { it.copy(lineHeightMultiplier = v) } },
            valueRange = 0.8f..2.0f,
        )
    }
}

@Composable
private fun TextStyleAlignSection(
    style: TextStyleConfig,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("水平对齐")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                HorizontalTextAlignment.Start to "左",
                HorizontalTextAlignment.Center to "中",
                HorizontalTextAlignment.End to "右",
            ).forEach { (align, label) ->
                FilterChip(
                    selected = style.horizontalAlignment == align,
                    onClick = { onStyleChange { it.copy(horizontalAlignment = align) } },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun TextStyleBlockSection(
    style: TextStyleConfig,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
) {
    val enabled = style.blockColorHex != null && style.blockAlpha > 0f
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("启用文字块背景")
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    onStyleChange {
                        if (checked) {
                            it.copy(
                                blockColorHex = it.blockColorHex ?: "#000000",
                                blockAlpha = if (it.blockAlpha > 0f) it.blockAlpha else 0.45f,
                                blockPaddingDp = if (it.blockPaddingDp > 0f) it.blockPaddingDp else 14f,
                                blockCornerRadiusDp = if (it.blockCornerRadiusDp > 0f) it.blockCornerRadiusDp else 10f,
                            )
                        } else {
                            it.copy(blockColorHex = null, blockAlpha = 0f)
                        }
                    }
                },
            )
        }
        if (enabled) {
            Text("背景颜色")
            ColorSwatchRow(
                selectedHex = style.blockColorHex,
                colors = BlockColorPresets,
                onSelect = { hex -> onStyleChange { it.copy(blockColorHex = hex) } },
            )
            HexColorField("自定义颜色 (#RRGGBB)", style.blockColorHex.orEmpty()) { hex ->
                onStyleChange { it.copy(blockColorHex = hex) }
            }
            Text("不透明度：${(style.blockAlpha * 100).roundToInt()}%")
            Slider(
                value = style.blockAlpha,
                onValueChange = { v -> onStyleChange { it.copy(blockAlpha = v) } },
                valueRange = 0f..1f,
            )
            Text("内边距：${style.blockPaddingDp.roundToInt()} dp")
            Slider(
                value = style.blockPaddingDp,
                onValueChange = { v -> onStyleChange { it.copy(blockPaddingDp = v) } },
                valueRange = 0f..48f,
            )
            Text("圆角：${style.blockCornerRadiusDp.roundToInt()} dp")
            Slider(
                value = style.blockCornerRadiusDp,
                onValueChange = { v -> onStyleChange { it.copy(blockCornerRadiusDp = v) } },
                valueRange = 0f..48f,
            )
        }
    }
}

@Composable
private fun TextStyleBorderSection(
    style: TextStyleConfig,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
) {
    val enabled = style.blockBorderWidthDp > 0f && style.blockBorderColorHex != null
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("启用边框")
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    onStyleChange {
                        if (checked) {
                            it.copy(
                                blockBorderWidthDp = if (it.blockBorderWidthDp > 0f) it.blockBorderWidthDp else 1.5f,
                                blockBorderColorHex = it.blockBorderColorHex ?: "#FFFFFF",
                                blockBorderAlpha = if (it.blockBorderAlpha > 0f) it.blockBorderAlpha else 1f,
                            )
                        } else {
                            it.copy(blockBorderWidthDp = 0f)
                        }
                    }
                },
            )
        }
        if (enabled) {
            Text("边框颜色")
            ColorSwatchRow(
                selectedHex = style.blockBorderColorHex,
                colors = BlockColorPresets,
                onSelect = { hex -> onStyleChange { it.copy(blockBorderColorHex = hex) } },
            )
            HexColorField("自定义颜色 (#RRGGBB)", style.blockBorderColorHex.orEmpty()) { hex ->
                onStyleChange { it.copy(blockBorderColorHex = hex) }
            }
            Text("宽度：${"%.1f".format(style.blockBorderWidthDp)} dp")
            Slider(
                value = style.blockBorderWidthDp,
                onValueChange = { v -> onStyleChange { it.copy(blockBorderWidthDp = v) } },
                valueRange = 0f..8f,
            )
            Text("不透明度：${(style.blockBorderAlpha * 100).roundToInt()}%")
            Slider(
                value = style.blockBorderAlpha,
                onValueChange = { v -> onStyleChange { it.copy(blockBorderAlpha = v) } },
                valueRange = 0f..1f,
            )
        }
    }
}

@Composable
private fun TextStyleShadowSection(
    style: TextStyleConfig,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
) {
    val enabled = style.shadowAlpha > 0f && (style.shadowRadiusDp > 0f || style.shadowDistanceDp > 0f)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("启用阴影")
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    onStyleChange {
                        if (checked) {
                            it.copy(
                                shadowRadiusDp = if (it.shadowRadiusDp > 0f) it.shadowRadiusDp else 8f,
                                shadowDistanceDp = if (it.shadowDistanceDp > 0f) it.shadowDistanceDp else 4f,
                                shadowAlpha = if (it.shadowAlpha > 0f) it.shadowAlpha else 0.45f,
                            )
                        } else {
                            it.copy(shadowAlpha = 0f)
                        }
                    }
                },
            )
        }
        if (enabled) {
            Text("阴影颜色")
            ColorSwatchRow(
                selectedHex = style.shadowColorHex,
                colors = ShadowColorPresets,
                onSelect = { hex -> onStyleChange { it.copy(shadowColorHex = hex) } },
            )
            Text("模糊半径：${style.shadowRadiusDp.roundToInt()} dp")
            Slider(
                value = style.shadowRadiusDp,
                onValueChange = { v -> onStyleChange { it.copy(shadowRadiusDp = v) } },
                valueRange = 0f..32f,
            )
            Text("偏移距离：${style.shadowDistanceDp.roundToInt()} dp")
            Slider(
                value = style.shadowDistanceDp,
                onValueChange = { v -> onStyleChange { it.copy(shadowDistanceDp = v) } },
                valueRange = 0f..32f,
            )
            Text("角度：${style.shadowAngleDegrees.roundToInt()}°")
            Slider(
                value = style.shadowAngleDegrees,
                onValueChange = { v -> onStyleChange { it.copy(shadowAngleDegrees = v) } },
                valueRange = 0f..360f,
            )
            Text("不透明度：${(style.shadowAlpha * 100).roundToInt()}%")
            Slider(
                value = style.shadowAlpha,
                onValueChange = { v -> onStyleChange { it.copy(shadowAlpha = v) } },
                valueRange = 0f..1f,
            )
        }
    }
}
