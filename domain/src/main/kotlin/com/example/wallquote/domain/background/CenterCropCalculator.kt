package com.example.wallquote.domain.background

data class CropTransform(
    val scale: Float,
    val srcLeft: Float,
    val srcTop: Float,
    val srcRight: Float,
    val srcBottom: Float,
    val dstLeft: Float,
    val dstTop: Float,
    val dstRight: Float,
    val dstBottom: Float,
)

/**
 * Center-crop mapping shared by Compose preview and Canvas wallpaper.
 */
object CenterCropCalculator {

    fun calculate(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): CropTransform? {
        if (sourceWidth <= 0 || sourceHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
            return null
        }
        val scale = maxOf(
            targetWidth.toFloat() / sourceWidth.toFloat(),
            targetHeight.toFloat() / sourceHeight.toFloat(),
        )
        val scaledWidth = sourceWidth * scale
        val scaledHeight = sourceHeight * scale
        val dx = (scaledWidth - targetWidth) / 2f
        val dy = (scaledHeight - targetHeight) / 2f

        val srcLeft = dx / scale
        val srcTop = dy / scale
        val srcRight = (dx + targetWidth) / scale
        val srcBottom = (dy + targetHeight) / scale

        return CropTransform(
            scale = scale,
            srcLeft = srcLeft.coerceIn(0f, sourceWidth.toFloat()),
            srcTop = srcTop.coerceIn(0f, sourceHeight.toFloat()),
            srcRight = srcRight.coerceIn(0f, sourceWidth.toFloat()),
            srcBottom = srcBottom.coerceIn(0f, sourceHeight.toFloat()),
            dstLeft = 0f,
            dstTop = 0f,
            dstRight = targetWidth.toFloat(),
            dstBottom = targetHeight.toFloat(),
        )
    }
}
