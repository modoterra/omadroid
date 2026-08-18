package com.omadroid.clock

data class TimerState(
    val durationMillis: Long = 5 * 60_000L,
    val running: Boolean = false,
    val remainingAtPause: Long = 5 * 60_000L,
    val runStartedElapsed: Long = 0L,
) {
    fun remaining(nowElapsed: Long): Long {
        if (!running) {
            return remainingAtPause.coerceAtLeast(0L)
        }
        return (remainingAtPause - (nowElapsed - runStartedElapsed)).coerceAtLeast(0L)
    }

    fun start(nowElapsed: Long): TimerState {
        val left = remaining(nowElapsed)
        if (left <= 0L) {
            return this
        }
        return copy(running = true, remainingAtPause = left, runStartedElapsed = nowElapsed)
    }

    fun pause(nowElapsed: Long): TimerState =
        copy(running = false, remainingAtPause = remaining(nowElapsed))

    fun reset(): TimerState =
        copy(running = false, remainingAtPause = durationMillis.coerceAtLeast(0L))

    fun withDuration(millis: Long): TimerState {
        val next = millis.coerceAtLeast(0L)
        return TimerState(durationMillis = next, remainingAtPause = next)
    }
}
