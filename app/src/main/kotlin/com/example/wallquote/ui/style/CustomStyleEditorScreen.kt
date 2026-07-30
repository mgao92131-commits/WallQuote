package com.example.wallquote.ui.style

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.QuoteTransform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomStyleEditorScreen(
    styleId: Long?,
    onFinished: () -> Unit,
    viewModel: CustomStyleEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                CustomStyleEditorEvent.Finish -> onFinished()
            }
        }
    }

    state.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearError,
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearError) { Text("确定") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (styleId == null) "新建样式" else "编辑样式") },
                navigationIcon = {
                    IconButton(onClick = onFinished) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = state.canSave) {
                        Text("保存")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("加载中…") }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                QuotePreview(
                    state = QuoteRenderInput(
                        background = BackgroundSpec.Solid(STYLE_PREVIEW_BACKGROUND_HEX),
                        lines = listOf(QuoteLine(id = 1, text = STYLE_PREVIEW_SAMPLE_TEXT, displayOrder = 0)),
                        previewLineIndex = 0,
                        textStyle = state.style,
                        transform = QuoteTransform(),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text("样式名称") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StyleSection.entries.forEach { section ->
                        FilterChip(
                            selected = state.selectedSection == section,
                            onClick = { viewModel.selectSection(section) },
                            label = { Text(section.label) },
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    StyleSectionContent(
                        section = state.selectedSection,
                        style = state.style,
                        onStyleChange = viewModel::updateStyle,
                    )
                }
            }
        }
    }
}
