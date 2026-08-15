package com.example.wallquote.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.ui.theme.WallQuoteColors
import com.example.wallquote.wallpaper.LiveWallpaperLauncher

@Composable
fun HomeScreen(
    onNewCollection: () -> Unit,
    onEditCollection: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val cardModels by viewModel.cardModels.collectAsStateWithLifecycle()
    val nowMinuteOfDay by viewModel.nowMinuteOfDay.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<CollectionConfig?>(null) }
    var pendingRename by remember { mutableStateOf<CollectionConfig?>(null) }
    var renameDraft by remember { mutableStateOf("") }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "WallQuote",
                color = WallQuoteColors.Cream,
                fontSize = 28.sp,
                fontFamily = FontFamily.Serif,
            )
            Spacer(Modifier.weight(1f))
            TextButton(
                onClick = { launchWallpaper() },
                enabled = cardModels.isNotEmpty(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = WallQuoteColors.Beige,
                    disabledContentColor = WallQuoteColors.Beige.copy(alpha = 0.35f),
                ),
            ) {
                Text("Wallpaper")
            }
            IconButton(onClick = onNewCollection) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "新建收藏集",
                    tint = WallQuoteColors.Cream,
                )
            }
        }

        if (cardModels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("暂无收藏集，点击右上角新建。", color = WallQuoteColors.Ink)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(cardModels, key = { it.config.id }) { item ->
                    CollectionCard(
                        card = item,
                        nowMinuteOfDay = nowMinuteOfDay,
                        onClick = { onEditCollection(item.config.id) },
                        onRequestDelete = { pendingDelete = item.config },
                        onRequestRename = {
                            pendingRename = item.config
                            renameDraft = item.config.name
                        },
                    )
                }
            }
        }
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
            title = { Text("操作失败") },
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

    pendingRename?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingRename = null },
            title = { Text("重命名") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    label = { Text("名称") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameCollection(target.id, renameDraft)
                        pendingRename = null
                    },
                    enabled = renameDraft.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRename = null }) { Text("取消") }
            },
        )
    }
}
