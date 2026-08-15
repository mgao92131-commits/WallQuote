package com.example.wallquote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun OptionCard(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) WallQuoteColors.SurfaceRaised else WallQuoteColors.Surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) WallQuoteColors.Beige else WallQuoteColors.BeigeMuted,
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = if (selected) WallQuoteColors.Cream else WallQuoteColors.Ink,
                textAlign = TextAlign.Center,
            )
        }
    }
}
