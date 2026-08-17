package com.omadroid.launcher.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class IconGlyphsTest {
    @Test
    fun batteryUsesPlugWhenCharging() {
        assertEquals(IconGlyphs.PLUG, IconGlyphs.battery(10, true))
        assertEquals(IconGlyphs.PLUG, IconGlyphs.battery(100, true))
    }

    @Test
    fun batteryStepsByPercent() {
        assertEquals(IconGlyphs.BATTERY_FULL, IconGlyphs.battery(90, false))
        assertEquals(IconGlyphs.BATTERY_THREE_QUARTERS, IconGlyphs.battery(60, false))
        assertEquals(IconGlyphs.BATTERY_HALF, IconGlyphs.battery(40, false))
        assertEquals(IconGlyphs.BATTERY_QUARTER, IconGlyphs.battery(20, false))
        assertEquals(IconGlyphs.BATTERY_EMPTY, IconGlyphs.battery(5, false))
    }
}
