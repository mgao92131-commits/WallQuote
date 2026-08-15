package com.example.wallquote.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.ui.theme.WallQuoteColors
import com.example.wallquote.ui.util.formatMinuteOfDay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionCard(
    card: CollectionCardUiModel,
    nowMinuteOfDay: Int,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
    onRequestRename: () -> Unit,
) {
    val config = card.config
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete()
            }
            false
        },
    )
    val shape = RoundedCornerShape(30.dp)
    var menuExpanded by remember { mutableStateOf(false) }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = WallQuoteColors.HandleEnd.copy(alpha = 0.35f),
                shape = shape,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = WallQuoteColors.Cream,
                    )
                }
            }
        },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .clickable(onClick = onClick),
            shape = shape,
            color = WallQuoteColors.Surface,
            border = BorderStroke(1.dp, WallQuoteColors.BeigeMuted),
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 30.dp, bottomStart = 30.dp)),
                ) {
                    QuotePreview(
                        state = QuoteRenderInput.fromCollection(config),
                        modifier = Modifier.fillMaxSize(),
                        processedPhotoBitmap = card.thumbnailBitmap,
                    )
                    if (card.thumbnailLoadFailed) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = "图片加载失败",
                            tint = WallQuoteColors.Ink,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(0.66f)
                        .fillMaxHeight()
                        .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val scheduleLabel = if (config.schedule.isAllDay()) {
                            "All Day"
                        } else {
                            "${formatMinuteOfDay(config.schedule.startMinuteOfDay)} – ${formatMinuteOfDay(config.schedule.endMinuteOfDay)}"
                        }
                        Text(
                            text = scheduleLabel,
                            color = WallQuoteColors.Cream,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "更多",
                                    tint = WallQuoteColors.Ink,
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("重命名") },
                                    onClick = {
                                        menuExpanded = false
                                        onRequestRename()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("删除") },
                                    onClick = {
                                        menuExpanded = false
                                        onRequestDelete()
                                    },
                                )
                            }
                        }
                    }
                    ScheduleTimelineBar(
                        config = config,
                        nowMinuteOfDay = nowMinuteOfDay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                    )
                    config.lines.take(3).forEachIndexed { index, line ->
                        Text(
                            text = if (index == 0) "❝ ${line.text}" else line.text,
                            color = WallQuoteColors.CreamMuted,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
