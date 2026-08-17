package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class BarStatusTest {
    @Test
    fun formatsWeekdayMonthDay() {
        val date = LocalDate.of(2026, 8, 17)
        assertEquals("Mon, Aug 17", formatBarDate(date, Locale.US))
    }

    @Test
    fun formatsBatteryPercent() {
        assertEquals("42%", formatBattery(42, false))
        assertEquals("100%+", formatBattery(140, true))
        assertEquals("0%", formatBattery(-3, false))
    }

    @Test
    fun scalesStickyBatteryExtras() {
        assertEquals(50, readBatteryPercent(50, 100))
        assertEquals(50, readBatteryPercent(5, 10))
        assertEquals(0, readBatteryPercent(-1, 100))
    }

    @Test
    fun describesWifiAndBattery() {
        assertEquals("Wi-Fi connected", wifiContentDescription(WifiState.Connected))
        assertEquals("Wi-Fi off", wifiContentDescription(WifiState.Off))
        assertEquals(
            "Battery 18 percent, charging",
            batteryContentDescription(18, true, Locale.US),
        )
    }
}
