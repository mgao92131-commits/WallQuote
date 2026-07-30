package com.example.wallquote.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.SurfaceHolder
import com.example.wallquote.domain.CollectionDefaults
import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.background.GradientGeometryCalculator
import com.example.wallquote.domain.layout.QuoteLayoutCalculator
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.model.WallpaperRenderSpec

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

        diagnostics.log(
            "render_started",
            mapOf(
                "gen" to surfaceGeneration,
                "w" to surfaceWidth,
                "h" to surfaceHeight,
            ),
        )

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
            diagnostics.log(
                "render_failed",
                mapOf("gen" to surfaceGeneration, "error" to error.javaClass.simpleName),
            )
            RenderOutcome.Failed(error.javaClass.simpleName)
        } finally {
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (_: Throwable) {
                    // Surface may already be gone.
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
            if (renderSpec.showEmptyHint) {
                drawHint(canvas, surfaceWidth, surfaceHeight)
            }
            return
        }
        drawQuote(canvas, surfaceWidth, surfaceHeight, text, renderSpec.textStyle, renderSpec)
    }

    private fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        background: BackgroundSpec,
        preparedPhoto: PreparedPhotoFrame?,
    ) {
        when (background) {
            is BackgroundSpec.Solid -> {
                canvas.drawColor(parseColor(background.colorHex, DEFAULT_BG))
            }
            is BackgroundSpec.Gradient -> {
                val start = parseColorOrNull(background.startColorHex)
                val end = parseColorOrNull(background.endColorHex)
                if (start == null || end == null) {
                    diagnostics.log("invalid_gradient_color")
                    canvas.drawColor(DEFAULT_BG)
                    return
                }
                val geometry = GradientGeometryCalculator.calculate(
                    width = width.toFloat(),
                    height = height.toFloat(),
                    angleDegrees = background.angleDegrees,
                )
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        geometry.startX,
                        geometry.startY,
                        geometry.endX,
                        geometry.endY,
                        start,
                        end,
                        Shader.TileMode.CLAMP,
                    )
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                diagnostics.log("gradient_rendered", mapOf("angle" to background.angleDegrees))
            }
            is BackgroundSpec.Photo -> {
                val bitmap = preparedPhoto?.bitmap
                if (bitmap != null && !bitmap.isRecycled) {
                    drawPhoto(canvas, width, height, bitmap, preparedPhoto.dimAmount)
                    diagnostics.log("photo_rendered", mapOf("assetId" to background.assetId))
                } else {
                    canvas.drawColor(DEFAULT_BG)
                }
            }
        }
    }

    private fun drawPhoto(
        canvas: Canvas,
        width: Int,
        height: Int,
        bitmap: Bitmap,
        dimAmount: Float,
    ) {
        val src = android.graphics.Rect(0, 0, bitmap.width, bitmap.height)
        val dst = android.graphics.Rect(0, 0, width, height)
        canvas.drawBitmap(bitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        val dim = dimAmount.coerceIn(0f, 1f)
        if (dim > 0f) {
            canvas.drawColor(Color.argb((dim * 255).toInt(), 0, 0, 0))
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
        style: TextStyleConfig,
        renderSpec: WallpaperRenderSpec,
    ) {
        val layoutInfo = QuoteLayoutCalculator.calculate(
            surfaceWidth = surfaceWidth,
            surfaceHeight = surfaceHeight,
            transform = renderSpec.transform,
        )
        if (layoutInfo.maxTextWidth <= 0) return

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor(style.colorHex, Color.WHITE)
            alpha = (style.textAlpha.coerceIn(0f, 1f) * 255).toInt()
            textSize = spToPx(style.textSizeSp)
            typeface = resolveTypeface(style)
        }

        val alignment = when (style.alignment) {
            0 -> Layout.Alignment.ALIGN_NORMAL
            2 -> Layout.Alignment.ALIGN_OPPOSITE
            else -> Layout.Alignment.ALIGN_CENTER
        }

        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, layoutInfo.maxTextWidth)
            .setAlignment(alignment)
            .setLineSpacing(0f, style.lineHeightMultiplier.coerceAtLeast(0.8f))
            .setIncludePad(false)
            .build()

        val dx = layoutInfo.centerX - staticLayout.width / 2f
        val dy = layoutInfo.centerY - staticLayout.height / 2f

        canvas.save()
        canvas.translate(layoutInfo.centerX, layoutInfo.centerY)
        canvas.rotate(layoutInfo.rotationDegrees)
        canvas.translate(-layoutInfo.centerX, -layoutInfo.centerY)
        canvas.translate(dx, dy)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun resolveTypeface(style: TextStyleConfig): Typeface {
        val family = when (style.fontFamilyName.lowercase()) {
            "sansserif", "sans_serif" -> Typeface.SANS_SERIF
            "monospace" -> Typeface.MONOSPACE
            "cursive" -> Typeface.DEFAULT
            else -> Typeface.SERIF
        }
        var styleFlag = Typeface.NORMAL
        if (style.isBold && style.isItalic) styleFlag = Typeface.BOLD_ITALIC
        else if (style.isBold) styleFlag = Typeface.BOLD
        else if (style.isItalic) styleFlag = Typeface.ITALIC
        return Typeface.create(family, styleFlag)
    }

    fun spToPx(sp: Float): Float = sp * density * fontScale

    private fun parseColor(hex: String, fallback: Int): Int =
        parseColorOrNull(hex) ?: fallback

    private fun parseColorOrNull(hex: String): Int? {
        if (!BackgroundValidation.isValidColorHex(if (hex.startsWith("#")) hex else "#$hex")) {
            // Still try Color.parseColor for lenient editor inputs without forcing validation.
        }
        return try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val DEFAULT_BG = Color.parseColor(CollectionDefaults.DEFAULT_SOLID_HEX)
    }
}
