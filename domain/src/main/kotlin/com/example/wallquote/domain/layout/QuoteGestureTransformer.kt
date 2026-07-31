package com.example.wallquote.domain.layout

import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.style.QuoteTransformNormalizer

/**
 * Pure translation of a raw drag/rotate gesture delta into a normalized [QuoteTransform].
 *
 * Callers (e.g. Compose's `detectTransformGestures`) already report [rotationDeltaDegrees] in
 * degrees, not radians — this function must NOT re-convert it (see P4-001).
 */
object QuoteGestureTransformer {
    fun apply(
        current: QuoteTransform,
        panX: Float,
        panY: Float,
        viewportWidth: Int,
        viewportHeight: Int,
        rotationDeltaDegrees: Float,
    ): QuoteTransform {
        if (viewportWidth <= 0 || viewportHeight <= 0) return current
        val dxFraction = panX / viewportWidth
        val dyFraction = panY / viewportHeight
        return QuoteTransformNormalizer.normalize(
            centerXFraction = current.centerXFraction + dxFraction,
            centerYFraction = current.centerYFraction + dyFraction,
            rotationDegrees = current.rotationDegrees + rotationDeltaDegrees,
        )
    }
}
