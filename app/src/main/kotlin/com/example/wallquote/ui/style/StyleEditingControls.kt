package com.example.wallquote.ui.style

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.model.TextStyleConfig
import kotlin.math.roundToInt

/** Shared tabs used by both the collection editor's Style tab and the custom style editor. */
enum class StyleSection(val label: String) {
    Text("文字"),
    Block("文字块"),
    Border("边框"),
    Shadow("阴影"),
    Align("对齐"),
}

val TextColorPresets = listOf("#FFFFFF", "#1A1A1A", "#FFD54F", "#A3BE8C", "#88C0D0")
val BlockColorPresets = listOf("#000000", "#FFFFFF", "#2E3440", "#3B4252")
val ShadowColorPresets = listOf("#000000", "#FFFFFF")
val FontFamilyOptions = listOf(
    SystemFontFamily.Serif to "衬线",
    SystemFontFamily.SansSerif to "无衬线",
    SystemFontFamily.Monospace to "等宽",
    SystemFontFamily.Cursive to "手写体",
)

@Composable
fun ColorSwatchRow(
    selectedHex: String?,
    colors: List<String>,
    onSelect: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { hex ->
            val selected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(32.dp)
                    .clip(CircleShape)
                    .background(parseColorHex(hex))
                    .border(
                        width = if (selected) 3.dp else 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(hex) },
            )
        }
    }
}

@Composable
fun HexColorField(
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
fun TextStyleTextSection(
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
fun TextStyleAlignSection(
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
fun TextStyleBlockSection(
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
fun TextStyleBorderSection(
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
fun TextStyleShadowSection(
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

/** Renders the section picked by [section] using the shared editing controls above. */
@Composable
fun StyleSectionContent(
    section: StyleSection,
    style: TextStyleConfig,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
) {
    when (section) {
        StyleSection.Text -> TextStyleTextSection(style, onStyleChange)
        StyleSection.Block -> TextStyleBlockSection(style, onStyleChange)
        StyleSection.Border -> TextStyleBorderSection(style, onStyleChange)
        StyleSection.Shadow -> TextStyleShadowSection(style, onStyleChange)
        StyleSection.Align -> TextStyleAlignSection(style, onStyleChange)
    }
}
