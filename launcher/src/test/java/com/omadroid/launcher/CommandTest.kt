package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandTest {
    @Test
    fun subsequenceMatches() {
        assertTrue(fuzzyScore("Theme", "thm") != null)
        assertNull(fuzzyScore("Theme", "xyz"))
    }

    @Test
    fun consecutiveBeatsScattered() {
        val tight = fuzzyScore("Settings", "set")!!
        val loose = fuzzyScore("Settings", "sgs")!!
        assertTrue(tight > loose)
    }

    @Test
    fun ranksTitleHitsFirst() {
        val items =
            listOf(
                CommandItem("a", "Files", "omadroid.dock", keywords = listOf("documents")),
                CommandItem("b", "Settings", "omadroid.dock"),
            )
        assertEquals(listOf("a"), fuzzySearch(items, "fil").map { it.id })
        assertEquals("a", fuzzySearch(items, "doc").first().id)
    }

    @Test
    fun blankQueryKeepsOrder() {
        val items =
            listOf(
                CommandItem("a", "Theme", "omadroid.sheet"),
                CommandItem("b", "Focus", "omadroid.dock"),
            )
        assertEquals(listOf("a", "b"), fuzzySearch(items, "").map { it.id })
    }
}
