package com.example.wallquote.ui.editor.background

import com.example.wallquote.domain.model.BackgroundSpec

data class BackgroundPreset(
    val id: String,
    val label: String,
    val spec: BackgroundSpec,
)

object BackgroundPresets {
    val all: List<BackgroundPreset> = listOf(
        BackgroundPreset("solid_black", "纯黑", BackgroundSpec.Solid("#000000")),
        BackgroundPreset(
            "aurora_blue",
            "极光蓝",
            BackgroundSpec.Gradient("#0B1D36", "#4F8FBF", 135f),
        ),
        BackgroundPreset(
            "sunrise_orange",
            "日出橙",
            BackgroundSpec.Gradient("#C44536", "#F4A261", 45f),
        ),
        BackgroundPreset(
            "violet",
            "紫罗兰",
            BackgroundSpec.Gradient("#3D2C56", "#9B8EC4", 120f),
        ),
        BackgroundPreset("night_purple", "暗夜紫", BackgroundSpec.Solid("#2E3440")),
    )

    fun matching(spec: BackgroundSpec): BackgroundPreset? =
        all.firstOrNull { preset -> specsMatch(preset.spec, spec) }

    private fun specsMatch(a: BackgroundSpec, b: BackgroundSpec): Boolean =
        when {
            a is BackgroundSpec.Solid && b is BackgroundSpec.Solid ->
                a.colorHex.equals(b.colorHex, ignoreCase = true)
            a is BackgroundSpec.Gradient && b is BackgroundSpec.Gradient ->
                a.startColorHex.equals(b.startColorHex, ignoreCase = true) &&
                    a.endColorHex.equals(b.endColorHex, ignoreCase = true) &&
                    kotlin.math.abs(a.angleDegrees - b.angleDegrees) < 1f
            else -> false
        }
}
