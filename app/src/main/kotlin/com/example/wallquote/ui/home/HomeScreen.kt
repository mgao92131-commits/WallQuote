package com.example.wallquote.ui.home

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.R
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.ui.util.formatMinuteOfDay
import com.example.wallquote.wallpaper.LiveWallpaperLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNewCollection: () -> Unit,
    onEditCollection: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
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
                        onClick = { onEditCollection(item.id) },
                        onDelete = { pendingDelete = item },
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
            text = { Text("确定删除「${target.name}」？") },
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

@Composable
private fun CollectionCard(
    config: CollectionConfig,
    onClick: () -> Unit,
    onDelete: () -> Unit,
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
            Text(
                text = config.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = formatMinuteOfDay(config.schedule.startMinuteOfDay) +
                    " – " + formatMinuteOfDay(config.schedule.endMinuteOfDay),
                style = MaterialTheme.typography.bodySmall,
            )
            config.lines.sortedBy { it.displayOrder }.take(2).forEach { line ->
                Text(text = line.text, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}
