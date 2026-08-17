package com.omadroid.launcher.widget

import com.omadroid.theme.ThemeColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GridStyleTest {
    @Test
    fun oneControlFitsInOneUnit() {
        val style = GridStyle(unitPx = 48, colors = stubColors())
        assertEquals(48, style.cellPx)
        assertTrue(style.textSizePx < style.cellPx)
        assertTrue(style.iconPx < style.cellPx)
        assertTrue(style.insetPx * 2 + style.textSizePx <= style.cellPx)
    }

    @Test
    fun scalesWithTheUnit() {
        val style = GridStyle(unitPx = 96, colors = stubColors())
        assertEquals(16, style.insetPx)
        assertEquals(40, style.iconPx)
        assertEquals(28, style.textSizePx)
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
