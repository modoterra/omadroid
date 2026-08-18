package com.omadroid.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.omadroid.compose.Compose
import com.omadroid.compose.Direction
import com.omadroid.compose.Glyph
import com.omadroid.compose.NoFling
import com.omadroid.compose.NoOverscroll
import com.omadroid.compose.Slot
import com.omadroid.compose.Stack
import com.omadroid.compose.Text
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
            Node(Slot.units(1)) {
                Box(
                    Modifier.fillMaxSize().background(Color(style.colors.lighterBackground)),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text("Clock")
                }
            }
            Node(Slot.units(1)) {
                Stack(Direction.Horizontal) {
                    ClockTab.entries.forEach { item ->
                        Node(Slot.grow()) {
                            TabCell(
                                label = item.label,
                                selected = item == tab,
                                accent = style.colors.accent,
                                muted = style.colors.muted,
                                onClick = { onTab(item) },
                            )
                        }
                    }
                }
            }
            Node(Slot.grow()) {
                when (tab) {
                    ClockTab.Alarms ->
                        AlarmsPane(
                            style = style,
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
                        TimerPane(style, timer, nowElapsed, onTimer)
                    ClockTab.Stopwatch ->
                        StopwatchPane(style, stopwatch, nowElapsed, onStopwatch)
                }
            }
        }
    }
}

@Composable
private fun TabCell(
    label: String,
    selected: Boolean,
    accent: Int,
    muted: Int,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) accent else muted, description = label)
    }
}

@Composable
private fun AlarmsPane(
    style: GridStyle,
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
    val density = LocalDensity.current
    val row = with(density) { style.cellPx.toDp() }
    Stack(Direction.Vertical, gap = false) {
        if (!exactAlarms) {
            Node(Slot.units(1)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Text("Approximate alarms", color = style.colors.yellow)
                }
            }
        }
        Node(Slot.grow()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState(),
                        overscrollEffect = NoOverscroll,
                        flingBehavior = NoFling,
                    ),
            ) {
                if (alarms.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().height(row),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("No alarms", color = style.colors.muted)
                    }
                }
                alarms.forEach { alarm ->
                    AlarmRow(style, alarm, row, onToggle, onDelete)
                }
            }
        }
        Node(Slot.units(1)) {
            Stack(Direction.Horizontal) {
                Node(Slot.square) {
                    ActionCell("-", "Hour down") { onDraftHour(draftHour - 1) }
                }
                Node(Slot.units(2)) {
                    CenterLabel(ClockMath.formatHm(draftHour, draftMinute), "Add time")
                }
                Node(Slot.square) {
                    ActionCell("+", "Hour up") { onDraftHour(draftHour + 1) }
                }
                Node(Slot.square) {
                    ActionCell("-", "Minute down") { onDraftMinute(draftMinute - 1) }
                }
                Node(Slot.square) {
                    ActionCell("+", "Minute up") { onDraftMinute(draftMinute + 1) }
                }
                Node(Slot.grow()) {
                    ActionCell("Add", "Add") { onAdd() }
                }
            }
        }
    }
}

@Composable
private fun AlarmRow(
    style: GridStyle,
    alarm: SavedAlarm,
    row: androidx.compose.ui.unit.Dp,
    onToggle: (SavedAlarm) -> Unit,
    onDelete: (SavedAlarm) -> Unit,
) {
    Stack(Direction.Horizontal, Modifier.height(row)) {
        Node(Slot.grow()) {
            CenterLabel(
                ClockMath.formatHm(alarm.hour, alarm.minute),
                ClockMath.formatHm(alarm.hour, alarm.minute),
                alignStart = true,
            )
        }
        Node(Slot.square) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onToggle(alarm) },
                    )
                    .semantics {
                        contentDescription = if (alarm.enabled) "Disable" else "Enable"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Glyph(
                    if (alarm.enabled) IconGlyphs.TOGGLE_ON else IconGlyphs.TOGGLE_OFF,
                    color = if (alarm.enabled) style.colors.accent else style.colors.muted,
                    description = if (alarm.enabled) "Disable" else "Enable",
                )
            }
        }
        Node(Slot.square) {
            ActionCell("x", "Delete") { onDelete(alarm) }
        }
    }
}

