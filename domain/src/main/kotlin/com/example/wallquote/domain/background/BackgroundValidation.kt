package com.example.wallquote.domain.background

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.PhotoScaleMode
import kotlin.math.roundToInt
import kotlin.math.sqrt

object BackgroundValidation {

    private val HEX_COLOR = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")
    private val FORMAL_ASSET_ID = Regex("^bg_[0-9a-f]{32}$")
    private val STAGING_TOKEN = Regex("^draft_[0-9a-f-]{36}$")

    fun isValidColorHex(hex: String): Boolean = HEX_COLOR.matches(normalizeColor(hex))

    fun isValidAssetId(assetId: String): Boolean = FORMAL_ASSET_ID.matches(assetId.trim())

    fun isValidStagingToken(token: String): Boolean = STAGING_TOKEN.matches(token.trim())

    fun normalize(spec: BackgroundSpec): BackgroundSpec =
        when (spec) {
            is BackgroundSpec.Solid -> spec.copy(colorHex = expandColor(normalizeColor(spec.colorHex)))
            is BackgroundSpec.Gradient -> spec.copy(
                startColorHex = expandColor(normalizeColor(spec.startColorHex)),
                endColorHex = expandColor(normalizeColor(spec.endColorHex)),
                angleDegrees = normalizeAngle(spec.angleDegrees),
            )
            is BackgroundSpec.Photo -> spec.copy(
                assetId = spec.assetId.trim(),
                dimAmount = spec.dimAmount.coerceIn(BackgroundLimits.DIM_MIN, BackgroundLimits.DIM_MAX),
                blurRadiusDp = spec.blurRadiusDp.coerceIn(
                    BackgroundLimits.BLUR_MIN_DP,
                    BackgroundLimits.BLUR_MAX_DP,
                ),
                scaleMode = PhotoScaleMode.CenterCrop,
            )
        }

    fun validate(spec: BackgroundSpec): String? =
        when (val normalized = normalize(spec)) {
            is BackgroundSpec.Solid ->
                if (!isValidColorHex(normalized.colorHex)) "背景颜色无效" else null
            is BackgroundSpec.Gradient -> when {
                !isValidColorHex(normalized.startColorHex) -> "渐变起始颜色无效"
                !isValidColorHex(normalized.endColorHex) -> "渐变结束颜色无效"
                else -> null
            }
            is BackgroundSpec.Photo -> when {
                normalized.assetId.isBlank() -> "请先选择图片"
                isValidStagingToken(normalized.assetId) -> null
                !isValidAssetId(normalized.assetId) -> "图片资产无效，请重新选择"
                else -> null
            }
        }

    /** Formal persisted photo asset ids only. */
    fun isUsablePhotoAssetId(assetId: String): Boolean = isValidAssetId(assetId)

    fun isExternalUri(value: String): Boolean =
        value.startsWith("content:", ignoreCase = true) ||
            value.startsWith("file:", ignoreCase = true) ||
            value.startsWith("http:", ignoreCase = true) ||
            value.startsWith("https:", ignoreCase = true)

    private fun normalizeColor(hex: String): String {
        val trimmed = hex.trim()
        return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
    }

    private fun expandColor(hex: String): String {
        val body = hex.removePrefix("#")
        return when (body.length) {
            3 -> "#" + body.map { "$it$it" }.joinToString("")
            else -> "#$body"
        }
    }

    fun normalizeAngle(degrees: Float): Float {
        if (!degrees.isFinite()) return 0f
        var value = degrees % 360f
        if (value < 0f) value += 360f
        return value.coerceIn(BackgroundLimits.ANGLE_MIN, BackgroundLimits.ANGLE_MAX)
    }
}

data class ProcessedImageSize(
    val width: Int,
    val height: Int,
)

object ProcessedImageSizeCalculator {
    fun calculate(
        targetWidth: Int,
        targetHeight: Int,
        blurRadiusDp: Float,
    ): ProcessedImageSize {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return ProcessedImageSize(0, 0)
        }
        val withBlur = blurRadiusDp > 0f
        val maxPixels = if (withBlur) {
            BackgroundLimits.MAX_PROCESS_PIXELS_BLUR
        } else {
            BackgroundLimits.MAX_PROCESS_PIXELS_SHARP
        }
        var width = targetWidth
        var height = targetHeight
        val pixels = width.toLong() * height.toLong()
        if (pixels > maxPixels) {
            val scale = sqrt(maxPixels.toDouble() / pixels.toDouble())
            width = maxOf(1, (width * scale).toInt())
            height = maxOf(1, (height * scale).toInt())
            // Guard rounding so we never exceed the pixel budget.
            while (width.toLong() * height > maxPixels && (width > 1 || height > 1)) {
                if (width >= height && width > 1) width-- else if (height > 1) height-- else break
            }
        }
        if (withBlur) {
            val maxEdge = BackgroundLimits.MAX_PROCESS_EDGE_BLUR
            val edge = maxOf(width, height)
            if (edge > maxEdge) {
                val scale = maxEdge.toFloat() / edge.toFloat()
                width = maxOf(1, (width * scale).toInt())
                height = maxOf(1, (height * scale).toInt())
            }
        }
        return ProcessedImageSize(width, height)
    }

    fun effectiveBlurRadiusPx(blurRadiusDp: Float, density: Float): Int {
        if (blurRadiusDp <= 0f) return 0
        return (blurRadiusDp * density).roundToInt().coerceIn(1, 25)
    }
}
