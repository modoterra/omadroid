package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class BarLayoutTest {
    @Test
    fun defaultPutsStatusOnRight() {
        val arranged = arrangeBar(defaultBarPlacements)
        assertEquals(emptyList<BarModule>(), arranged[BarAnchor.Center])
        assertEquals(listOf(BarModule.Wifi, BarModule.Battery), arranged[BarAnchor.Right])
        assertEquals(emptyList<BarModule>(), arranged[BarAnchor.Left])
    }

    @Test
    fun keepsOrderInsideEachAnchor() {
        val arranged =
            arrangeBar(
                listOf(
                    BarPlacement(BarModule.Wifi, BarAnchor.Left),
                    BarPlacement(BarModule.Date, BarAnchor.Left),
                    BarPlacement(BarModule.Battery, BarAnchor.Center),
                ),
            )
        assertEquals(listOf(BarModule.Wifi, BarModule.Date), arranged[BarAnchor.Left])
        assertEquals(listOf(BarModule.Battery), arranged[BarAnchor.Center])
        assertEquals(emptyList<BarModule>(), arranged[BarAnchor.Right])
    }

    @Test
    fun includesEveryAnchorEvenWhenEmpty() {
        val arranged = arrangeBar(emptyList())
        assertEquals(setOf(BarAnchor.Left, BarAnchor.Center, BarAnchor.Right), arranged.keys)
    }
}
