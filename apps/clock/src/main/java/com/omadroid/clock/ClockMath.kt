package com.omadroid.clock

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

object ClockMath {
    fun wrapHour(hour: Int): Int = ((hour % 24) + 24) % 24

    fun wrapMinute(minute: Int): Int = ((minute % 60) + 60) % 60

    fun wrapSecond(second: Int): Int = ((second % 60) + 60) % 60

    fun formatHm(hour: Int, minute: Int): String =
        String.format(Locale.US, "%02d:%02d", wrapHour(hour), wrapMinute(minute))

    fun formatDuration(millis: Long): String {
        val totalSec = (millis.coerceAtLeast(0L) / 1000L)
        val hours = totalSec / 3600L
        val minutes = (totalSec % 3600L) / 60L
        val seconds = totalSec % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatStopwatch(millis: Long): String {
        val total = millis.coerceAtLeast(0L)
        val centis = (total / 10L) % 100L
        val seconds = (total / 1000L) % 60L
        val minutes = (total / 60_000L) % 60L
        val hours = total / 3_600_000L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d.%02d", hours, minutes, seconds, centis)
        } else {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
        }
    }

    fun nextAlarmMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        var next =
            now.withHour(wrapHour(hour))
                .withMinute(wrapMinute(minute))
                .withSecond(0)
                .withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.toInstant().toEpochMilli()
    }
}
