package com.omadroid.clock

data class StopwatchState(
    val running: Boolean = false,
    val accumulated: Long = 0L,
    val runStartedElapsed: Long = 0L,
    val laps: List<Long> = emptyList(),
) {
    fun elapsed(nowElapsed: Long): Long {
        if (!running) {
            return accumulated.coerceAtLeast(0L)
        }
        return (accumulated + (nowElapsed - runStartedElapsed)).coerceAtLeast(0L)
    }

    fun start(nowElapsed: Long): StopwatchState =
        copy(running = true, runStartedElapsed = nowElapsed)

    fun pause(nowElapsed: Long): StopwatchState =
        copy(running = false, accumulated = elapsed(nowElapsed))

    fun reset(): StopwatchState = StopwatchState()

    fun lap(nowElapsed: Long): StopwatchState {
        val mark = elapsed(nowElapsed)
        if (mark <= 0L) {
            return this
        }
        return copy(laps = laps + mark)
    }
}
