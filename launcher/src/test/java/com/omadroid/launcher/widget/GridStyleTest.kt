package com.omadroid.launcher.widget

import com.omadroid.theme.ThemeColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridStyleTest {
    @Test
    fun spaceIsEqualAndInnerFitsTheCell() {
        val style = GridStyle(unitPx = 32, colors = stubColors())
        assertEquals(32, style.cellPx)
        assertEquals(4, style.spacePx)
        assertEquals(24, style.innerPx)
        assertEquals(style.innerPx + 2 * style.spacePx, style.cellPx)
        assertEquals(0f, style.cornerPx)
        assertTrue(style.textSizePx <= style.innerPx)
        assertEquals(14, style.textSizePx)
        assertEquals(12, style.captionSizePx)
        assertTrue(style.captionSizePx < style.textSizePx)
        assertEquals(style.textSizePx, style.iconPx)
        assertEquals(8, style.workspaceInsetPx)
        assertEquals(16, style.chromeBlurPx)
        assertEquals(0.9f, style.chromeFillAlpha)
    }

    @Test
    fun scalesWithTheUnit() {
        val style = GridStyle(unitPx = 64, colors = stubColors())
        assertEquals(8, style.spacePx)
        assertEquals(48, style.innerPx)
        assertEquals(style.spacePx * 2 + style.innerPx, style.cellPx)
    }

    private fun stubColors(): ThemeColors =
        ThemeColors.parse(
            "test",
            """
            background = "#000000"
            foreground = "#ffffff"
            lighter_background = "#111111"
            muted = "#888888"
            accent = "#00ff00"
            red = "#ff0000"
            yellow = "#ffff00"
            green = "#00ff00"
            cyan = "#00ffff"
            blue = "#0000ff"
            magenta = "#ff00ff"
            """.trimIndent(),
        )
}
