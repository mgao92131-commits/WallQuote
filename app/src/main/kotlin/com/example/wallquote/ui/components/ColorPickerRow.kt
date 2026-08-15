package com.example.wallquote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun ColorPickerRow(
    selectedHex: String?,
    colors: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    pickerFallbackHex: String = "#FFFFFF",
) {
    var showPicker by remember { mutableStateOf(false) }
    val isCustom = selectedHex != null &&
        colors.none { it.equals(selectedHex, ignoreCase = true) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onClear != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, WallQuoteColors.BeigeMuted, CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "取消",
                    tint = WallQuoteColors.Ink,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF3B30),
                            Color(0xFFFFCC00),
                            Color(0xFF34C759),
                            Color(0xFF007AFF),
                            Color(0xFFAF52DE),
                            Color(0xFFFF3B30),
                        ),
                    ),
                )
                .border(
                    width = if (isCustom) 2.5.dp else 2.dp,
                    color = if (isCustom) WallQuoteColors.Cream else WallQuoteColors.Canvas,
                    shape = CircleShape,
                )
                .clickable(onClickLabel = "色盘") { showPicker = true },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(parseColorHex(selectedHex ?: pickerFallbackHex)),
            )
        }
        ColorSwatchRow(
            selectedHex = selectedHex,
            colors = colors,
            onSelect = onSelect,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
        )
    }
    if (showPicker) {
        ColorPickerDialog(
            initialHex = selectedHex ?: pickerFallbackHex,
            onConfirm = { hex ->
                onSelect(hex)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}
