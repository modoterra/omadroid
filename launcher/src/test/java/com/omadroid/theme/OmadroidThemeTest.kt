package com.omadroid.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class OmadroidThemeTest {
    @Test
    fun emptySlugFallsBackToTokyoNight() {
        assertEquals(ThemeColors.DEFAULT_SLUG, OmadroidTheme.normalizeSlug(null))
        assertEquals(ThemeColors.DEFAULT_SLUG, OmadroidTheme.normalizeSlug(""))
        assertEquals(ThemeColors.DEFAULT_SLUG, OmadroidTheme.normalizeSlug("   "))
    }

    @Test
    fun keepsARealSlug() {
        assertEquals("gruvbox", OmadroidTheme.normalizeSlug("gruvbox"))
        assertEquals("catppuccin-latte", OmadroidTheme.normalizeSlug(" catppuccin-latte "))
    }

    @Test
    fun missingProviderDoesNotEscape() {
        OmadroidTheme.withoutThemeProvider {
            throw SecurityException("Failed to find provider com.omadroid.theme")
        }
        OmadroidTheme.withoutThemeProvider {
            throw IllegalArgumentException("Unknown URI")
        }
    }
}
