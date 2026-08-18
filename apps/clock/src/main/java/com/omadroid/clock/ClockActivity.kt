package com.omadroid.clock

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.omadroid.launcher.unitLengthPx
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.theme.OmadroidTheme
import com.omadroid.theme.ThemeColors
import kotlinx.coroutines.delay

class ClockActivity : ComponentActivity() {
    private lateinit var store: AlarmStore
    private lateinit var scheduler: AlarmScheduler
    private var theme by mutableStateOf(placeholderTheme())
    private var style by mutableStateOf(GridStyle(1, placeholderTheme()))
    private var alarms by mutableStateOf(emptyList<SavedAlarm>())
    private var tab by mutableStateOf(ClockTab.Alarms)
    private var draftHour by mutableIntStateOf(7)
    private var draftMinute by mutableIntStateOf(0)
    private var timer by mutableStateOf(TimerState())
    private var stopwatch by mutableStateOf(StopwatchState())
    private var nowElapsed by mutableLongStateOf(0L)
    private var themeWatch: ContentObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.decorView.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        store = AlarmStore.deviceProtected(this)
        scheduler = AlarmScheduler(this)
        applySelectedTheme()
        hideSystemBars()
        alarms = store.load()
        nowElapsed = SystemClock.elapsedRealtime()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent {
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
                onAddAlarm = { addAlarm() },
                onToggleAlarm = { toggleAlarm(it) },
                onDeleteAlarm = { deleteAlarm(it) },
                onTimer = { timer = it },
                onStopwatch = { stopwatch = it },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        alarms = store.load()
        if (themeWatch == null) {
            themeWatch = OmadroidTheme.observe(this) { applySelectedTheme() }
        }
        applySelectedTheme()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun addAlarm() {
        val next =
            SavedAlarm(
                id = AlarmCodec.nextId(alarms),
                hour = draftHour,
                minute = draftMinute,
                enabled = true,
            )
        val updated = alarms + next
        persist(updated)
        scheduler.schedule(next)
    }

    private fun toggleAlarm(alarm: SavedAlarm) {
        val next = alarm.copy(enabled = !alarm.enabled)
        val updated = alarms.map { if (it.id == alarm.id) next else it }
        persist(updated)
        if (next.enabled) {
            scheduler.schedule(next)
        } else {
            scheduler.cancel(next.id)
        }
    }

    private fun deleteAlarm(alarm: SavedAlarm) {
        persist(alarms.filterNot { it.id == alarm.id })
        scheduler.cancel(alarm.id)
    }

    private fun persist(next: List<SavedAlarm>) {
        alarms = next
        store.save(next)
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT < 30) {
            return
        }
        window.setDecorFitsSystemWindows(false)
        window.attributes.layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        window.insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
    }

    override fun onStop() {
        themeWatch?.let { contentResolver.unregisterContentObserver(it) }
        themeWatch = null
        super.onStop()
    }

    private fun applySelectedTheme() {
        theme =
            try {
                OmadroidTheme.load(this)
            } catch (_: com.omadroid.theme.ThemeColorsException) {
                placeholderTheme()
            }
        style = GridStyle(unitLengthPx(resources.displayMetrics.density), theme)
        window.decorView.setBackgroundColor(theme.background)
    }
}

private fun placeholderTheme(): ThemeColors =
    ThemeColors(
        slug = ThemeColors.DEFAULT_SLUG,
        mode = "dark",
        accent = 0xFF7AA2F7.toInt(),
        selection = 0xFF292E42.toInt(),
        muted = 0xFF414868.toInt(),
        background = 0xFF1A1B26.toInt(),
        darkBackground = 0xFF13141C.toInt(),
        darkerBackground = 0xFF0E0E14.toInt(),
        lighterBackground = 0xFF24283B.toInt(),
        foreground = 0xFFA9B1D6.toInt(),
        darkForeground = 0xFF565F89.toInt(),
        lightForeground = 0xFFB4BEE6.toInt(),
        brightForeground = 0xFFC0CAF5.toInt(),
        red = 0xFFF7768E.toInt(),
        yellow = 0xFFE0AF68.toInt(),
        orange = 0xFFEB927B.toInt(),
        green = 0xFF9ECE6A.toInt(),
        cyan = 0xFF449DAB.toInt(),
        blue = 0xFF7AA2F7.toInt(),
        magenta = 0xFFAD8EE6.toInt(),
        brown = 0xFF75493D.toInt(),
    )
