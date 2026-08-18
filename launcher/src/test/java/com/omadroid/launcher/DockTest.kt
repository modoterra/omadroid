package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class DockTest {
    @Test
    fun commandFieldIsASquareWhenClosed() {
        assertEquals(32, commandFieldWidthPx(open = false, dockWidthPx = 360, cellPx = 32, spacePx = 4))
    }

    @Test
    fun commandFieldFillsTheDockWhenOpen() {
        // 360 - layout 32 - menu 32 - two gaps of 4
        assertEquals(288, commandFieldWidthPx(open = true, dockWidthPx = 360, cellPx = 32, spacePx = 4))
    }

    @Test
    fun commandFieldNeverShrinksBelowASquare() {
        assertEquals(32, commandFieldWidthPx(open = true, dockWidthPx = 40, cellPx = 32, spacePx = 4))
    }
}
