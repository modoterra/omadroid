package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspacesTest {
    @Test
    fun defaultsToThreeNumberedDesktops() {
        val workspaces = defaultWorkspaces()
        assertEquals(listOf("1", "2", "3"), workspaces.items.map { it.id })
        assertEquals("1", workspaces.activeId)
    }

    @Test
    fun selectMovesTheActiveWorkspace() {
        val next = defaultWorkspaces().select("3")
        assertEquals("3", next.activeId)
        assertEquals("3", next.active.name)
    }

    @Test
    fun selectIgnoresUnknownIds() {
        assertEquals("1", defaultWorkspaces().select("9").activeId)
    }

    @Test
    fun focusShowsOnlyTheActiveWorkspace() {
        val workspaces = defaultWorkspaces().select("2")
        assertEquals(listOf("2"), visibleWorkspaces(workspaces, LauncherLayout.Focus).map { it.id })
        assertEquals(
            listOf("1", "2", "3"),
            visibleWorkspaces(workspaces, LauncherLayout.Desktop).map { it.id },
        )
    }
}
