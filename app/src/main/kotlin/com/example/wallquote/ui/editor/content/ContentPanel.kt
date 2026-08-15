package com.example.wallquote.ui.editor.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wallquote.ui.editor.EditorTextEntry
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun ContentPanel(
    texts: List<EditorTextEntry>,
    previewIndex: Int,
    onAdd: () -> Unit,
    onUpdate: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onSelectPreview: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canDelete = texts.size > 1
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(texts, key = { _, item -> item.clientKey }) { index, entry ->
                QuoteItem(
                    text = entry.text,
                    selected = index == previewIndex,
                    canDelete = canDelete,
                    onTextChange = {
                        onSelectPreview(index)
                        onUpdate(entry.clientKey, it)
                    },
                    onSelect = { onSelectPreview(index) },
                    onDelete = { onDelete(entry.clientKey) },
                )
            }
        }
        TextButton(
            onClick = onAdd,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("+ Add new quote", color = WallQuoteColors.Beige)
        }
    }
}
