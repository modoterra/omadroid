package com.omadroid.clock

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.omadroid.compose.LocalGridStyle
import kotlinx.coroutines.delay

@Composable
fun ClockHost() {
    val context = LocalContext.current
    val style = LocalGridStyle.current
    val store = remember { AlarmStore.deviceProtected(context) }
    val scheduler = remember { AlarmScheduler(context) }
    var alarms by remember { mutableStateOf(store.load()) }
    var tab by remember { mutableStateOf(ClockTab.Alarms) }
    var draftHour by remember { mutableIntStateOf(7) }
    var draftMinute by remember { mutableIntStateOf(0) }
    var timer by remember { mutableStateOf(TimerState()) }
    var stopwatch by remember { mutableStateOf(StopwatchState()) }
    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val notify =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        alarms = store.load()
    }
    LaunchedEffect(timer.running, stopwatch.running) {
        while (timer.running || stopwatch.running) {
            nowElapsed = SystemClock.elapsedRealtime()
            if (timer.running && timer.remaining(nowElapsed) <= 0L) {
                timer = timer.pause(nowElapsed)
            }
            delay(50)
        }
        nowElapsed = SystemClock.elapsedRealtime()
    }
    ClockScreen(
        style = style,
        tab = tab,
        alarms = alarms,
        draftHour = draftHour,
        draftMinute = draftMinute,
        exactAlarms = scheduler.canScheduleExact(),
        timer = timer,
        stopwatch = stopwatch,
        nowElapsed = nowElapsed,
        onTab = { tab = it },
        onDraftHour = { draftHour = ClockMath.wrapHour(it) },
        onDraftMinute = { draftMinute = ClockMath.wrapMinute(it) },
        onAddAlarm = {
            val next =
                SavedAlarm(
                    id = AlarmCodec.nextId(alarms),
                    hour = draftHour,
                    minute = draftMinute,
                    enabled = true,
                )
            val updated = alarms + next
            alarms = updated
            store.save(updated)
            scheduler.schedule(next)
        },
        onToggleAlarm = { alarm ->
            val next = alarm.copy(enabled = !alarm.enabled)
            val updated = alarms.map { if (it.id == alarm.id) next else it }
            alarms = updated
            store.save(updated)
            if (next.enabled) {
                scheduler.schedule(next)
            } else {
                scheduler.cancel(next.id)
            }
        },
        onDeleteAlarm = { alarm ->
            val updated = alarms.filterNot { it.id == alarm.id }
            alarms = updated
            store.save(updated)
            scheduler.cancel(alarm.id)
        },
        onTimer = { timer = it },
        onStopwatch = { stopwatch = it },
    )
}
