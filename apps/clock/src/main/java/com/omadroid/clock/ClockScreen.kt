package com.omadroid.clock

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omadroid.compose.Block
import com.omadroid.compose.Cell
import com.omadroid.compose.Chrome
import com.omadroid.compose.Compose
import com.omadroid.compose.Direction
import com.omadroid.compose.Glyph
import com.omadroid.compose.Line
import com.omadroid.compose.LocalGridStyle
import com.omadroid.compose.Page
import com.omadroid.compose.Rule
import com.omadroid.compose.Slot
import com.omadroid.compose.Stack
import com.omadroid.compose.Text
import com.omadroid.compose.cellDp
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.launcher.widget.IconGlyphs

enum class ClockTab(val label: String) {
    Alarms("Alarms"),
    Timer("Timer"),
    Stopwatch("Stopwatch"),
}

@Composable
fun ClockScreen(
    style: GridStyle,
    tab: ClockTab,
    alarms: List<SavedAlarm>,
    draftHour: Int,
    draftMinute: Int,
    exactAlarms: Boolean,
    timer: TimerState,
    stopwatch: StopwatchState,
    nowElapsed: Long,
    onTab: (ClockTab) -> Unit,
    onDraftHour: (Int) -> Unit,
    onDraftMinute: (Int) -> Unit,
    onAddAlarm: () -> Unit,
    onToggleAlarm: (SavedAlarm) -> Unit,
    onDeleteAlarm: (SavedAlarm) -> Unit,
    onTimer: (TimerState) -> Unit,
    onStopwatch: (StopwatchState) -> Unit,
) {
    Compose(style) {
        Stack(Direction.Vertical, gap = false) {
            Chrome("Clock")
            Node(Slot.units(1)) {
                Stack(Direction.Horizontal, gap = false, pad = false) {
                    ClockTab.entries.forEach { item ->
                        Node(Slot.grow()) {
                            Cell(
                                align = Alignment.Center,
                                onClick = { onTab(item) },
                                description = item.label,
                            ) {
                                Text(
                                    item.label,
                                    color = if (item == tab) style.colors.accent else style.colors.muted,
                                    description = item.label,
                                )
                            }
                        }
                    }
                }
            }
            Node(Slot.grow()) {
                when (tab) {
                    ClockTab.Alarms ->
                        AlarmsPane(
                            alarms = alarms,
                            draftHour = draftHour,
                            draftMinute = draftMinute,
                            exactAlarms = exactAlarms,
                            onDraftHour = onDraftHour,
                            onDraftMinute = onDraftMinute,
                            onAdd = onAddAlarm,
                            onToggle = onToggleAlarm,
                            onDelete = onDeleteAlarm,
                        )
                    ClockTab.Timer ->
                        TimerPane(timer, nowElapsed, onTimer)
                    ClockTab.Stopwatch ->
                        StopwatchPane(stopwatch, nowElapsed, onStopwatch)
                }
            }
        }
    }
}

