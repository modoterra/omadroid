package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrollingTest {
    @Test
    fun clampKeepsIndexInsideTheClientList() {
        assertEquals(0, clampScrollIndex(-3, 3))
        assertEquals(0, clampScrollIndex(0, 3))
        assertEquals(2, clampScrollIndex(2, 3))
        assertEquals(2, clampScrollIndex(9, 3))
    }

    @Test
    fun clampOnEmptyListIsZero() {
        assertEquals(0, clampScrollIndex(0, 0))
        assertEquals(0, clampScrollIndex(4, 0))
        assertEquals(0, clampScrollIndex(-1, 0))
    }

    @Test
    fun nextAndPrevClampAtTheEnds() {
        assertEquals(0, prevScrollIndex(0, 3))
        assertEquals(1, nextScrollIndex(0, 3))
        assertEquals(2, nextScrollIndex(1, 3))
        assertEquals(2, nextScrollIndex(2, 3))
        assertEquals(1, prevScrollIndex(2, 3))
        assertEquals(0, nextScrollIndex(0, 0))
        assertEquals(0, prevScrollIndex(0, 1))
        assertEquals(0, nextScrollIndex(0, 1))
    }

    @Test
    fun focusPageIsClampedPerWorkspace() {
        val clients =
            listOf(
                WorkspaceClient("a", "A", "pkg/a"),
                WorkspaceClient("b", "B", "pkg/b"),
            )
        val workspaces =
            defaultWorkspaces()
                .addClient("1", clients[0])
                .addClient("1", clients[1])
                .focusPage(8, "1")
        assertEquals(1, workspaces.items.first { it.id == "1" }.focusedIndex)
        assertEquals(0, workspaces.focusPage(-2, "1").items.first { it.id == "1" }.focusedIndex)
    }
}
