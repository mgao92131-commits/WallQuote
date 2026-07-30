package com.example.wallquote.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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

    private val renderer = CanvasWallpaperRenderer(
        density = 2f,
        fontScale = 1f,
        diagnostics = diagnostics,
    )

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
    }

    @Test
    fun gradientZeroDegrees_logsRendered() {
        val bitmap = Bitmap.createBitmap(100, 40, Bitmap.Config.ARGB_8888)
        renderer.draw(
            Canvas(bitmap),
            100,
            40,
            WallpaperRenderSpec(
                background = BackgroundSpec.Gradient("#FF0000", "#0000FF", angleDegrees = 0f),
                text = null,
                textStyle = TextStyleConfig(),
                transform = QuoteTransform(),
            ),
        )
        assertTrue(diagnostics.events.contains("gradient_rendered"))
    }

    @Test
    fun photoWithDim_logsPhotoRendered() {
        val photo = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888).also {
            it.eraseColor(Color.WHITE)
        }
        val bitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888)
        renderer.draw(
            Canvas(bitmap),
            20,
            20,
            WallpaperRenderSpec(
                background = BackgroundSpec.Photo(assetId = "bg_x", dimAmount = 0.5f),
                text = "hi",
                textStyle = TextStyleConfig(colorHex = "#00FF00"),
                transform = QuoteTransform(),
            ),
            preparedPhoto = PreparedPhotoFrame(photo, dimAmount = 0.5f),
        )
        assertTrue(diagnostics.events.contains("photo_rendered"))
    }

    @Test
    fun invalidColorFallsBackWithoutCrash() {
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
                background = BackgroundSpec.Gradient("bad", "#222222"),
                text = null,
                textStyle = TextStyleConfig(),
                transform = QuoteTransform(),
                showEmptyHint = true,
            ),
        )
        assertTrue(diagnostics.events.contains("invalid_gradient_color"))
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
