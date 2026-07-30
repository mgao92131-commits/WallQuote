package com.example.wallquote.domain.background

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.PhotoScaleMode

object BackgroundValidation {

    private val HEX_COLOR = Regex("^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")

    fun isValidColorHex(hex: String): Boolean = HEX_COLOR.matches(normalizeColor(hex))

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
                isExternalUri(normalized.assetId) -> "图片资产无效，请重新选择"
                else -> null
            }
        }

    fun isUsablePhotoAssetId(assetId: String): Boolean {
        val trimmed = assetId.trim()
        return trimmed.isNotEmpty() && !isExternalUri(trimmed)
    }

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
