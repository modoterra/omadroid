package com.omadroid.clock

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class ClockMathTest {
    @Test
    fun formatsHourMinuteWithPadding() {
        assertEquals("07:05", ClockMath.formatHm(7, 5))
        assertEquals("00:00", ClockMath.formatHm(0, 0))
        assertEquals("23:59", ClockMath.formatHm(23, 59))
    }

    @Test
    fun wrapsHourAndMinute() {
        assertEquals(0, ClockMath.wrapHour(24))
        assertEquals(23, ClockMath.wrapHour(-1))
        assertEquals(0, ClockMath.wrapMinute(60))
        assertEquals(59, ClockMath.wrapMinute(-1))
    }

    @Test
    fun formatsDurationUnderAnHourAsMmSs() {
        assertEquals("05:00", ClockMath.formatDuration(5 * 60_000L))
        assertEquals("00:00", ClockMath.formatDuration(0))
        assertEquals("00:01", ClockMath.formatDuration(1999))
        assertEquals("1:01:01", ClockMath.formatDuration(3_661_000L))
    }

    @Test
    fun formatsStopwatchWithCentiseconds() {
        assertEquals("00:00.00", ClockMath.formatStopwatch(0))
        assertEquals("00:01.50", ClockMath.formatStopwatch(1500))
        assertEquals("01:02.03", ClockMath.formatStopwatch(62_030L))
        assertEquals("1:00:00.00", ClockMath.formatStopwatch(3_600_000L))
    }

    @Test
    fun nextAlarmIsTodayWhenTimeIsStillAhead() {
        val zone = ZoneOffset.UTC
        val now = LocalDateTime.of(2026, 3, 15, 10, 0).atZone(zone).toInstant().toEpochMilli()
        val next = ClockMath.nextAlarmMillis(10, 30, now, zone)
        val expected = LocalDateTime.of(2026, 3, 15, 10, 30).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, next)
    }

    @Test
    fun nextAlarmRollsToTomorrowWhenTimeHasPassed() {
        val zone = ZoneOffset.UTC
        val now = LocalDateTime.of(2026, 3, 15, 10, 0).atZone(zone).toInstant().toEpochMilli()
        val next = ClockMath.nextAlarmMillis(10, 0, now, zone)
        val expected = LocalDateTime.of(2026, 3, 16, 10, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, next)
    }

    @Test
    fun nextAlarmCrossesMidnight() {
        val zone = ZoneOffset.UTC
        val now = LocalDateTime.of(2026, 3, 15, 23, 30).atZone(zone).toInstant().toEpochMilli()
        val next = ClockMath.nextAlarmMillis(0, 15, now, zone)
        val expected = LocalDateTime.of(2026, 3, 16, 0, 15).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, next)
    }
}
