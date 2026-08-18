package com.omadroid.clock

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmCodecTest {
    @Test
    fun roundTripsAlarms() {
        val alarms =
            listOf(
                SavedAlarm(1, 7, 30, true),
                SavedAlarm(2, 8, 0, false),
            )
        assertEquals(alarms, AlarmCodec.parse(AlarmCodec.serialize(alarms)))
    }

    @Test
    fun emptyStringIsNoAlarms() {
        assertEquals(emptyList<SavedAlarm>(), AlarmCodec.parse(""))
        assertEquals("", AlarmCodec.serialize(emptyList()))
    }

    @Test
    fun nextIdIsOnePastMax() {
        assertEquals(1, AlarmCodec.nextId(emptyList()))
        assertEquals(4, AlarmCodec.nextId(listOf(SavedAlarm(1, 7, 0, true), SavedAlarm(3, 8, 0, true))))
    }
}
