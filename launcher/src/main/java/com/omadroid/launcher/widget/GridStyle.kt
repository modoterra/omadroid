package com.omadroid.launcher.widget

import com.omadroid.launcher.UNIT_DP
import com.omadroid.theme.ThemeColors

data class GridStyle(
    val unitPx: Int,
    val colors: ThemeColors,
) {
    val cellPx: Int = unitPx
    /** Same length for every pad and margin. */
    val spacePx: Int = (unitPx / 8).coerceAtLeast(1)
    val innerPx: Int = (unitPx - 2 * spacePx).coerceAtLeast(1)
    val textSizePx: Int = (unitPx * 14 / UNIT_DP).coerceAtLeast(1)
    val captionSizePx: Int = (unitPx * 12 / UNIT_DP).coerceAtLeast(1)
    val iconPx: Int = textSizePx
    val cornerPx: Float = 0f
    /** Gap between the workspace and the bar, dock, and screen edges. */
    val workspaceInsetPx: Int = spacePx * 2
    /** Blur radius for wallpaper under bar, dock, and Command. */
    val chromeBlurPx: Int = unitPx.coerceAtLeast(1)
    /** Theme wash over that blur. 1f is a solid theme plate. */
    val chromeFillAlpha: Float = 0.55f
    /** Hyprland-style window outline. */
    val windowBorderPx: Int = (unitPx * 2 / UNIT_DP).coerceAtLeast(1)
}
