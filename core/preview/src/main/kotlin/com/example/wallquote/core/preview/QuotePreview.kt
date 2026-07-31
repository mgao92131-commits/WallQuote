package com.example.wallquote.core.preview

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.wallquote.domain.CollectionDefaults
import com.example.wallquote.domain.background.GradientGeometryCalculator
import com.example.wallquote.domain.layout.MeasuredQuoteText
import com.example.wallquote.domain.layout.QuoteBlockLayoutCalculator
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.QuoteRenderInput
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.style.TextStyleNormalizer
import kotlin.math.roundToInt

@Composable
fun QuotePreview(
    state: QuoteRenderInput,
    modifier: Modifier = Modifier,
    processedPhotoBitmap: Bitmap? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val densityScale = density.density

        when (val bg = state.background) {
            is BackgroundSpec.Solid -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(parseColorHex(bg.colorHex)),
                )
            }
            is BackgroundSpec.Gradient -> {
                val geometry = GradientGeometryCalculator.calculate(
                    width = widthPx,
                    height = heightPx,
                    angleDegrees = bg.angleDegrees,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    parseColorHex(bg.startColorHex),
                                    parseColorHex(bg.endColorHex),
                                ),
                                start = Offset(geometry.startX, geometry.startY),
                                end = Offset(geometry.endX, geometry.endY),
                            ),
                        ),
                )
            }
            is BackgroundSpec.Photo -> {
                // Neutral placeholder while the bitmap is loading or missing, rather than pure
                // black (which reads as a hard failure / empty state on real photo previews).
                Box(Modifier.fillMaxSize().background(parseColorHex(CollectionDefaults.DEFAULT_SOLID_HEX))) {
                    if (processedPhotoBitmap != null && !processedPhotoBitmap.isRecycled) {
                        Image(
                            bitmap = processedPhotoBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    val dim = bg.dimAmount.coerceIn(0f, 1f)
                    if (dim > 0f) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = dim)),
                        )
                    }
                }
            }
        }

        val style = TextStyleNormalizer.normalize(state.textStyle)
        val textAlign = when (style.horizontalAlignment) {
            HorizontalTextAlignment.Start -> TextAlign.Start
            HorizontalTextAlignment.End -> TextAlign.End
            HorizontalTextAlignment.Center -> TextAlign.Center
        }
        // Approximate measured text using max width; Compose measures during layout.
        // Share pivot / block padding rules with Canvas via QuoteBlockLayoutCalculator.
        val provisional = MeasuredQuoteText(
            widthPx = widthPx * 0.84f,
            heightPx = with(density) { (style.textSizeSp * style.lineHeightMultiplier * 3).sp.toPx() },
        )
        val layout = QuoteBlockLayoutCalculator.calculate(
            surfaceWidth = widthPx.roundToInt(),
            surfaceHeight = heightPx.roundToInt(),
            transform = state.transform,
            measuredText = provisional,
            style = style,
            density = densityScale,
        )
        val (shadowDx, shadowDy) = QuoteBlockLayoutCalculator.shadowOffsetPx(style, densityScale)
        val textShadow = if (TextStyleNormalizer.hasVisibleShadow(style)) {
            Shadow(
                color = parseColorHex(style.shadowColorHex).copy(alpha = style.shadowAlpha),
                offset = Offset(shadowDx, shadowDy),
                blurRadius = style.shadowRadiusDp * densityScale,
            )
        } else {
            null
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = (layout.pivotXPx - widthPx / 2f).roundToInt(),
                        y = (layout.pivotYPx - heightPx / 2f).roundToInt(),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            val blockShape = RoundedCornerShape(style.blockCornerRadiusDp.dp)
            val blockColor = style.blockColorHex
            val borderColor = style.blockBorderColorHex
            val hasBlock = TextStyleNormalizer.hasVisibleBlock(style) && blockColor != null
            // Border visibility must NOT depend on the block fill (P4-008): a border-only style
            // (no background fill) still needs to render its border and reserve padding.
            val hasBorder = style.blockBorderWidthDp > 0f &&
                borderColor != null &&
                style.blockBorderAlpha > 0f
            Box(
                modifier = Modifier
                    .rotate(layout.rotationDegrees)
                    .then(
                        if (hasBlock) {
                            Modifier.background(
                                color = parseColorHex(blockColor).copy(alpha = style.blockAlpha),
                                shape = blockShape,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (hasBorder) {
                            Modifier.border(
                                width = style.blockBorderWidthDp.dp,
                                color = parseColorHex(borderColor).copy(alpha = style.blockBorderAlpha),
                                shape = blockShape,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .then(
                        if (hasBlock || hasBorder) Modifier.padding(style.blockPaddingDp.dp) else Modifier,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.previewText,
                    modifier = Modifier.width(with(density) { layout.textLayoutWidthPx.toDp() }),
                    style = TextStyle(
                        color = parseColorHex(style.colorHex).copy(alpha = style.textAlpha),
                        fontSize = style.textSizeSp.sp,
                        fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal,
                        fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = resolveFontFamily(style.fontFamily),
                        textAlign = textAlign,
                        lineHeight = (style.textSizeSp * style.lineHeightMultiplier).sp,
                        letterSpacing = style.letterSpacingEm.em,
                        shadow = textShadow,
                    ),
                )
            }
        }
    }
}

fun resolveFontFamily(family: SystemFontFamily): FontFamily =
    when (family) {
        SystemFontFamily.SansSerif -> FontFamily.SansSerif
        SystemFontFamily.Monospace -> FontFamily.Monospace
        SystemFontFamily.Cursive -> FontFamily.Cursive
        SystemFontFamily.Serif -> FontFamily.Serif
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
