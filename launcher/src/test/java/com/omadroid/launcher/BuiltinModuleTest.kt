package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinModuleTest {
    @Test
    fun workspaceSwitcherMountsOnTheBarLeft() {
        val arranged = arrangeBar(composeBarPlacements(builtinModules(), defaultBarPlacements))
        assertEquals(listOf(BarModule.WorkspaceSwitcher), arranged[BarAnchor.Left])
        assertEquals(emptyList<BarModule>(), arranged[BarAnchor.Center])
        assertEquals(listOf(BarModule.Battery), arranged[BarAnchor.Right])
    }

    @Test
    fun omittingTheSwitcherLeavesTheBarAlone() {
        val modules = builtinModules().filter { it.module != BuiltinModule.WorkspaceSwitcher }
        val arranged = arrangeBar(composeBarPlacements(modules, defaultBarPlacements))
        assertEquals(emptyList<BarModule>(), arranged[BarAnchor.Left])
    }

    @Test
    fun moduleIdsAreFirstParty() {
        assertTrue(builtinModules().all { it.module.id.startsWith("omadroid.") })
    }
}