@Composable
private fun AlarmsPane(
    alarms: List<SavedAlarm>,
    draftHour: Int,
    draftMinute: Int,
    exactAlarms: Boolean,
    onDraftHour: (Int) -> Unit,
    onDraftMinute: (Int) -> Unit,
    onAdd: () -> Unit,
    onToggle: (SavedAlarm) -> Unit,
    onDelete: (SavedAlarm) -> Unit,
) {
    val style = LocalGridStyle.current
    Stack(Direction.Vertical, gap = false) {
        if (!exactAlarms) {
            Node(Slot.units(1)) {
                Line("Approximate alarms", color = style.colors.yellow)
            }
        }
        Node(Slot.grow()) {
            Page {
                if (alarms.isEmpty()) {
                    Line("No alarms")
                }
                alarms.forEachIndexed { index, alarm ->
                    Block {
                        AlarmRow(alarm, onToggle, onDelete)
                    }
                    if (index < alarms.lastIndex) {
                        Rule()
                    }
                }
            }
        }
        Node(Slot.units(1)) {
            Stack(Direction.Horizontal) {
                Node(Slot.square) {
                    Action("-", "Hour down") { onDraftHour(draftHour - 1) }
                }
                Node(Slot.units(2)) {
                    Cell(align = Alignment.Center, description = "Add time") {
                        Text(ClockMath.formatHm(draftHour, draftMinute), description = "Add time")
                    }
                }
                Node(Slot.square) {
                    Action("+", "Hour up") { onDraftHour(draftHour + 1) }
                }
                Node(Slot.square) {
                    Action("-", "Minute down") { onDraftMinute(draftMinute - 1) }
                }
                Node(Slot.square) {
                    Action("+", "Minute up") { onDraftMinute(draftMinute + 1) }
                }
                Node(Slot.grow()) {
                    Action("Add", "Add", onAdd)
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: SavedAlarm,
    onToggle: (SavedAlarm) -> Unit,
    onDelete: (SavedAlarm) -> Unit,
) {
    val style = LocalGridStyle.current
    val time = ClockMath.formatHm(alarm.hour, alarm.minute)
    Stack(Direction.Horizontal, Modifier.height(style.cellDp())) {
        Node(Slot.grow()) {
            Cell(description = time) {
                Text(time, description = time)
            }
        }
        Node(Slot.square) {
            Cell(
                align = Alignment.Center,
                onClick = { onToggle(alarm) },
                description = if (alarm.enabled) "Disable" else "Enable",
            ) {
                Glyph(
                    if (alarm.enabled) IconGlyphs.TOGGLE_ON else IconGlyphs.TOGGLE_OFF,
                    color = if (alarm.enabled) style.colors.accent else style.colors.muted,
                    description = if (alarm.enabled) "Disable" else "Enable",
                )
            }
        }
        Node(Slot.square) {
            Action("x", "Delete") { onDelete(alarm) }
        }
    }
}

@Composable
private fun TimerPane(
    timer: TimerState,
    nowElapsed: Long,
    onTimer: (TimerState) -> Unit,
) {
    val remaining = timer.remaining(nowElapsed)
    val minutes = ((timer.durationMillis / 1000L) / 60L).toInt().coerceAtLeast(0)
    val seconds = ((timer.durationMillis / 1000L) % 60L).toInt()
    Stack(Direction.Vertical) {
        Node(Slot.grow()) {
            Cell(align = Alignment.Center, description = "Timer") {
                Text(ClockMath.formatDuration(remaining), description = "Timer")
            }
        }
        if (!timer.running && remaining == timer.durationMillis) {
            Node(Slot.units(1)) {
                Stack(Direction.Horizontal) {
                    Node(Slot.square) {
                        Action("-", "Minute down") {
                            onTimer(timer.withDuration(((minutes - 1).coerceAtLeast(0) * 60L + seconds) * 1000L))
                        }
                    }
                    Node(Slot.units(2)) {
                        Cell(align = Alignment.Center, description = "Minutes") {
                            Text("%02d m".format(minutes), description = "Minutes")
                        }
                    }
                    Node(Slot.square) {
                        Action("+", "Minute up") {
                            onTimer(timer.withDuration(((minutes + 1).coerceAtMost(99) * 60L + seconds) * 1000L))
                        }
                    }
                    Node(Slot.square) {
                        Action("-", "Second down") {
                            onTimer(
                                timer.withDuration(
                                    (minutes * 60L + ClockMath.wrapSecond(seconds - 1)) * 1000L,
                                ),
                            )
                        }
                    }
                    Node(Slot.units(2)) {
                        Cell(align = Alignment.Center, description = "Seconds") {
                            Text("%02d s".format(seconds), description = "Seconds")
                        }
                    }
                    Node(Slot.square) {
                        Action("+", "Second up") {
                            onTimer(
                                timer.withDuration(
                                    (minutes * 60L + ClockMath.wrapSecond(seconds + 1)) * 1000L,
                                ),
                            )
                        }
                    }
                }
            }
        }
        Node(Slot.units(1)) {
            Stack(Direction.Horizontal) {
                Node(Slot.grow()) {
                    val label = if (timer.running) "Pause" else "Start"
                    Action(label, label) {
                        onTimer(if (timer.running) timer.pause(nowElapsed) else timer.start(nowElapsed))
                    }
                }
                Node(Slot.grow()) {
                    Action("Reset", "Reset") { onTimer(timer.reset()) }
                }
            }
        }
    }
}

@Composable
private fun StopwatchPane(
    stopwatch: StopwatchState,
    nowElapsed: Long,
    onStopwatch: (StopwatchState) -> Unit,
) {
    val elapsed = stopwatch.elapsed(nowElapsed)
    Stack(Direction.Vertical, gap = false) {
        Node(Slot.units(2)) {
            Cell(align = Alignment.Center, description = "Stopwatch") {
                Text(ClockMath.formatStopwatch(elapsed), description = "Stopwatch")
            }
        }
        Node(Slot.units(1)) {
            Stack(Direction.Horizontal) {
                Node(Slot.grow()) {
                    val label = if (stopwatch.running) "Pause" else "Start"
                    Action(label, label) {
                        onStopwatch(
                            if (stopwatch.running) {
                                stopwatch.pause(nowElapsed)
                            } else {
                                stopwatch.start(nowElapsed)
                            },
                        )
                    }
                }
                Node(Slot.grow()) {
                    Action("Lap", "Lap") { onStopwatch(stopwatch.lap(nowElapsed)) }
                }
                Node(Slot.grow()) {
                    Action("Reset", "Reset") { onStopwatch(stopwatch.reset()) }
                }
            }
        }
        Node(Slot.grow()) {
            Page {
                stopwatch.laps.asReversed().forEachIndexed { index, mark ->
                    val n = stopwatch.laps.size - index
                    Line(
                        "$n  ${ClockMath.formatStopwatch(mark)}",
                        description = "Lap $n",
                    )
                }
            }
        }
    }
}

@Composable
private fun Action(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    Cell(align = Alignment.Center, onClick = onClick, description = description) {
        Text(label, description = description)
    }
}
