package com.omadroid.launcher.widget

import com.omadroid.launcher.UNIT_DP
import com.omadroid.theme.ThemeColors

data class GridStyle(
    val unitPx: Int,
    val colors: ThemeColors,
) {
    val cellPx: Int = unitPx
    val insetPx: Int = (unitPx / 6).coerceAtLeast(1)
    val gapPx: Int = (unitPx / 6).coerceAtLeast(1)
    val tightGapPx: Int = (unitPx / 12).coerceAtLeast(1)
    val iconPx: Int = (unitPx * 20 / UNIT_DP).coerceAtLeast(1)
    val textSizePx: Int = (unitPx * 14 / UNIT_DP).coerceAtLeast(1)
    val cornerPx: Float = (unitPx / 6).toFloat()
}
