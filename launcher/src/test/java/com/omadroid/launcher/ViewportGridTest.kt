package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class ViewportGridTest {
    @Test
    fun dividesTheViewportIntoCells() {
        val grid = measureGrid(widthPx = 1080, heightPx = 1920, unitPx = 48)
        assertEquals(22, grid.columns)
        assertEquals(40, grid.rows)
        assertEquals(24, grid.extraWidthPx)
        assertEquals(0, grid.extraHeightPx)
    }

    @Test
    fun barsAreOneUnitAndWorkspacesGetTheRest() {
        val grid = measureGrid(1080, 1920, 48)
        val slots = allocateSpace(grid, defaultSpaceClaims())
        val bar = slots.getValue(BuiltinModule.Bar)
        val workspaces = slots.getValue(BuiltinModule.Workspaces)
        val launcher = slots.getValue(BuiltinModule.LauncherBar)

        assertEquals(1, bar.units.rows)
        assertEquals(0, bar.units.row)
        assertEquals(48, bar.pixels.height)

        assertEquals(1, launcher.units.rows)
        assertEquals(39, launcher.units.row)
        assertEquals(48, launcher.pixels.height)

        assertEquals(1, workspaces.units.row)
        assertEquals(38, workspaces.units.rows)
        assertEquals(38 * 48, workspaces.pixels.height)
        assertEquals(1080, workspaces.pixels.width)
    }

    @Test
    fun leftoverHeightGoesToTheFillSlot() {
        val grid = measureGrid(100, 100, 48)
        val slots = allocateSpace(grid, defaultSpaceClaims())
        assertEquals(48, slots.getValue(BuiltinModule.Bar).pixels.height)
        assertEquals(48, slots.getValue(BuiltinModule.LauncherBar).pixels.height)
        assertEquals(4, slots.getValue(BuiltinModule.Workspaces).pixels.height)
        assertEquals(100, slots.getValue(BuiltinModule.Workspaces).pixels.width)
    }

    @Test
    fun unitLengthUsesThePendingConstant() {
        assertEquals(96, unitLengthPx(2f))
    }
}
