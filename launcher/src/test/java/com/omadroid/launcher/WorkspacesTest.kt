package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspacesTest {
    @Test
    fun defaultsToThreeNumberedDesktops() {
        val workspaces = defaultWorkspaces()
        assertEquals(listOf("1", "2", "3"), workspaces.items.map { it.id })
        assertEquals("1", workspaces.activeId)
        assertEquals(WorkspaceLayout.Dwindle, workspaces.active.layout)
        assertTrue(workspaces.items.all { it.layout == WorkspaceLayout.Dwindle })
        assertTrue(workspaces.items.all { it.clients.isEmpty() })
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

    @Test
    fun addingAClientDoesNotAffectOtherWorkspaces() {
        val files = WorkspaceClient("c1", "Files", "pkg/files")
        val next = defaultWorkspaces().addClient("1", files)
        assertEquals(listOf(files), next.items.first { it.id == "1" }.clients)
        assertEquals(emptyList<WorkspaceClient>(), next.items.first { it.id == "2" }.clients)
        assertEquals(emptyList<WorkspaceClient>(), next.items.first { it.id == "3" }.clients)
        val both =
            next.addClient("2", WorkspaceClient("c2", "Clock", "pkg/clock"))
        assertEquals(listOf("c1"), both.items.first { it.id == "1" }.clients.map { it.id })
        assertEquals(listOf("c2"), both.items.first { it.id == "2" }.clients.map { it.id })
        assertEquals(emptyList<WorkspaceClient>(), both.items.first { it.id == "3" }.clients)
        assertEquals("1", both.activeId)
    }

    @Test
    fun layoutIsStoredPerWorkspace() {
        val next =
            defaultWorkspaces()
                .setLayout(WorkspaceLayout.Scrolling, "2")
                .cycleLayout("1")
        assertEquals(WorkspaceLayout.Scrolling, next.items.first { it.id == "1" }.layout)
        assertEquals(WorkspaceLayout.Scrolling, next.items.first { it.id == "2" }.layout)
        assertEquals(WorkspaceLayout.Dwindle, next.items.first { it.id == "3" }.layout)
    }

    @Test
    fun addClientOnActiveWorkspaceFocusesTheNewPage() {
        val next =
            defaultWorkspaces()
                .addClient(WorkspaceClient("a", "A", "p/a"))
                .addClient(WorkspaceClient("b", "B", "p/b"))
        assertEquals(1, next.active.focusedIndex)
        assertEquals(2, next.active.clients.size)
        assertEquals(0, next.items.first { it.id == "2" }.focusedIndex)
    }
}
