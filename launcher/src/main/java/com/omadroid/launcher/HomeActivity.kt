package com.omadroid.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalView
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.theme.ThemeCatalog
import com.omadroid.theme.ThemeColors
import java.time.LocalDate
import java.util.Locale

class HomeActivity : ComponentActivity() {
    private var theme by mutableStateOf(placeholderTheme())
    private var style by mutableStateOf(GridStyle(1, placeholderTheme()))
    private var unitPx: Int = 0
    private var themeListY by mutableIntStateOf(0)
    private var layout by mutableStateOf(LauncherLayout.Desktop)
    private var workspaces by mutableStateOf(defaultWorkspaces())
    private var sheet by mutableStateOf(NavStack())
    private var commandOpen by mutableStateOf(false)
    private var commandQuery by mutableStateOf("")
    private var wipe by mutableStateOf<ImageBitmap?>(null)
    private var bar by mutableStateOf(
        BarStatus(
            dateLabel = formatBarDate(LocalDate.now(), Locale.getDefault()),
            wifi = WifiState.Disconnected,
            batteryPercent = 0,
            charging = false,
        ),
    )
    private val barReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                bar = currentBarStatus()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        actionBar?.hide()
        window.decorView.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        theme = loadSavedTheme()
        if (savedInstanceState != null) {
            layout =
                LauncherLayout.valueOf(
                    savedInstanceState.getString(STATE_LAYOUT, LauncherLayout.Desktop.name),
                )
            workspaces =
                workspaces.select(
                    savedInstanceState.getString(STATE_WORKSPACE, workspaces.activeId)
                        ?: workspaces.activeId,
                )
        }
        window.decorView.setBackgroundColor(theme.background)
        hideSystemBars()
        unitPx = unitLengthPx(resources.displayMetrics.density)
        style = GridStyle(unitPx, theme)
        setContent {
            val view = LocalView.current
            Home(
                state =
                    HomeState(
                        style = style,
                        layout = layout,
                        workspaces = workspaces,
                        bar = bar,
                        sheet = sheet,
                        commandOpen = commandOpen,
                        commandQuery = commandQuery,
                        themeListY = themeListY,
                        wipe = wipe,
                    ),
                onLayout = {
                    layout = layout.next()
                },
                onSelectWorkspace = { id ->
                    workspaces = workspaces.select(id)
                },
                onCommand = { text ->
                    commandQuery = text
                    commandOpen = true
                },
                onCommandOpen = { commandOpen = true },
                onCommandDismiss = { dismissCommand() },
                onOpenMenu = {
                    dismissCommand()
                    openSheet(menuRoute())
                },
                onSheetBack = { onSheetBack() },
                onSheetDismiss = { sheet = NavStack() },
                onMenuClick = { item ->
                    dismissCommand()
                    when {
                        item.id == ROUTE_THEME -> openSheet(themeRoute())
                        startLaunchable(this, item.id) -> sheet = NavStack()
                    }
                },
                onCommandClick = { item ->
                    dismissCommand()
                    when {
                        item.id == ROUTE_THEME -> openSheet(themeRoute())
                        item.id == ITEM_FOCUS ->
                            layout =
                                if (layout == LauncherLayout.Focus) {
                                    LauncherLayout.Desktop
                                } else {
                                    LauncherLayout.Focus
                                }
                        item.id.startsWith("workspace/") ->
                            workspaces = workspaces.select(item.id.removePrefix("workspace/"))
                        startLaunchable(this, item.id) -> sheet = NavStack()
                    }
                },
                onMenuToggle = { item, on ->
                    if (item.id == ITEM_FOCUS) {
                        layout = if (on) LauncherLayout.Focus else LauncherLayout.Desktop
                    }
                    dismissCommand()
                },
                onThemeClick = { slug -> activateTheme(slug, view) },
                onThemeScroll = { themeListY = it },
                onWipeDone = { wipe = null },
            )
        }
        bar = currentBarStatus()
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { onSheetBack() },
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_LAYOUT, layout.name)
        outState.putString(STATE_WORKSPACE, workspaces.activeId)
    }

    override fun onStart() {
        super.onStart()
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(barReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(barReceiver, filter)
        }
        bar = currentBarStatus()
        hideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onStop() {
        unregisterReceiver(barReceiver)
        super.onStop()
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (onSheetBack()) {
            return
        }
        super.onBackPressed()
    }

    private fun onSheetBack(): Boolean {
        if (commandOpen) {
            dismissCommand()
            return true
        }
        if (!sheet.isOpen) {
            return false
        }
        sheet = if (sheet.canPop) sheet.pop() else NavStack()
        return true
    }

    private fun dismissCommand() {
        commandOpen = false
        commandQuery = ""
    }

    private fun menuRoute(): NavRoute =
        NavRoute(ROUTE_MENU, getString(R.string.sheet_menu))

    private fun themeRoute(): NavRoute =
        NavRoute(ROUTE_THEME, getString(R.string.sheet_theme))

    private fun openSheet(route: NavRoute) {
        sheet = if (sheet.isOpen) sheet.push(route) else NavStack.root(route)
    }

    private fun currentBarStatus(): BarStatus {
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging =
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        return BarStatus(
            dateLabel = formatBarDate(LocalDate.now(), Locale.getDefault()),
            wifi = currentWifiState(),
            batteryPercent = readBatteryPercent(level, scale),
            charging = charging,
        )
    }

    private fun currentWifiState(): WifiState {
        return try {
            val connectivity =
                getSystemService(ConnectivityManager::class.java) ?: return WifiState.Disconnected
            val network = connectivity.activeNetwork ?: return WifiState.Disconnected
            val caps = connectivity.getNetworkCapabilities(network) ?: return WifiState.Disconnected
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                WifiState.Connected
            } else {
                WifiState.Disconnected
            }
        } catch (_: SecurityException) {
            WifiState.Disconnected
        }
    }

    private fun prefs() =
        createDeviceProtectedStorageContext().getSharedPreferences(PREFS, MODE_PRIVATE)

    private fun loadSavedTheme(): ThemeColors {
        val slug = prefs().getString(PREF_THEME, ThemeColors.DEFAULT_SLUG) ?: ThemeColors.DEFAULT_SLUG
        return try {
            ThemeCatalog.load(assets, slug)
        } catch (_: com.omadroid.theme.ThemeColorsException) {
            ThemeCatalog.load(assets, ThemeColors.DEFAULT_SLUG)
        }
    }

    private fun activateTheme(slug: String, view: android.view.View) {
        if (slug == theme.slug || wipe != null) {
            return
        }
        val snapshot = snapshotView(view)
        prefs().edit().putString(PREF_THEME, slug).apply()
        theme = ThemeCatalog.load(assets, slug)
        style = GridStyle(unitPx, theme)
        window.decorView.setBackgroundColor(theme.background)
        wipe = snapshot
    }

    companion object {
        private const val STATE_LAYOUT = "launcher_layout"
        private const val STATE_WORKSPACE = "workspace"
        private const val PREFS = "omadroid"
        private const val PREF_THEME = "theme_slug"
    }
}

private fun placeholderTheme(): ThemeColors =
    ThemeColors(
        slug = ThemeColors.DEFAULT_SLUG,
        mode = "dark",
        accent = 0,
        selection = 0,
        muted = 0,
        background = 0,
        darkBackground = 0,
        darkerBackground = 0,
        lighterBackground = 0,
        foreground = 0,
        darkForeground = 0,
        lightForeground = 0,
        brightForeground = 0,
        red = 0,
        yellow = 0,
        orange = 0,
        green = 0,
        cyan = 0,
        blue = 0,
        magenta = 0,
        brown = 0,
    )
