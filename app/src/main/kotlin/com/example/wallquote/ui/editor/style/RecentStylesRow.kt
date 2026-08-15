package com.example.wallquote.ui.editor.style

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wallquote.core.preview.QuotePreview
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.ui.theme.WallQuoteColors

@Composable
fun RecentStylesRow(
    styles: List<TextStyleConfig>,
    current: TextStyleConfig,
    previewInputBase: QuoteRenderInput,
    processedPhotoBitmap: android.graphics.Bitmap?,
    onSelect: (TextStyleConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (styles.isEmpty()) return
    LazyRow(modifier = modifier, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        itemsIndexed(styles, key = { index, style -> "$index-${style.hashCode()}" }) { _, style ->
            val selected = style == current
            Surface(
                onClick = { onSelect(style) },
                modifier = Modifier
                    .width(72.dp)
                    .height(88.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) WallQuoteColors.Beige else WallQuoteColors.BeigeMuted,
                ),
            ) {
                QuotePreview(
                    state = previewInputBase.copy(textStyle = style),
                    modifier = Modifier.padding(0.dp),
                    processedPhotoBitmap = processedPhotoBitmap,
                )
            }
        }
    }
}
