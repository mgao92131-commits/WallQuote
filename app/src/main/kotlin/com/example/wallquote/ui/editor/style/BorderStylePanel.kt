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
import com.example.wallquote.ui.components.ColorSwatchRow
import com.example.wallquote.ui.components.WallQuoteSlider
import com.example.wallquote.ui.theme.WallQuoteColors
import kotlin.math.roundToInt

private val BorderColors = listOf("#FFFFFF", "#000000", "#D9CDB8", "#88C0D0", "#BF616A")

@Composable
fun BorderStylePanel(
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
        ColorSwatchRow(
            selectedHex = style.blockBorderColorHex,
            colors = BorderColors,
            onSelect = { hex ->
                onStyleChange {
                    it.copy(
                        blockBorderColorHex = hex,
                        blockBorderWidthDp = if (it.blockBorderWidthDp > 0f) it.blockBorderWidthDp else 1.5f,
                    )
                }
            },
        )
        WallQuoteSlider(
            label = "粗细",
            valueLabel = "${"%.1f".format(style.blockBorderWidthDp)} dp",
            value = style.blockBorderWidthDp,
            onValueChange = { v ->
                onStyleChange {
                    it.copy(
                        blockBorderWidthDp = v,
                        blockBorderColorHex = it.blockBorderColorHex ?: "#FFFFFF",
                    )
                }
            },
            valueRange = 0f..8f,
        )
        WallQuoteSlider(
            label = "透明度",
            valueLabel = "${(style.blockBorderAlpha * 100).roundToInt()}%",
            value = style.blockBorderAlpha,
            onValueChange = { v -> onStyleChange { it.copy(blockBorderAlpha = v) } },
            valueRange = 0f..1f,
        )
    }
}
