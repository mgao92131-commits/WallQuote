package com.example.wallquote.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DockBackground = Color(0xE6121214)
private val DockSelected = Color(0x26FFFFFF)
private val DockSelectedContent = Color(0xFFF4F0E8)
private val DockIdleContent = Color(0x99F4F0E8)

@Composable
fun EditorBottomDock(
    selectedPanel: EditorPanel?,
    onSelect: (EditorPanel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(DockBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EditorPanel.entries.forEach { panel ->
            DockItem(
                panel = panel,
                selected = selectedPanel == panel,
                onClick = { onSelect(panel) },
            )
        }
    }
}

@Composable
private fun DockItem(
    panel: EditorPanel,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) DockSelectedContent else DockIdleContent
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) DockSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = panel.icon,
            contentDescription = panel.title,
            tint = contentColor,
        )
        Text(
            text = panel.title,
            color = contentColor,
            fontSize = 11.sp,
        )
    }
}

private val EditorPanel.icon: ImageVector
    get() = when (this) {
        EditorPanel.Time -> Icons.Outlined.Schedule
        EditorPanel.Background -> Icons.Outlined.Image
        EditorPanel.Content -> Icons.AutoMirrored.Outlined.Subject
        EditorPanel.Style -> Icons.Outlined.TextFields
    }
