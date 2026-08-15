package com.example.wallquote.ui.editor.style

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.domain.automatch.TextStyleSuggestion
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.ui.editor.StyleEditorDraft
import com.example.wallquote.ui.editor.StyleEditorTab
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun StyleEditor(
    draft: StyleEditorDraft,
    previewInput: QuoteRenderInput,
    processedPhotoBitmap: android.graphics.Bitmap?,
    recentStyles: List<TextStyleConfig>,
    autoMatchAvailable: Boolean,
    autoMatchLoading: Boolean,
    autoMatchSuggestion: TextStyleSuggestion?,
    onWorkingStyleChange: ((TextStyleConfig) -> TextStyleConfig) -> Unit,
    onApplyRecent: (TextStyleConfig) -> Unit,
    onSelectTab: (StyleEditorTab) -> Unit,
    onConfirm: () -> Unit,
    onDiscard: () -> Unit,
    onRequestAutoMatch: () -> Unit,
    onConfirmAutoMatch: () -> Unit,
    onUndoAutoMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = draft.workingStyle
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WallQuoteColors.Canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDiscard) {
                Icon(Icons.Default.Close, contentDescription = "取消", tint = WallQuoteColors.Cream)
            }
            Text(
                text = "自定义样式",
                color = WallQuoteColors.Cream,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onConfirm) {
                Icon(Icons.Default.Check, contentDescription = "完成", tint = WallQuoteColors.Cream)
            }
        }

        QuotePreview(
            state = previewInput,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            processedPhotoBitmap = processedPhotoBitmap,
        )

        StyleTabBar(
            selected = draft.selectedTab,
            onSelect = onSelectTab,
        )

        when (draft.selectedTab) {
            StyleEditorTab.Text -> TextStylePanel(
                style = style,
                previewInputBase = previewInput,
                processedPhotoBitmap = processedPhotoBitmap,
                recentStyles = recentStyles,
                autoMatchAvailable = autoMatchAvailable,
                autoMatchLoading = autoMatchLoading,
                autoMatchSuggestion = autoMatchSuggestion,
                onStyleChange = onWorkingStyleChange,
                onApplyRecent = onApplyRecent,
                onRequestAutoMatch = onRequestAutoMatch,
                onConfirmAutoMatch = onConfirmAutoMatch,
                onUndoAutoMatch = onUndoAutoMatch,
            )
            StyleEditorTab.Border -> BorderStylePanel(style = style, onStyleChange = onWorkingStyleChange)
            StyleEditorTab.Shadow -> ShadowStylePanel(style = style, onStyleChange = onWorkingStyleChange)
            StyleEditorTab.Block -> BlockStylePanel(style = style, onStyleChange = onWorkingStyleChange)
            StyleEditorTab.Align -> AlignmentStylePanel(style = style, onStyleChange = onWorkingStyleChange)
        }
    }
}
