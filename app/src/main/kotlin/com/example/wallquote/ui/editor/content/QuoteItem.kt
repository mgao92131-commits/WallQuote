package com.example.wallquote.ui.editor.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallquote.ui.theme.WallQuoteColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteItem(
    text: String,
    selected: Boolean,
    canDelete: Boolean,
    onTextChange: (String) -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    if (!canDelete) {
        QuoteItemSurface(
            text = text,
            selected = selected,
            shape = shape,
            onTextChange = onTextChange,
            onSelect = onSelect,
            modifier = modifier,
        )
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = WallQuoteColors.HandleEnd.copy(alpha = 0.35f),
                shape = shape,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd,
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
        QuoteItemSurface(
            text = text,
            selected = selected,
            shape = shape,
            onTextChange = onTextChange,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun QuoteItemSurface(
    text: String,
    selected: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onTextChange: (String) -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = shape,
        color = if (selected) WallQuoteColors.SelectedQuote else WallQuoteColors.SurfaceRaised,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) WallQuoteColors.Beige else WallQuoteColors.BeigeMuted,
        ),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    color = WallQuoteColors.Cream,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                ),
                cursorBrush = SolidColor(WallQuoteColors.Beige),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text("输入名言", color = WallQuoteColors.Ink, fontSize = 16.sp)
                    }
                    inner()
                },
            )
        }
    }
}
