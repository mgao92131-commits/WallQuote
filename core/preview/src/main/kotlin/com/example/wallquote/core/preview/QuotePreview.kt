package com.example.wallquote.core.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wallquote.domain.layout.QuoteLayoutCalculator
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteRenderInput
import kotlin.math.roundToInt

@Composable
fun QuotePreview(
    state: QuoteRenderInput,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = when (val bg = state.background) {
        is BackgroundSpec.Solid -> parseColorHex(bg.colorHex)
        is BackgroundSpec.Gradient -> parseColorHex(bg.startColorHex)
        is BackgroundSpec.Photo -> Color.Black
    }

    val textAlign = when (state.textStyle.alignment) {
        0 -> TextAlign.Start
        2 -> TextAlign.End
        else -> TextAlign.Center
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.roundToInt()
        val heightPx = with(density) { maxHeight.toPx() }.roundToInt()
        val layout = QuoteLayoutCalculator.calculate(
            surfaceWidth = widthPx,
            surfaceHeight = heightPx,
            transform = state.transform,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = layout.centerX.roundToInt() - widthPx / 2,
                        y = layout.centerY.roundToInt() - heightPx / 2,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.previewText,
                modifier = Modifier
                    .widthIn(max = with(density) { layout.maxTextWidth.toDp() })
                    .rotate(layout.rotationDegrees),
                style = TextStyle(
                    color = parseColorHex(state.textStyle.colorHex).copy(alpha = state.textStyle.textAlpha),
                    fontSize = state.textStyle.textSizeSp.sp,
                    fontWeight = if (state.textStyle.isBold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (state.textStyle.isItalic) FontStyle.Italic else FontStyle.Normal,
                    fontFamily = resolveFontFamily(state.textStyle.fontFamilyName),
                    textAlign = textAlign,
                    lineHeight = (state.textStyle.textSizeSp * state.textStyle.lineHeightMultiplier).sp,
                ),
            )
        }
    }
}

fun resolveFontFamily(name: String): FontFamily =
    when (name.lowercase()) {
        "sansserif", "sans_serif" -> FontFamily.SansSerif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.Serif
    }

fun parseColorHex(hex: String): Color {
    val normalized = hex.removePrefix("#")
    return try {
        val argb = when (normalized.length) {
            6 -> 0xFF000000L or normalized.toLong(16)
            8 -> normalized.toLong(16)
            else -> 0xFF2E3440L
        }
        Color(argb.toInt())
    } catch (_: Exception) {
        Color(0xFF2E3440)
    }
}
