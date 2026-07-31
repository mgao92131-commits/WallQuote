package com.example.wallquote.automatch

import android.graphics.Bitmap
import com.example.wallquote.domain.automatch.BackgroundSample
import com.example.wallquote.domain.automatch.Contrast
import com.example.wallquote.domain.automatch.autoMatchKey
import com.example.wallquote.domain.background.GradientGeometryCalculator
import com.example.wallquote.domain.model.BackgroundSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Samples the current background so [com.example.wallquote.domain.automatch.AutoStyleMatcher]
 * can suggest a contrast-safe text style. Sampling for photos runs off the main thread.
 */
interface BackgroundSampler {
    suspend fun sample(background: BackgroundSpec, photoBitmap: Bitmap?): BackgroundSample
}

@Singleton
class DefaultBackgroundSampler @Inject constructor() : BackgroundSampler {

    private val cacheLock = Any()
    private val cache = LinkedHashMap<String, BackgroundSample>()

    override suspend fun sample(background: BackgroundSpec, photoBitmap: Bitmap?): BackgroundSample {
        val key = cacheKey(background)
        synchronized(cacheLock) { cache[key] }?.let { return it }

        val sample = withContext(Dispatchers.IO) {
            when (background) {
                is BackgroundSpec.Solid -> sampleSolid(background.colorHex)
                is BackgroundSpec.Gradient -> sampleGradient(background)
                is BackgroundSpec.Photo -> samplePhoto(photoBitmap, background.dimAmount)
            }
        }

        synchronized(cacheLock) {
            cache[key] = sample
            if (cache.size > MAX_CACHE_ENTRIES) {
                cache.remove(cache.keys.first())
            }
        }
        return sample
    }

    /** Reuses [autoMatchKey] so the sample cache and Auto Match staleness checks stay in sync
     * (both must invalidate together when dim/blur/asset/colors change). */
    private fun cacheKey(background: BackgroundSpec): String = background.autoMatchKey()

    private fun sampleSolid(colorHex: String): BackgroundSample =
        buildSample(listOf(parseHex(colorHex)))

    /**
     * Sample colors at 25/50/75% of the gradient axis. [GradientGeometryCalculator] gives the same
     * axis used to render the gradient (on a normalized unit square), so the fractional distance
     * along that axis is used to linearly interpolate the two stop colors.
     */
    private fun sampleGradient(spec: BackgroundSpec.Gradient): BackgroundSample {
        val geometry = GradientGeometryCalculator.calculate(
            width = 1f,
            height = 1f,
            angleDegrees = spec.angleDegrees,
        )
        val axisLength = kotlin.math.hypot(
            (geometry.endX - geometry.startX).toDouble(),
            (geometry.endY - geometry.startY).toDouble(),
        ).toFloat()
        val start = parseHex(spec.startColorHex)
        val end = parseHex(spec.endColorHex)
        val samples = GRADIENT_SAMPLE_FRACTIONS.map { t ->
            // Fraction along the axis maps 1:1 to color-stop fraction for a two-stop linear gradient.
            val axisT = if (axisLength > 0f) t else 0.5f
            lerpColor(start, end, axisT)
        }
        return buildSample(samples)
    }

    /**
     * Samples the decoded (un-dimmed) preview bitmap and applies [dimAmount] to each sampled
     * pixel (multiplying RGB by `1 - dim`) so the suggestion matches what will actually be
     * rendered on top of the dimmed photo, not the raw source image.
     */
    private fun samplePhoto(bitmap: Bitmap?, dimAmount: Float): BackgroundSample {
        if (bitmap == null || bitmap.isRecycled) {
            return buildSample(listOf(applyDim(NEUTRAL_GRAY, dimAmount)))
        }
        val small = Bitmap.createScaledBitmap(bitmap, PHOTO_GRID_SIZE, PHOTO_GRID_SIZE, true)
        val colors = ArrayList<Long>(PHOTO_GRID_SIZE * PHOTO_GRID_SIZE)
        for (y in 0 until PHOTO_GRID_SIZE) {
            for (x in 0 until PHOTO_GRID_SIZE) {
                val pixel = small.getPixel(x, y)
                val argb = (pixel.toLong() and 0x00FFFFFFL) or 0xFF000000L
                colors += applyDim(argb, dimAmount)
            }
        }
        if (small !== bitmap) small.recycle()
        return buildSample(colors)
    }

