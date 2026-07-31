package com.example.wallquote.domain.style

import com.example.wallquote.domain.model.TextStyleConfig

/**
 * Repairs a [TextStyleConfig] read back from storage (e.g. a corrupt or hand-edited DB row/JSON
 * blob) into a safe, renderable style. Unlike [TextStyleValidator], this never surfaces an error
 * to the user — it always returns a usable style, silently coercing out-of-range or invalid
 * fields via [TextStyleNormalizer.normalize].
 *
 * Use [TextStyleValidator] to reject bad *user input* before it is saved; use this to defensively
 * sanitize data already at rest.
 */
object TextStyleSanitizer {
    fun sanitize(style: TextStyleConfig): TextStyleConfig = TextStyleNormalizer.normalize(style)
}
