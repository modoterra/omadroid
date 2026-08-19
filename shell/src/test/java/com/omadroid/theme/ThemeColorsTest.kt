package com.omadroid.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

class ThemeColorsTest {
    @Test
    fun parsesOmarchySemanticHex() {
        val theme = ThemeColors.parse("tokyo-night", TOKYO)

        assertEquals("tokyo-night", theme.slug)
        assertEquals("dark", theme.mode)
        assertEquals(0xFF1A1B26.toInt(), theme.background)
        assertEquals(0xFFA9B1D6.toInt(), theme.foreground)
        assertEquals(0xFF7AA2F7.toInt(), theme.accent)
        assertEquals(0xFF414868.toInt(), theme.muted)
        assertEquals(theme.accent, theme.activeBorder)
        assertEquals(theme.muted, theme.inactiveBorder)
    }

    @Test
    fun parsesShippedTokyoNightFile() {
        val file = File("themes/tokyo-night/colors.toml")
        if (!file.isFile) {
            return
        }
        val theme = ThemeColors.parse("tokyo-night", file.readText())
        assertEquals(0xFF1A1B26.toInt(), theme.background)
        assertEquals(0xFFA9B1D6.toInt(), theme.foreground)
    }

    @Test
    fun parsesEveryShippedOmarchyPalette() {
        val root = File("themes")
        if (!root.isDirectory) {
            return
        }
        val slugs = root.listFiles()?.filter { it.resolve("colors.toml").isFile } ?: emptyList()
        assert(slugs.isNotEmpty())
        slugs.forEach { dir ->
            ThemeColors.parse(dir.name, dir.resolve("colors.toml").readText())
        }
    }

    @Test
    fun displayNameTitleCasesSlug() {
        assertEquals("Tokyo Night", ThemeCatalog.displayName("tokyo-night"))
        assertEquals("Catppuccin Latte", ThemeCatalog.displayName("catppuccin-latte"))
    }

    @Test
    fun readsOmarchyWindowBorders() {
        val theme =
            ThemeColors.parse(
                "last-horizon",
                """
                mode = "dark"
                accent = "#b59790"
                muted = "#584e51"
                background = "#0c0b0c"
                lighter_background = "#0c0b0c"
                foreground = "#FAFCFB"
                red = "#c38b7b"
                yellow = "#6B5E73"
                green = "#87a9b0"
                cyan = "#a5a0b6"
                blue = "#b59790"
                magenta = "#c4d8e2"
                hyprland_active_border = "rgba(8a8588ee) rgba(e2dddcee)"
                hyprland_inactive_border = "rgba(584e51aa)"
                active_border_color = "#d6d3de"
                """.trimIndent(),
            )
        assertEquals(0xFFD6D3DE.toInt(), theme.activeBorder)
        assertEquals(0xAA584E51.toInt(), theme.inactiveBorder)
    }

    @Test
    fun rejectsMissingBackground() {
        assertThrows(ThemeColorsException::class.java) {
            ThemeColors.parse("broken", """mode = "dark"\nforeground = "#ffffff"\n""")
        }
    }

    companion object {
        const val TOKYO = """
mode = "dark"
accent = "#7aa2f7"
selection = "#292e42"
muted = "#414868"
background = "#1a1b26"
dark_background = "#13141c"
darker_background = "#0e0e14"
lighter_background = "#24283b"
foreground = "#a9b1d6"
dark_foreground = "#565f89"
light_foreground = "#b4bee6"
bright_foreground = "#c0caf5"
red = "#f7768e"
yellow = "#e0af68"
orange = "#eb927b"
green = "#9ece6a"
cyan = "#449dab"
blue = "#7aa2f7"
magenta = "#ad8ee6"
brown = "#75493d"
"""
    }
}
