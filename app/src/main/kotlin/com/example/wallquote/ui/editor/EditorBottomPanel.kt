package com.example.wallquote.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.wallquote.ui.theme.WallQuoteColors

val EditorPanel.bottomPanelHeightFraction: Float
    get() = when (this) {
        EditorPanel.Time, EditorPanel.Background -> 0.58f
        EditorPanel.Style -> 0.60f
        EditorPanel.Content -> 0.82f
    }

@Composable
fun EditorBottomPanel(
    height: Dp,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = WallQuoteColors.Surface.copy(alpha = 0.97f),
        tonalElevation = 6.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(WallQuoteColors.Beige.copy(alpha = 0.45f)),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                content()
            }
        }
    }
}
