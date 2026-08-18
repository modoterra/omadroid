package com.omadroid.gallery.widget

import com.omadroid.theme.ThemeColors

const val UNIT_DP = 32

data class GridStyle(
    val unitPx: Int,
    val colors: ThemeColors,
) {
    val cellPx: Int = unitPx
    /** Same length for every pad and margin. */
    val spacePx: Int = (unitPx / 8).coerceAtLeast(1)
    val innerPx: Int = (unitPx - 2 * spacePx).coerceAtLeast(1)
    val textSizePx: Int = (unitPx * 14 / UNIT_DP).coerceAtLeast(1)
    val iconPx: Int = textSizePx
    val cornerPx: Float = 0f
}

fun unitLengthPx(density: Float): Int = (UNIT_DP * density).toInt().coerceAtLeast(1)