@Composable
private fun TimerPane(
    style: GridStyle,
    timer: TimerState,
    nowElapsed: Long,
    onTimer: (TimerState) -> Unit,
) {
    val remaining = timer.remaining(nowElapsed)
    val minutes = ((timer.durationMillis / 1000L) / 60L).toInt().coerceAtLeast(0)
    val seconds = ((timer.durationMillis / 1000L) % 60L).toInt()
    Stack(Direction.Vertical) {
        Node(Slot.grow()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(ClockMath.formatDuration(remaining), description = "Timer")
            }
        }
        if (!timer.running && remaining == timer.durationMillis) {
            Node(Slot.units(1)) {
                Stack(Direction.Horizontal) {
                    Node(Slot.square) {
                        ActionCell("-", "Minute down") {
                            onTimer(timer.withDuration(((minutes - 1).coerceAtLeast(0) * 60L + seconds) * 1000L))
                        }
                    }
                    Node(Slot.units(2)) {
                        CenterLabel("%02d m".format(minutes), "Minutes")
                    }
                    Node(Slot.square) {
                        ActionCell("+", "Minute up") {
                            onTimer(timer.withDuration(((minutes + 1).coerceAtMost(99) * 60L + seconds) * 1000L))
                        }
                    }
                    Node(Slot.square) {
                        ActionCell("-", "Second down") {
                            onTimer(
                                timer.withDuration(
                                    (minutes * 60L + ClockMath.wrapSecond(seconds - 1)) * 1000L,
                                ),
                            )
                        }
                    }
                    Node(Slot.units(2)) {
                        CenterLabel("%02d s".format(seconds), "Seconds")
                    }
                    Node(Slot.square) {
                        ActionCell("+", "Second up") {
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
                    ActionCell(label, label) {
                        onTimer(if (timer.running) timer.pause(nowElapsed) else timer.start(nowElapsed))
                    }
                }
                Node(Slot.grow()) {
                    ActionCell("Reset", "Reset") { onTimer(timer.reset()) }
                }
            }
        }
    }
}

@Composable
private fun StopwatchPane(
    style: GridStyle,
    stopwatch: StopwatchState,
    nowElapsed: Long,
    onStopwatch: (StopwatchState) -> Unit,
) {
    val density = LocalDensity.current
    val row = with(density) { style.cellPx.toDp() }
    val elapsed = stopwatch.elapsed(nowElapsed)
    Stack(Direction.Vertical, gap = false) {
        Node(Slot.units(2)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(ClockMath.formatStopwatch(elapsed), description = "Stopwatch")
            }
        }
        Node(Slot.units(1)) {
            Stack(Direction.Horizontal) {
                Node(Slot.grow()) {
                    val label = if (stopwatch.running) "Pause" else "Start"
                    ActionCell(label, label) {
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
                    ActionCell("Lap", "Lap") { onStopwatch(stopwatch.lap(nowElapsed)) }
                }
                Node(Slot.grow()) {
                    ActionCell("Reset", "Reset") { onStopwatch(stopwatch.reset()) }
                }
            }
        }
        Node(Slot.grow()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState(),
                        overscrollEffect = NoOverscroll,
                        flingBehavior = NoFling,
                    ),
            ) {
                stopwatch.laps.asReversed().forEachIndexed { index, mark ->
                    val n = stopwatch.laps.size - index
                    Box(
                        Modifier.fillMaxWidth().height(row),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text(
                            "$n  ${ClockMath.formatStopwatch(mark)}",
                            color = style.colors.muted,
                            description = "Lap $n",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCell(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, description = description)
    }
}

@Composable
private fun CenterLabel(
    text: String,
    description: String,
    alignStart: Boolean = false,
) {
    Box(
        Modifier.fillMaxSize().semantics { contentDescription = description },
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.Center,
    ) {
        Text(text, description = description)
    }
}
