package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DwindleTest {
    @Test
    fun zeroClientsIsEmpty() {
        assertNull(dwindleTree(emptyList<String>()))
        assertEquals(emptyList<Pair<ClientId, DwindleRect>>(), dwindleRects(200, 100, null))
        assertEquals(emptyList<Pair<ClientId, DwindleRect>>(), dwindleRects(200, 100, dwindleTree(emptyList())))
    }

    @Test
    fun oneClientFillsTheWorkspace() {
        val rects = dwindleRects(200, 100, tree("a"))
        assertEquals(listOf("a" to DwindleRect(0, 0, 200, 100)), rects)
        assertAreasAddUp(200, 100, rects)
    }

    @Test
    fun twoClientsSplitVertically() {
        val rects = dwindleRects(200, 100, tree("a", "b"))
        assertEquals(
            listOf(
                "a" to DwindleRect(0, 0, 100, 100),
                "b" to DwindleRect(100, 0, 100, 100),
            ),
            rects,
        )
        assertAreasAddUp(200, 100, rects)
        val root = tree("a", "b") as DwindleNode.Split
        assertEquals(DwindleAxis.Vertical, root.axis)
    }

    @Test
    fun threeClientsThenSplitHorizontally() {
        val rects = dwindleRects(200, 100, tree("a", "b", "c"))
        assertEquals(
            listOf(
                "a" to DwindleRect(0, 0, 100, 100),
                "b" to DwindleRect(100, 0, 100, 50),
                "c" to DwindleRect(100, 50, 100, 50),
            ),
            rects,
        )
        assertAreasAddUp(200, 100, rects)
        val root = tree("a", "b", "c") as DwindleNode.Split
        assertEquals(DwindleAxis.Vertical, root.axis)
        val last = root.second as DwindleNode.Split
        assertEquals(DwindleAxis.Horizontal, last.axis)
    }

    @Test
    fun fourClientsAlternateBackToVertical() {
        val rects = dwindleRects(200, 100, tree("a", "b", "c", "d"))
        assertEquals(
            listOf(
                "a" to DwindleRect(0, 0, 100, 100),
                "b" to DwindleRect(100, 0, 100, 50),
                "c" to DwindleRect(100, 50, 50, 50),
                "d" to DwindleRect(150, 50, 50, 50),
            ),
            rects,
        )
        assertAreasAddUp(200, 100, rects)
        val root = tree("a", "b", "c", "d") as DwindleNode.Split
        assertEquals(DwindleAxis.Vertical, root.axis)
        val mid = root.second as DwindleNode.Split
        assertEquals(DwindleAxis.Horizontal, mid.axis)
        val last = mid.second as DwindleNode.Split
        assertEquals(DwindleAxis.Vertical, last.axis)
    }

    @Test
    fun oddSizesStillAddUp() {
        val rects = dwindleRects(201, 99, tree("a", "b", "c", "d"))
        assertAreasAddUp(201, 99, rects)
        assertEquals(4, rects.size)
        assertTrue(rects.all { it.second.width >= 0 && it.second.height >= 0 })
    }

    private fun tree(vararg ids: String): DwindleNode? = dwindleTree(ids.toList())

    private fun assertAreasAddUp(width: Int, height: Int, rects: List<Pair<ClientId, DwindleRect>>) {
        assertEquals(width.toLong() * height, rects.sumOf { it.second.area })
    }
}
