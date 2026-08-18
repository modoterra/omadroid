package com.omadroid.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeBackgroundTest {
    @Test
    fun picksSortedWallpaperAndSkipsLogo() {
        val files =
            ThemeCatalog.backgroundFiles(
                arrayOf("omarchy.png", "2-waves.png", "1-totoro.png", ".hidden"),
            )
        assertEquals(listOf("1-totoro.png", "2-waves.png"), files)
    }

    @Test
    fun tokyoNightShipsABackground() {
        val dir = java.io.File("../shell/themes/tokyo-night/backgrounds")
        if (!dir.isDirectory) {
            return
        }
        val names = ThemeCatalog.backgroundFiles(dir.list())
        assertTrue(names.isNotEmpty())
    }
}