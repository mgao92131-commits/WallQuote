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

private val ShadowColors = listOf("#000000", "#FFFFFF", "#1A1A1A")

@Composable
fun ShadowStylePanel(
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
            selectedHex = style.shadowColorHex,
            colors = ShadowColors,
            onSelect = { hex ->
                onStyleChange {
                    it.copy(
                        shadowColorHex = hex,
                        shadowAlpha = if (it.shadowAlpha > 0f) it.shadowAlpha else 0.45f,
                        shadowRadiusDp = if (it.shadowRadiusDp > 0f) it.shadowRadiusDp else 8f,
                    )
                }
            },
            onClear = { onStyleChange { it.copy(shadowAlpha = 0f) } },
        )
        WallQuoteSlider(
            label = "模糊",
            valueLabel = "${style.shadowRadiusDp.roundToInt()} dp",
            value = style.shadowRadiusDp,
            onValueChange = { v ->
                onStyleChange {
                    it.copy(
                        shadowRadiusDp = v,
                        shadowAlpha = if (it.shadowAlpha > 0f) it.shadowAlpha else 0.45f,
                    )
                }
            },
            valueRange = 0f..32f,
        )
        WallQuoteSlider(
            label = "距离",
            valueLabel = "${style.shadowDistanceDp.roundToInt()} dp",
            value = style.shadowDistanceDp,
            onValueChange = { v -> onStyleChange { it.copy(shadowDistanceDp = v) } },
            valueRange = 0f..32f,
        )
        WallQuoteSlider(
            label = "角度",
            valueLabel = "${style.shadowAngleDegrees.roundToInt()}°",
            value = style.shadowAngleDegrees,
            onValueChange = { v -> onStyleChange { it.copy(shadowAngleDegrees = v) } },
            valueRange = 0f..360f,
        )
        WallQuoteSlider(
            label = "透明度",
            valueLabel = "${(style.shadowAlpha * 100).roundToInt()}%",
            value = style.shadowAlpha,
            onValueChange = { v -> onStyleChange { it.copy(shadowAlpha = v) } },
            valueRange = 0f..1f,
        )
    }
}
