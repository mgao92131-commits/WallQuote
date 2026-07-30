package com.example.wallquote.core.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.wallquote.domain.background.GradientGeometryCalculator
import com.example.wallquote.domain.layout.QuoteLayoutCalculator
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteRenderInput
import java.io.File
import kotlin.math.roundToInt

@Composable
fun QuotePreview(
    state: QuoteRenderInput,
    modifier: Modifier = Modifier,
    resolvedPhotoPath: String? = null,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

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
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    val path = resolvedPhotoPath
                    if (path != null) {
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (bg.blurRadiusDp > 0f) {
                                        Modifier.blur(bg.blurRadiusDp.dp)
                                    } else {
                                        Modifier
                                    },
                                ),
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

        val textAlign = when (state.textStyle.alignment) {
            0 -> TextAlign.Start
            2 -> TextAlign.End
            else -> TextAlign.Center
        }
        val layout = QuoteLayoutCalculator.calculate(
            surfaceWidth = widthPx.roundToInt(),
            surfaceHeight = heightPx.roundToInt(),
            transform = state.transform,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = layout.centerX.roundToInt() - widthPx.roundToInt() / 2,
                        y = layout.centerY.roundToInt() - heightPx.roundToInt() / 2,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.previewText,
                modifier = Modifier
                    .width(with(density) { layout.maxTextWidth.toDp() })
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
