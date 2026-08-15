package com.example.wallquote.ui.editor.style

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallquote.ui.editor.StyleEditorTab
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun StyleTabBar(
    selected: StyleEditorTab,
    onSelect: (StyleEditorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StyleEditorTab.entries.forEach { tab ->
            val isSelected = selected == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) WallQuoteColors.SurfaceRaised else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) WallQuoteColors.Cream else WallQuoteColors.Ink,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

private val StyleEditorTab.label: String
    get() = when (this) {
        StyleEditorTab.Text -> "文本"
        StyleEditorTab.Border -> "描边"
        StyleEditorTab.Shadow -> "阴影"
        StyleEditorTab.Block -> "背景"
        StyleEditorTab.Align -> "对齐"
    }
