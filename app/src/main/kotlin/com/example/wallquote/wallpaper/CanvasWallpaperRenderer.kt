package com.example.wallquote.wallpaper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.SurfaceHolder
import com.example.wallquote.domain.CollectionDefaults
import com.example.wallquote.domain.background.GradientGeometryCalculator
import com.example.wallquote.domain.layout.MeasuredQuoteText
import com.example.wallquote.domain.layout.QuoteBlockLayoutCalculator
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.model.WallpaperRenderSpec
import com.example.wallquote.domain.style.TextStyleNormalizer

class CanvasWallpaperRenderer(
    private val density: Float,
    private val fontScale: Float,
    private val diagnostics: WallpaperDiagnostics,
) : WallpaperRenderTarget {

    override fun render(
        holder: SurfaceHolder?,
        surfaceWidth: Int,
        surfaceHeight: Int,
        renderSpec: WallpaperRenderSpec,
        surfaceGeneration: Long,
        preparedPhoto: PreparedPhotoFrame?,
    ): RenderOutcome {
        if (holder == null || surfaceWidth <= 0 || surfaceHeight <= 0) {
            return RenderOutcome.Skipped
        }
        if (!holder.surface.isValid) {
            diagnostics.log("canvas_lock_failed", mapOf("reason" to "invalid_surface", "gen" to surfaceGeneration))
            return RenderOutcome.Failed("invalid_surface")
        }
        diagnostics.log("render_started", mapOf("gen" to surfaceGeneration, "w" to surfaceWidth, "h" to surfaceHeight))
        var canvas: Canvas? = null
        return try {
            canvas = holder.lockCanvas()
            if (canvas == null) {
                diagnostics.log("canvas_lock_failed", mapOf("gen" to surfaceGeneration))
                RenderOutcome.Failed("lock_null")
            } else {
                draw(canvas, surfaceWidth, surfaceHeight, renderSpec, preparedPhoto)
                diagnostics.log("render_completed", mapOf("gen" to surfaceGeneration))
                RenderOutcome.Success
            }
        } catch (error: Throwable) {
            diagnostics.log("render_failed", mapOf("gen" to surfaceGeneration, "error" to error.javaClass.simpleName))
            RenderOutcome.Failed(error.javaClass.simpleName)
        } finally {
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Throwable) {
                }
            }
        }
    }

    fun draw(
        canvas: Canvas,
        surfaceWidth: Int,
        surfaceHeight: Int,
        renderSpec: WallpaperRenderSpec,
        preparedPhoto: PreparedPhotoFrame? = null,
    ) {
        drawBackground(canvas, surfaceWidth, surfaceHeight, renderSpec.background, preparedPhoto)
        val text = renderSpec.text
        if (text.isNullOrBlank()) {
            if (renderSpec.showEmptyHint) drawHint(canvas, surfaceWidth, surfaceHeight)
            return
        }
        drawQuote(canvas, surfaceWidth, surfaceHeight, text, renderSpec)
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        background: BackgroundSpec,
        preparedPhoto: PreparedPhotoFrame?,
    ) {
        when (background) {
            is BackgroundSpec.Solid -> canvas.drawColor(parseColor(background.colorHex, DEFAULT_BG))
            is BackgroundSpec.Gradient -> {
                val start = parseColorOrNull(background.startColorHex)
                val end = parseColorOrNull(background.endColorHex)
                if (start == null || end == null) {
                    diagnostics.log("invalid_gradient_color")
                    canvas.drawColor(DEFAULT_BG)
                    return
                }
                val geometry = GradientGeometryCalculator.calculate(width.toFloat(), height.toFloat(), background.angleDegrees)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        geometry.startX, geometry.startY, geometry.endX, geometry.endY,
                        start, end, Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                diagnostics.log("gradient_rendered", mapOf("angle" to background.angleDegrees))
            }
            is BackgroundSpec.Photo -> {
                val bitmap = preparedPhoto?.bitmap
                if (bitmap != null && !bitmap.isRecycled) {
                    canvas.drawBitmap(
                        bitmap,
                        android.graphics.Rect(0, 0, bitmap.width, bitmap.height),
                        android.graphics.Rect(0, 0, width, height),
                        Paint(Paint.FILTER_BITMAP_FLAG),
                    )
                    val dim = preparedPhoto.dimAmount.coerceIn(0f, 1f)
                    if (dim > 0f) canvas.drawColor(Color.argb((dim * 255).toInt(), 0, 0, 0))
                    diagnostics.log("photo_rendered", mapOf("assetId" to background.assetId))
                } else {
                    canvas.drawColor(DEFAULT_BG)
                }
            }
        }
    }

    private fun drawHint(canvas: Canvas, width: Int, height: Int) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = spToPx(16f)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("打开「壁上言」添加内容", width / 2f, height / 2f, paint)
    }

    private fun drawQuote(
        canvas: Canvas,
        surfaceWidth: Int,
        surfaceHeight: Int,
        text: String,
        renderSpec: WallpaperRenderSpec,
    ) {
        val style = TextStyleNormalizer.normalize(renderSpec.textStyle)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(style.colorHex, Color.WHITE)
            alpha = ((style.textAlpha * renderSpec.transitionTextAlpha.coerceIn(0f, 1f)) * 255).toInt()
                .coerceIn(0, 255)
            textSize = spToPx(style.textSizeSp)
            typeface = resolveTypeface(style)
            letterSpacing = style.letterSpacingEm
        }
        val alignment = when (style.horizontalAlignment) {
            HorizontalTextAlignment.Start -> Layout.Alignment.ALIGN_NORMAL
            HorizontalTextAlignment.End -> Layout.Alignment.ALIGN_OPPOSITE
            HorizontalTextAlignment.Center -> Layout.Alignment.ALIGN_CENTER
        }
        val maxWidth = QuoteBlockLayoutCalculator.calculate(
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
            transform = renderSpec.transform,
            measuredText = MeasuredQuoteText(0f, 0f),
            style = style,
            density = density,
        ).textLayoutWidthPx
        if (maxWidth <= 0) return

        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, maxWidth)
            .setAlignment(alignment)
            .setLineSpacing(0f, style.lineHeightMultiplier.coerceAtLeast(0.8f))
            .setIncludePad(false)
            .build()

        val measured = MeasuredQuoteText(
            widthPx = staticLayout.width.toFloat(),
            heightPx = staticLayout.height.toFloat(),
        )
        val layout = QuoteBlockLayoutCalculator.calculate(
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
            transform = renderSpec.transform,
            measuredText = measured,
            style = style,
            density = density,
        )

        canvas.save()
        canvas.translate(layout.pivotXPx, layout.pivotYPx)
        canvas.rotate(layout.rotationDegrees)
        canvas.translate(-layout.pivotXPx, -layout.pivotYPx)

        val blockRect = RectF(layout.blockLeftPx, layout.blockTopPx, layout.blockRightPx, layout.blockBottomPx)
        val blockColorHex = style.blockColorHex
        if (TextStyleNormalizer.hasVisibleBlock(style) && blockColorHex != null) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.FILL
                color = parseColor(blockColorHex, Color.BLACK)
                alpha = (style.blockAlpha * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRoundRect(blockRect, style.blockCornerRadiusDp * density, style.blockCornerRadiusDp * density, fill)
        }
        val blockBorderColorHex = style.blockBorderColorHex
        if (style.blockBorderWidthDp > 0f && blockBorderColorHex != null && style.blockBorderAlpha > 0f) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.style = Paint.Style.STROKE
                strokeWidth = style.blockBorderWidthDp * density
                color = parseColor(blockBorderColorHex, Color.WHITE)
                alpha = (style.blockBorderAlpha * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawRoundRect(blockRect, style.blockCornerRadiusDp * density, style.blockCornerRadiusDp * density, stroke)
        }

        if (TextStyleNormalizer.hasVisibleShadow(style)) {
            val (dx, dy) = QuoteBlockLayoutCalculator.shadowOffsetPx(style, density)
            textPaint.setShadowLayer(
                style.shadowRadiusDp * density,
                dx,
                dy,
                Color.argb(
                    (style.shadowAlpha * 255).toInt().coerceIn(0, 255),
                    Color.red(parseColor(style.shadowColorHex, Color.BLACK)),
                    Color.green(parseColor(style.shadowColorHex, Color.BLACK)),
                    Color.blue(parseColor(style.shadowColorHex, Color.BLACK)),
                ),
            )
        } else {
            textPaint.clearShadowLayer()
        }

        val textLeft = layout.pivotXPx - measured.widthPx / 2f
        val textTop = layout.pivotYPx - measured.heightPx / 2f
        canvas.translate(textLeft, textTop)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun resolveTypeface(style: TextStyleConfig): Typeface {
        var styleFlag = Typeface.NORMAL
        if (style.isBold && style.isItalic) styleFlag = Typeface.BOLD_ITALIC
        else if (style.isBold) styleFlag = Typeface.BOLD
        else if (style.isItalic) styleFlag = Typeface.ITALIC
        // "cursive" is a generic Typeface family name resolved by the platform/device font
        // config, distinct from Typeface.DEFAULT (which previously made Cursive render
        // identically to Sans/Serif's default). Typeface.create(String, Int) looks it up by name.
        return when (style.fontFamily) {
            SystemFontFamily.SansSerif -> Typeface.create(Typeface.SANS_SERIF, styleFlag)
            SystemFontFamily.Monospace -> Typeface.create(Typeface.MONOSPACE, styleFlag)
            SystemFontFamily.Cursive -> Typeface.create("cursive", styleFlag)
            SystemFontFamily.Serif -> Typeface.create(Typeface.SERIF, styleFlag)
        }
    }

    fun spToPx(sp: Float): Float = sp * density * fontScale

    private fun parseColor(hex: String, fallback: Int): Int = parseColorOrNull(hex) ?: fallback

    private fun parseColorOrNull(hex: String): Int? =
        try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (_: Exception) {
            null
        }

    companion object {
        private val DEFAULT_BG = Color.parseColor(CollectionDefaults.DEFAULT_SOLID_HEX)
    }
}
