package com.omadroid.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerStateTest {
    @Test
    fun remainingIsDurationWhileIdle() {
        val timer = TimerState(durationMillis = 60_000L, remainingAtPause = 60_000L)
        assertEquals(60_000L, timer.remaining(0L))
        assertFalse(timer.running)
    }

    @Test
    fun remainingCountsDownWhileRunning() {
        val timer =
            TimerState(
                durationMillis = 10_000L,
                remainingAtPause = 10_000L,
            ).start(1_000L)
        assertTrue(timer.running)
        assertEquals(7_000L, timer.remaining(4_000L))
    }

    @Test
    fun remainingDoesNotGoNegative() {
        val timer =
            TimerState(durationMillis = 1_000L, remainingAtPause = 1_000L).start(0L)
        assertEquals(0L, timer.remaining(5_000L))
    }

    @Test
    fun pauseFreezesRemaining() {
        val running = TimerState(durationMillis = 10_000L, remainingAtPause = 10_000L).start(0L)
        val paused = running.pause(3_000L)
        assertFalse(paused.running)
        assertEquals(7_000L, paused.remaining(3_000L))
        assertEquals(7_000L, paused.remaining(9_000L))
    }

    @Test
    fun resetRestoresDuration() {
        val reset =
            TimerState(durationMillis = 5_000L, remainingAtPause = 5_000L)
                .start(0L)
                .pause(2_000L)
                .reset()
        assertFalse(reset.running)
        assertEquals(5_000L, reset.remaining(100L))
    }
}