    private fun applyDim(argb: Long, dimAmount: Float): Long {
        val factor = 1f - dimAmount.coerceIn(0f, 1f)
        fun channel(shift: Int): Long =
            (((argb shr shift) and 0xFF) * factor).toLong().coerceIn(0L, 255L)
        return 0xFF000000L or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private fun buildSample(colors: List<Long>): BackgroundSample {
        if (colors.isEmpty()) return buildSample(listOf(NEUTRAL_GRAY))
        val average = averageColor(colors)
        val dominant = dominantColor(colors)
        return BackgroundSample(
            averageColorArgb = average,
            dominantColorArgb = dominant,
            averageLuminance = Contrast.relativeLuminance(average),
            contrastWithWhite = Contrast.contrastWithWhite(average),
            contrastWithBlack = Contrast.contrastWithBlack(average),
            visualComplexity = luminanceVariance(colors),
        )
    }

    private fun averageColor(colors: List<Long>): Long {
        var r = 0L
        var g = 0L
        var b = 0L
        colors.forEach { c ->
            r += (c shr 16) and 0xFF
            g += (c shr 8) and 0xFF
            b += c and 0xFF
        }
        val n = colors.size
        return 0xFF000000L or ((r / n) shl 16) or ((g / n) shl 8) or (b / n)
    }

    /** Coarsely quantized most-frequent color bucket. */
    private fun dominantColor(colors: List<Long>): Long {
        val buckets = HashMap<Long, Int>()
        colors.forEach { c -> buckets.merge(quantize(c), 1, Int::plus) }
        return buckets.maxByOrNull { it.value }?.key ?: colors.first()
    }

    private fun quantize(color: Long): Long {
        fun bucket(v: Long) = (v / 32) * 32
        val r = bucket((color shr 16) and 0xFF)
        val g = bucket((color shr 8) and 0xFF)
        val b = bucket(color and 0xFF)
        return 0xFF000000L or (r shl 16) or (g shl 8) or b
    }

    /** Normalized standard-deviation of luminance in [0,1]; higher means busier background. */
    private fun luminanceVariance(colors: List<Long>): Float {
        if (colors.size < 2) return 0f
        val luminances = colors.map { Contrast.relativeLuminance(it) }
        val mean = luminances.average().toFloat()
        val variance = luminances.sumOf { l -> val d = (l - mean).toDouble(); d * d } / luminances.size
        return (sqrt(variance).toFloat() / 0.5f).coerceIn(0f, 1f)
    }

    private fun lerpColor(start: Long, end: Long, t: Float): Long {
        fun channel(v: Long, shift: Int) = ((v shr shift) and 0xFF).toInt()
        fun mix(a: Int, b: Int) = (a + (b - a) * t).toInt().coerceIn(0, 255)
        val r = mix(channel(start, 16), channel(end, 16))
        val g = mix(channel(start, 8), channel(end, 8))
        val b = mix(channel(start, 0), channel(end, 0))
        return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    private fun parseHex(hex: String): Long {
        val n = hex.removePrefix("#")
        return when (n.length) {
            6 -> 0xFF000000L or n.toLong(16)
            8 -> n.toLong(16)
            else -> NEUTRAL_GRAY
        }
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 16
        private const val PHOTO_GRID_SIZE = 8
        private const val NEUTRAL_GRAY = 0xFF808080L
        private val GRADIENT_SAMPLE_FRACTIONS = listOf(0.25f, 0.5f, 0.75f)
    }
}
