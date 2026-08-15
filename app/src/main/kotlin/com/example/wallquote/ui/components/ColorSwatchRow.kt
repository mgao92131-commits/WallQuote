package com.example.wallquote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.wallquote.core.preview.parseColorHex
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun ColorSwatchRow(
    selectedHex: String?,
    colors: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        colors.forEach { hex ->
            val selected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(parseColorHex(hex))
                    .border(
                        width = if (selected) 2.5.dp else 1.dp,
                        color = if (selected) WallQuoteColors.Cream else WallQuoteColors.BeigeMuted,
                        shape = CircleShape,
                    )
                    .clickable { onSelect(hex) },
            )
        }
    }
}
