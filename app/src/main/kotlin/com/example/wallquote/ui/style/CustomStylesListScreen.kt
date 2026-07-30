package com.example.wallquote.ui.style

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CustomTextStyle
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.QuoteTransform

/** Neutral, style-agnostic preview background so swatches only reflect text style choices. */
internal const val STYLE_PREVIEW_BACKGROUND_HEX = "#3B4252"
internal const val STYLE_PREVIEW_SAMPLE_TEXT = "文字示例 The quick brown fox"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomStylesListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: CustomStylesListViewModel = hiltViewModel(),
) {
    val styles by viewModel.styles.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<CustomTextStyle?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("自定义样式") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onCreate) {
                        Icon(Icons.Default.Add, contentDescription = "新建样式")
                    }
                },
            )
        },
    ) { padding ->
        if (styles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("暂无自定义样式，点击右上角新建。")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(styles, key = { it.id }) { item ->
                    CustomStyleCard(
                        style = item,
                        onClick = { onEdit(item.id) },
                        onDelete = { pendingDelete = item },
                    )
                }
            }
        }
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
            title = { Text("删除样式") },
            text = {
                Text(
                    "确定删除「${target.name}」？已使用该样式的收藏集保存的是独立副本，不会受影响。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(target.id)
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
private fun CustomStyleCard(
    style: CustomTextStyle,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                QuotePreview(
                    state = QuoteRenderInput(
                        background = BackgroundSpec.Solid(STYLE_PREVIEW_BACKGROUND_HEX),
                        lines = listOf(QuoteLine(id = 1, text = STYLE_PREVIEW_SAMPLE_TEXT, displayOrder = 0)),
                        previewLineIndex = 0,
                        textStyle = style.style,
                        transform = QuoteTransform(),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(style.name, style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}
