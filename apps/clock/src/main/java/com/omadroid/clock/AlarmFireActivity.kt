package com.omadroid.clock

import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omadroid.compose.Compose
import com.omadroid.compose.Direction
import com.omadroid.compose.Slot
import com.omadroid.compose.Stack
import com.omadroid.compose.Text
import com.omadroid.launcher.unitLengthPx
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.theme.OmadroidTheme
import com.omadroid.theme.ThemeColors

class AlarmFireActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        hideSystemBars()
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val alarm = AlarmStore.deviceProtected(this).load().find { it.id == id }
        val label = if (alarm != null) ClockMath.formatHm(alarm.hour, alarm.minute) else "--:--"
        AlarmTone.start(applicationContext)
        val theme =
            try {
                OmadroidTheme.load(this)
            } catch (_: com.omadroid.theme.ThemeColorsException) {
                ThemeColors.parse(
                    ThemeColors.DEFAULT_SLUG,
                    """
                    background = "#1a1b26"
                    foreground = "#a9b1d6"
                    lighter_background = "#24283b"
                    muted = "#414868"
                    accent = "#7aa2f7"
                    red = "#f7768e"
                    yellow = "#e0af68"
                    green = "#9ece6a"
                    cyan = "#449dab"
                    blue = "#7aa2f7"
                    magenta = "#ad8ee6"
                    """.trimIndent(),
                )
            }
        window.decorView.setBackgroundColor(theme.background)
        val style = GridStyle(unitLengthPx(resources.displayMetrics.density), theme)
        setContent {
            Compose(style) {
                Stack(Direction.Vertical) {
                    Node(Slot.grow()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Stack(Direction.Vertical) {
                                Node(Slot.units(1)) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(getString(R.string.alarm_ringing), color = style.colors.accent)
                                    }
                                }
                                Node(Slot.units(2)) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(label)
                                    }
                                }
                            }
                        }
                    }
                    Node(Slot.units(1)) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { dismiss() },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(getString(R.string.alarm_dismiss), color = style.colors.red)
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun dismiss() {
        AlarmReceiver.dismiss(this)
        finish()
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
}
