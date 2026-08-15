package com.example.wallquote.ui.editor.style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.ui.components.OptionCard
import com.example.wallquote.ui.components.WallQuoteSlider

@Composable
fun AlignmentStylePanel(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OptionCard(
                label = "左对齐",
                selected = style.horizontalAlignment == HorizontalTextAlignment.Start,
                onClick = { onStyleChange { it.copy(horizontalAlignment = HorizontalTextAlignment.Start) } },
                modifier = Modifier.weight(1f),
            )
            OptionCard(
                label = "居中",
                selected = style.horizontalAlignment == HorizontalTextAlignment.Center,
                onClick = { onStyleChange { it.copy(horizontalAlignment = HorizontalTextAlignment.Center) } },
                modifier = Modifier.weight(1f),
            )
            OptionCard(
                label = "右对齐",
                selected = style.horizontalAlignment == HorizontalTextAlignment.End,
                onClick = { onStyleChange { it.copy(horizontalAlignment = HorizontalTextAlignment.End) } },
                modifier = Modifier.weight(1f),
            )
        }
        WallQuoteSlider(
            label = "字间距",
            valueLabel = "${"%.2f".format(style.letterSpacingEm)} em",
            value = style.letterSpacingEm,
            onValueChange = { v -> onStyleChange { it.copy(letterSpacingEm = v) } },
            valueRange = -0.05f..0.5f,
        )
        WallQuoteSlider(
            label = "行间距",
            valueLabel = "${"%.2f".format(style.lineHeightMultiplier)}×",
            value = style.lineHeightMultiplier,
            onValueChange = { v -> onStyleChange { it.copy(lineHeightMultiplier = v) } },
            valueRange = 0.8f..2.0f,
        )
    }
}
