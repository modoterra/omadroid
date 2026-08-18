package com.omadroid.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchStateTest {
    @Test
    fun elapsedCountsUpWhileRunning() {
        val watch = StopwatchState().start(100L)
        assertTrue(watch.running)
        assertEquals(400L, watch.elapsed(500L))
    }

    @Test
    fun pauseFreezesElapsed() {
        val paused = StopwatchState().start(0L).pause(250L)
        assertFalse(paused.running)
        assertEquals(250L, paused.elapsed(250L))
        assertEquals(250L, paused.elapsed(900L))
    }

    @Test
    fun lapRecordsCurrentElapsed() {
        val lapped = StopwatchState().start(0L).lap(1_250L)
        assertEquals(listOf(1_250L), lapped.laps)
        assertEquals(1_250L, lapped.elapsed(1_250L))
    }

    @Test
    fun resetClearsElapsedAndLaps() {
        val reset = StopwatchState().start(0L).lap(100L).reset()
        assertEquals(0L, reset.elapsed(50L))
        assertEquals(emptyList<Long>(), reset.laps)
        assertFalse(reset.running)
    }
}
