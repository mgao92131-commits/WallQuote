package com.example.wallquote.ui.editor.style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.ui.components.ColorPickerRow
import com.example.wallquote.ui.components.WallQuoteSlider
import com.example.wallquote.ui.theme.WallQuoteColors
import kotlin.math.roundToInt

private val BlockColors = listOf("#000000", "#FFFFFF", "#2E3440", "#3B4252", "#4A4458")

@Composable
fun BlockStylePanel(
    style: TextStyleConfig,
    onStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("颜色", color = WallQuoteColors.Ink)
        ColorPickerRow(
            selectedHex = style.blockColorHex,
            colors = BlockColors,
            onSelect = { hex ->
                onStyleChange {
                    it.copy(
                        blockColorHex = hex,
                        blockAlpha = if (it.blockAlpha > 0f) it.blockAlpha else 0.45f,
                        blockPaddingDp = if (it.blockPaddingDp > 0f) it.blockPaddingDp else 14f,
                    )
                }
            },
            onClear = { onStyleChange { it.copy(blockColorHex = null, blockAlpha = 0f) } },
        )
        WallQuoteSlider(
            label = "透明度",
            valueLabel = "${(style.blockAlpha * 100).roundToInt()}%",
            value = style.blockAlpha,
            onValueChange = { v ->
                onStyleChange {
                    it.copy(
                        blockAlpha = v,
                        blockColorHex = it.blockColorHex ?: "#000000",
                        blockPaddingDp = if (it.blockPaddingDp > 0f) it.blockPaddingDp else 14f,
                    )
                }
            },
            valueRange = 0f..1f,
        )
        WallQuoteSlider(
            label = "圆角",
            valueLabel = "${style.blockCornerRadiusDp.roundToInt()} dp",
            value = style.blockCornerRadiusDp,
            onValueChange = { v -> onStyleChange { it.copy(blockCornerRadiusDp = v) } },
            valueRange = 0f..48f,
        )
    }
}
