package com.example.wallquote.ui.editor.style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wallquote.domain.automatch.TextStyleSuggestion
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.ui.components.ColorSwatchRow
import com.example.wallquote.ui.components.OptionCard
import com.example.wallquote.ui.components.WallQuoteSlider
import com.example.wallquote.ui.theme.WallQuoteColors
import kotlin.math.roundToInt

private val TextColors = listOf("#FFFFFF", "#1A1A1A", "#F4F0E8", "#FFD54F", "#A3BE8C", "#88C0D0")

@Composable
fun TextStylePanel(
    style: TextStyleConfig,
    previewInputBase: QuoteRenderInput,
    processedPhotoBitmap: android.graphics.Bitmap?,
    recentStyles: List<TextStyleConfig>,
    autoMatchAvailable: Boolean,
    autoMatchLoading: Boolean,
    autoMatchSuggestion: TextStyleSuggestion?,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
    onApplyRecent: (TextStyleConfig) -> Unit,
    onRequestAutoMatch: () -> Unit,
    onConfirmAutoMatch: () -> Unit,
    onUndoAutoMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (recentStyles.isNotEmpty()) {
            Text("最近使用", color = WallQuoteColors.Ink)
            RecentStylesRow(
                styles = recentStyles,
                current = style,
                previewInputBase = previewInputBase,
                processedPhotoBitmap = processedPhotoBitmap,
                onSelect = onApplyRecent,
            )
        }

        Text("颜色", color = WallQuoteColors.Ink)
        ColorSwatchRow(
            selectedHex = style.colorHex,
            colors = TextColors,
            onSelect = { hex -> onStyleChange { it.copy(colorHex = hex) } },
        )

        WallQuoteSlider(
            label = "透明度",
            valueLabel = "${(style.textAlpha * 100).roundToInt()}%",
            value = style.textAlpha,
            onValueChange = { v -> onStyleChange { it.copy(textAlpha = v) } },
            valueRange = 0f..1f,
        )

        Text("字体", color = WallQuoteColors.Ink)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                SystemFontFamily.Serif to "衬线",
                SystemFontFamily.SansSerif to "无衬线",
                SystemFontFamily.Monospace to "等宽",
                SystemFontFamily.Cursive to "手写",
            ).forEach { (family, label) ->
                OptionCard(
                    label = label,
                    selected = style.fontFamily == family,
                    onClick = { onStyleChange { it.copy(fontFamily = family) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        WallQuoteSlider(
            label = "字号",
            valueLabel = "${style.textSizeSp.roundToInt()} sp",
            value = style.textSizeSp,
            onValueChange = { v -> onStyleChange { it.copy(textSizeSp = v) } },
            valueRange = 12f..96f,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OptionCard(
                label = "常规",
                selected = !style.isBold && !style.isItalic,
                onClick = { onStyleChange { it.copy(isBold = false, isItalic = false) } },
                modifier = Modifier.weight(1f),
            )
            OptionCard(
                label = "加粗",
                selected = style.isBold,
                onClick = { onStyleChange { it.copy(isBold = !it.isBold) } },
                modifier = Modifier.weight(1f),
            )
            OptionCard(
                label = "斜体",
                selected = style.isItalic,
                onClick = { onStyleChange { it.copy(isItalic = !it.isItalic) } },
                modifier = Modifier.weight(1f),
            )
        }

        Text("自动匹配背景", color = WallQuoteColors.Ink)
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
                    Text(autoMatchSuggestion.reason, color = WallQuoteColors.Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = onConfirmAutoMatch) { Text("应用") }
                        TextButton(onClick = onUndoAutoMatch) { Text("撤销") }
                    }
                }
            }
        }
    }
}
