package com.omadroid.launcher.widget

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class StackTest {
    @Test
    fun growTakesLeftoverAfterFixed() {
        val sizes =
            stackDistribute(
                innerMain = 100,
                gap = 4,
                bases = intArrayOf(20, 0, 20),
                grows = floatArrayOf(0f, 1f, 0f),
            )
        assertArrayEquals(intArrayOf(20, 52, 20), sizes)
    }

    @Test
    fun splitGrowByWeight() {
        val sizes =
            stackDistribute(
                innerMain = 80,
                gap = 0,
                bases = intArrayOf(0, 0),
                grows = floatArrayOf(1f, 3f),
            )
        assertEquals(20, sizes[0])
        assertEquals(60, sizes[1])
    }

    @Test
    fun noGrowLeavesBases() {
        val sizes =
            stackDistribute(
                innerMain = 200,
                gap = 8,
                bases = intArrayOf(10, 10),
                grows = floatArrayOf(0f, 0f),
            )
        assertArrayEquals(intArrayOf(10, 10), sizes)
    }
}
