package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavStackTest {
    @Test
    fun emptyUntilRooted() {
        val empty = NavStack()
        assertFalse(empty.isOpen)
        assertNull(empty.current)
        assertFalse(empty.canPop)
    }

    @Test
    fun pushAndPop() {
        val search = NavRoute("search", "Search")
        val app = NavRoute("app", "Settings")
        val stacked = NavStack.root(search).push(app)
        assertEquals(app, stacked.current)
        assertTrue(stacked.canPop)
        val back = stacked.pop()
        assertEquals(search, back.current)
        assertFalse(back.canPop)
        assertTrue(back.pop().routes.isEmpty())
    }
}
