package com.omadroid.launcher.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class MenuSpecTest {
    @Test
    fun flattensSectionsInOrder() {
        val spec =
            MenuSpec(
                listOf(
                    MenuSection("Desktop", listOf(MenuItem("focus", "Focus", toggled = false))),
                    MenuSection(
                        "Apps",
                        listOf(
                            MenuItem("settings", "Settings", icon = "s"),
                            MenuItem("files", "Files"),
                        ),
                    ),
                ),
            )
        assertEquals(
            listOf("focus", "settings", "files"),
            menuRows(spec).map { it.id },
        )
    }

    @Test
    fun toggleUpdatesOnlyThatItem() {
        val spec =
            MenuSpec(
                listOf(
                    MenuSection(
                        "Desktop",
                        listOf(
                            MenuItem("focus", "Focus", toggled = false),
                            MenuItem("sound", "Sound", toggled = true),
                        ),
                    ),
                ),
            )
        val next = withToggled(spec, "focus", true)
        assertEquals(true, menuRows(next).first { it.id == "focus" }.toggled)
        assertEquals(true, menuRows(next).first { it.id == "sound" }.toggled)
    }
}
