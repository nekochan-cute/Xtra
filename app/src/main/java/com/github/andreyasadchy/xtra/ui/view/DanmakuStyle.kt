package com.github.andreyasadchy.xtra.ui.view

/**
 * Appearance values shared by the settings screen and the Canvas renderer.
 * Invalid or obsolete preferences must never produce unusable text or timing.
 */
internal data class DanmakuStyle(
    val textSizeSp: Int = 20,
    val travelTimeSeconds: Int = 6,
    val outlineWidthDp: Int = 3,
    val opacityPercent: Int = 100,
    val maxLanes: Int = 12,
    val textColorName: String = "white",
    val outlineColorName: String = "black",
) {
    companion object {
        fun sanitized(
            textSizeSp: Int = 20,
            travelTimeSeconds: Int = 6,
            outlineWidthDp: Int = 3,
            opacityPercent: Int = 100,
            maxLanes: Int = 12,
            textColorName: String? = "white",
            outlineColorName: String? = "black",
        ) = DanmakuStyle(
            textSizeSp = textSizeSp.coerceIn(12, 48),
            travelTimeSeconds = travelTimeSeconds.coerceIn(3, 15),
            outlineWidthDp = outlineWidthDp.coerceIn(0, 6),
            opacityPercent = opacityPercent.coerceIn(20, 100),
            maxLanes = maxLanes.coerceIn(1, 12),
            textColorName = textColorName?.takeIf { it in setOf("white", "yellow", "cyan", "green", "pink") } ?: "white",
            outlineColorName = outlineColorName?.takeIf { it in setOf("black", "white", "dark_gray") } ?: "black",
        )
    }
}
