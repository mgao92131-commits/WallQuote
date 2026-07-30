package com.example.wallquote.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.model.WallpaperRenderSpec
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CanvasWallpaperRendererTest {

    private val diagnostics = object : WallpaperDiagnostics {
        val events = mutableListOf<String>()
        override fun log(event: String, details: Map<String, Any?>) {
            events += event
        }
    }

    private val renderer = CanvasWallpaperRenderer(density = 2f, diagnostics = diagnostics)

    @Test
    fun drawsSolidChineseEnglishAndEmojiWithoutCrash() {
        val bitmap = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888)
        renderer.draw(
            canvas = Canvas(bitmap),
            surfaceWidth = 200,
            surfaceHeight = 400,
            renderSpec = WallpaperRenderSpec(
                background = BackgroundSpec.Solid("#112233"),
                text = "你好 WallQuote\nHello 🙂",
                textStyle = TextStyleConfig(isBold = true, isItalic = true, alignment = 0),
                transform = QuoteTransform(0.5f, 0.4f, 15f),
            ),
        )
        renderer.draw(
            Canvas(bitmap),
            200,
            400,
            WallpaperRenderSpec(
                background = BackgroundSpec.Solid("#000000"),
                text = "centered",
                textStyle = TextStyleConfig(alignment = 1, fontFamilyName = "Monospace"),
                transform = QuoteTransform(),
            ),
        )
        renderer.draw(
            Canvas(bitmap),
            200,
            400,
            WallpaperRenderSpec(
                background = BackgroundSpec.Solid("#FFFFFF"),
                text = "right",
                textStyle = TextStyleConfig(alignment = 2, fontFamilyName = "SansSerif"),
                transform = QuoteTransform(0.7f, 0.7f, -10f),
            ),
        )
    }

    @Test
    fun invalidColorAndGradientDoNotCrash() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        renderer.draw(
            Canvas(bitmap),
            100,
            100,
            WallpaperRenderSpec(
                background = BackgroundSpec.Solid("not-a-color"),
                text = "x",
                textStyle = TextStyleConfig(colorHex = "bad"),
                transform = QuoteTransform(),
            ),
        )
        renderer.draw(
            Canvas(bitmap),
            50,
            50,
            WallpaperRenderSpec(
                background = BackgroundSpec.Gradient("#111111", "#222222"),
                text = null,
                textStyle = TextStyleConfig(),
                transform = QuoteTransform(),
                showEmptyHint = true,
            ),
        )
        assertTrue(diagnostics.events.contains("invalid_background_fallback"))
    }

    @Test
    fun zeroSizeDoesNotCrash() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        renderer.draw(
            Canvas(bitmap),
            0,
            0,
            WallpaperRenderSpec(
                background = BackgroundSpec.Solid("#000000"),
                text = "emoji 🙂",
                textStyle = TextStyleConfig(),
                transform = QuoteTransform(),
            ),
        )
    }
}
