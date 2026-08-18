package com.omadroid.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SlotTest {
    @Test
    fun growDefaultWeightIsOne() {
        val slot = Slot.grow()
        assertEquals(1f, slot.grow)
        assertNull(slot.units)
        assertFalse(slot.square)
    }

    @Test
    fun unitsHoldsCount() {
        val slot = Slot.units(2)
        assertEquals(2, slot.units)
        assertEquals(0f, slot.grow)
    }

    @Test
    fun squareIsCrossAxisSized() {
        assertTrue(Slot.square.square)
    }

    @Test
    fun fitIsEmpty() {
        assertEquals(Slot(), Slot.fit)
    }
}