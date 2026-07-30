package com.example.wallquote.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.R
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.time.ScheduleTimelineGeometry
import com.example.wallquote.ui.util.formatMinuteOfDay
import com.example.wallquote.wallpaper.LiveWallpaperLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewCollection: () -> Unit,
    onEditCollection: (Long) -> Unit,
    onManageCustomStyles: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val nowMinuteOfDay by viewModel.nowMinuteOfDay.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<CollectionConfig?>(null) }
    var showEmptyWallpaperHint by remember { mutableStateOf(false) }
    var wallpaperError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    fun launchWallpaper() {
        when (val result = LiveWallpaperLauncher.launch(context)) {
            is LiveWallpaperLauncher.Result.Failed -> wallpaperError = result.message
            LiveWallpaperLauncher.Result.LaunchedChangeLiveWallpaper,
            LiveWallpaperLauncher.Result.LaunchedChooser,
            -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("壁上言") },
                actions = {
                    IconButton(onClick = onManageCustomStyles) {
                        Icon(Icons.Default.Palette, contentDescription = "自定义样式")
                    }
                    IconButton(
                        onClick = {
                            if (collections.isEmpty()) {
                                showEmptyWallpaperHint = true
                            } else {
                                launchWallpaper()
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.Wallpaper,
                            contentDescription = stringResource(R.string.set_as_wallpaper),
                        )
                    }
                    IconButton(onClick = onNewCollection) {
                        Icon(Icons.Default.Add, contentDescription = "新建收藏集")
                    }
                },
            )
        },
    ) { padding ->
        if (collections.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("暂无收藏集，点击右上角新建。")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(collections, key = { it.id }) { item ->
                    CollectionCard(
                        config = item,
                        nowMinuteOfDay = nowMinuteOfDay,
                        onClick = { onEditCollection(item.id) },
                        onRequestDelete = { pendingDelete = item },
                    )
                }
            }
        }
    }

    if (showEmptyWallpaperHint) {
        AlertDialog(
            onDismissRequest = { showEmptyWallpaperHint = false },
            title = { Text(stringResource(R.string.set_as_wallpaper)) },
            text = { Text(stringResource(R.string.set_wallpaper_empty_hint)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyWallpaperHint = false
                        launchWallpaper()
                    },
                ) { Text("继续设置") }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyWallpaperHint = false }) { Text("取消") }
            },
        )
    }

    wallpaperError?.let { message ->
        AlertDialog(
            onDismissRequest = { wallpaperError = null },
            title = { Text("无法设为壁纸") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { wallpaperError = null }) { Text("确定") }
            },
        )
    }

    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("删除失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("确定") }
            },
        )
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除收藏集") },
            text = { Text("确定删除「${target.name}」？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCollection(target.id)
                        pendingDelete = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionCard(
    config: CollectionConfig,
    nowMinuteOfDay: Int,
    onClick: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    // Swiping never deletes directly: confirmValueChange always rejects the state change (returns
    // false) after surfacing the confirm dialog via onRequestDelete, so the box snaps back on its own.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onRequestDelete()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
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
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                QuotePreview(
                    state = QuoteRenderInput.fromCollection(config),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = config.name, style = MaterialTheme.typography.titleMedium)
                    val isActiveNow = config.schedule.contains(nowMinuteOfDay)
                    if (isActiveNow) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                text = "使用中",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Text(
                    text = "共 ${config.lines.size} 条名言 · " +
                        formatMinuteOfDay(config.schedule.startMinuteOfDay) +
                        " – " + formatMinuteOfDay(config.schedule.endMinuteOfDay),
                    style = MaterialTheme.typography.bodySmall,
                )
                ScheduleTimelineBar(
                    config = config,
                    nowMinuteOfDay = nowMinuteOfDay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(10.dp),
                )
            }
        }
    }
}

/** Renders a 24h horizontal bar highlighting the collection's active window(s) and the current time. */
@Composable
private fun ScheduleTimelineBar(
    config: CollectionConfig,
    nowMinuteOfDay: Int,
    modifier: Modifier = Modifier,
) {
    val result = ScheduleTimelineGeometry.compute(config.schedule, nowMinuteOfDay)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (result.isActiveNow) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    }
    val nowColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.background(trackColor, CircleShape)) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)
        result.segments.forEach { segment ->
            val left = segment.startFraction * size.width
            val right = segment.endFraction * size.width
            if (right > left) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                    size = androidx.compose.ui.geometry.Size(right - left, size.height),
                    cornerRadius = corner,
                )
            }
        }
        val nowX = (result.nowFraction * size.width).coerceIn(1f, size.width - 1f)
        drawLine(
            color = nowColor,
            start = androidx.compose.ui.geometry.Offset(nowX, 0f),
            end = androidx.compose.ui.geometry.Offset(nowX, size.height),
            strokeWidth = 2f,
        )
    }
}
