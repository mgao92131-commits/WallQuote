package com.example.wallquote.ui.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.time.HALF_HOUR_SLOTS
import com.example.wallquote.domain.time.halfHourIndexFromMinute
import com.example.wallquote.ui.util.formatMinuteOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    collectionId: Long?,
    onFinished: () -> Unit,
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
                QuotePreview(
                    state = state.toPreviewInput(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    resolvedPhotoPath = state.resolvedPhotoPath,
                )
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
                    EditorTab.Time -> TimeTabContent(
                        startMinute = state.startMinute,
                        endMinute = state.endMinute,
                        onStartIndexChange = viewModel::setStartHalfHourIndex,
                        onEndIndexChange = viewModel::setEndHalfHourIndex,
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
                        onStyleChange = viewModel::updateTextStyle,
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

@Composable
private fun TimeTabContent(
    startMinute: Int,
    endMinute: Int,
    onStartIndexChange: (Int) -> Unit,
    onEndIndexChange: (Int) -> Unit,
) {
    val startIndex = halfHourIndexFromMinute(startMinute).toFloat()
    val endIndex = halfHourIndexFromMinute(endMinute).toFloat()
    Column(modifier = Modifier.padding(16.dp)) {
        Text("开始：${formatMinuteOfDay(startMinute)}")
        Slider(
            value = startIndex,
            onValueChange = { onStartIndexChange(it.toInt()) },
            valueRange = 0f..(HALF_HOUR_SLOTS - 1).toFloat(),
            steps = HALF_HOUR_SLOTS - 2,
        )
        Text("结束：${formatMinuteOfDay(endMinute)}")
        Slider(
            value = endIndex,
            onValueChange = { onEndIndexChange(it.toInt()) },
            valueRange = 0f..(HALF_HOUR_SLOTS - 1).toFloat(),
            steps = HALF_HOUR_SLOTS - 2,
        )
        Text(
            text = "起止相同表示全天有效。步长 30 分钟（00:00–23:30）。",
            style = MaterialTheme.typography.bodySmall,
        )
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
    style: com.example.wallquote.domain.model.TextStyleConfig,
    onStyleChange: ((com.example.wallquote.domain.model.TextStyleConfig) -> com.example.wallquote.domain.model.TextStyleConfig) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("字号：${style.textSizeSp.toInt()} sp")
        Slider(
            value = style.textSizeSp,
            onValueChange = { v -> onStyleChange { it.copy(textSizeSp = v) } },
            valueRange = 16f..64f,
        )
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
        Text("水平对齐")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "左", 1 to "中", 2 to "右").forEach { (align, label) ->
                FilterChip(
                    selected = style.alignment == align,
                    onClick = { onStyleChange { it.copy(alignment = align) } },
                    label = { Text(label) },
                )
            }
        }
        Text("字体")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Serif" to "衬线", "SansSerif" to "无衬线", "Monospace" to "等宽").forEach { (id, label) ->
                FilterChip(
                    selected = style.fontFamilyName.equals(id, ignoreCase = true),
                    onClick = { onStyleChange { it.copy(fontFamilyName = id) } },
                    label = { Text(label) },
                )
            }
        }
        Text("文字颜色")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("#FFFFFF", "#000000", "#FFD54F").forEach { hex ->
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .fillMaxWidth(0.15f)
                        .clip(CircleShape)
                        .background(parseColorHex(hex))
                        .border(
                            width = if (style.colorHex.equals(hex, ignoreCase = true)) 3.dp else 0.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        )
                        .clickable { onStyleChange { it.copy(colorHex = hex) } },
                )
            }
        }
    }
}
