package com.example.wallquote.domain.automatch

import com.example.wallquote.domain.model.BackgroundSpec

/**
 * Stable key capturing everything that affects Auto Match sampling for a given background
 * (including photo dim/blur, which change the sampled pixels). Used to detect that a
 * background changed underneath an in-flight Auto Match request so its result can be
 * discarded instead of applied to a now-stale background.
 */
fun BackgroundSpec.autoMatchKey(): String = when (this) {
    is BackgroundSpec.Solid -> "solid:$colorHex"
    is BackgroundSpec.Gradient -> "gradient:$startColorHex:$endColorHex:$angleDegrees"
    is BackgroundSpec.Photo -> "photo:$assetId:$dimAmount:$blurRadiusDp"
}
