package com.example.wallquote.ui.editor.background

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.ui.components.ColorSwatchRow
import com.example.wallquote.ui.components.WallQuoteSlider
import com.example.wallquote.ui.editor.BackgroundKind
import com.example.wallquote.ui.editor.EditorUiState
import com.example.wallquote.ui.editor.PhotoEditorState
import com.example.wallquote.ui.theme.WallQuoteColors
import kotlin.math.roundToInt

private val SolidColors = listOf("#000000", "#2E3440", "#3B4252", "#BF616A", "#FFFFFF", "#4F8FBF")

@Composable
fun CustomBackgroundPanel(
    state: EditorUiState,
    onBack: () -> Unit,
    onSelectSolidKind: () -> Unit,
    onSelectGradientKind: () -> Unit,
    onSelectPhotoKind: () -> Unit,
    onSolidSelected: (String) -> Unit,
    onGradientChange: (startHex: String?, endHex: String?, angleDegrees: Float?) -> Unit,
    onSwapGradient: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickCancelled: () -> Unit,
    onPhotoPicked: (String) -> Unit,
    onDimChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) onPickCancelled() else onPhotoPicked(uri.toString())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = WallQuoteColors.Cream,
                )
            }
            Text("Custom Background", color = WallQuoteColors.Cream)
        }

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
                Text("颜色", color = WallQuoteColors.Ink)
                ColorSwatchRow(
                    selectedHex = bg.colorHex,
                    colors = SolidColors,
                    onSelect = onSolidSelected,
                )
            }
            is BackgroundSpec.Gradient -> {
                Text("起始 / 结束颜色", color = WallQuoteColors.Ink)
                ColorSwatchRow(
                    selectedHex = bg.startColorHex,
                    colors = SolidColors,
                    onSelect = { onGradientChange(it, null, null) },
                )
                ColorSwatchRow(
                    selectedHex = bg.endColorHex,
                    colors = SolidColors,
                    onSelect = { onGradientChange(null, it, null) },
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(parseColorHex(bg.startColorHex), parseColorHex(bg.endColorHex)),
                            ),
                        )
                        .border(1.dp, WallQuoteColors.BeigeMuted, CircleShape),
                )
                TextButton(onClick = onSwapGradient) { Text("交换颜色") }
                WallQuoteSlider(
                    label = "角度",
                    valueLabel = "${bg.angleDegrees.roundToInt()}°",
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
                    is PhotoEditorState.Failed -> Text(photoState.message, color = WallQuoteColors.HandleEnd)
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
                WallQuoteSlider(
                    label = "暗化",
                    valueLabel = "${(bg.dimAmount * 100).roundToInt()}%",
                    value = bg.dimAmount,
                    onValueChange = onDimChange,
                    valueRange = 0f..1f,
                )
                WallQuoteSlider(
                    label = "模糊",
                    valueLabel = "${bg.blurRadiusDp.roundToInt()} dp",
                    value = bg.blurRadiusDp,
                    onValueChange = onBlurChange,
                    valueRange = 0f..25f,
                )
            }
        }
    }
}
