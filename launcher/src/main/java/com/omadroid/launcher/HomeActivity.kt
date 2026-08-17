package com.omadroid.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.omadroid.launcher.widget.GridButton
import com.omadroid.launcher.widget.GridChrome
import com.omadroid.launcher.widget.GridField
import com.omadroid.launcher.widget.GridIcon
import com.omadroid.launcher.widget.GridIconButton
import com.omadroid.launcher.widget.GridRow
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.launcher.widget.GridText
import com.omadroid.launcher.widget.gridCellParams
import com.omadroid.launcher.widget.gridIconParams
import com.omadroid.launcher.widget.gridStretchParams
import com.omadroid.launcher.widget.tint
import com.omadroid.theme.ThemeCatalog
import com.omadroid.theme.ThemeColors
import java.time.LocalDate
import java.util.Locale

class HomeActivity : Activity() {
    private lateinit var launcherApps: LauncherApps
    private lateinit var theme: ThemeColors
    private lateinit var style: GridStyle
    private lateinit var grid: GridMetrics
    private lateinit var slots: Map<BuiltinModule, AllocatedSpace>
    private lateinit var layoutButton: GridIconButton
    private lateinit var dateView: GridText
    private lateinit var wifiIcon: GridIcon
    private lateinit var batteryIcon: GridIcon
    private lateinit var batteryView: GridText
    private var layout: LauncherLayout = LauncherLayout.Desktop
    private var workspaces: Workspaces = defaultWorkspaces()
    private lateinit var workspaceSwitcher: LinearLayout
    private lateinit var workspaceCanvas: FrameLayout
    private val workspaceButtons = mutableMapOf<String, GridButton>()
    private val user: UserHandle = Process.myUserHandle()
    private val barReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                bindBar()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcherApps = getSystemService(LauncherApps::class.java)
        theme = ThemeCatalog.load(assets)
        if (savedInstanceState != null) {
            layout = LauncherLayout.valueOf(
                savedInstanceState.getString(STATE_LAYOUT, LauncherLayout.Desktop.name),
            )
            workspaces = workspaces.select(
                savedInstanceState.getString(STATE_WORKSPACE, workspaces.activeId) ?: workspaces.activeId,
            )
        }
        window.decorView.setBackgroundColor(theme.background)
        val unitPx = unitLengthPx(resources.displayMetrics.density)
        style = GridStyle(unitPx, theme)
        grid =
            measureGrid(
                resources.displayMetrics.widthPixels,
                resources.displayMetrics.heightPixels,
                unitPx,
            )
        slots = allocateSpace(grid, defaultSpaceClaims())
        setContentView(buildChrome())
        bindLayoutButton()
        bindBar()
        bindWorkspaces()
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                OnBackInvokedCallback { },
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
        bindBar()
    }

    override fun onStop() {
        unregisterReceiver(barReceiver)
        super.onStop()
    }

    private fun buildChrome(): View {
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(theme.background)
            }
        root.addView(
            buildBar(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                slots.getValue(BuiltinModule.Bar).pixels.height,
            ),
        )
        root.addView(
            buildWorkspaces(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        root.addView(
            buildLauncherBar(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                slots.getValue(BuiltinModule.LauncherBar).pixels.height,
            ),
        )
        return root
    }

    private fun buildBar(): View {
        val bar =
            GridChrome(this, style).apply {
                contentDescription = getString(R.string.bar_name)
            }
        val arranged = arrangeBar(composeBarPlacements(builtinModules(), defaultBarPlacements))
        bar.addView(buildAnchor(BarAnchor.Left, arranged), matchBar())
        bar.addView(buildAnchor(BarAnchor.Center, arranged), matchBar())
        bar.addView(buildAnchor(BarAnchor.Right, arranged), matchBar())
        return bar
    }

    private fun matchBar(): FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

    private fun buildAnchor(
        anchor: BarAnchor,
        arranged: Map<BarAnchor, List<BarModule>>,
    ): View {
        val gravity =
            when (anchor) {
                BarAnchor.Left -> Gravity.START or Gravity.CENTER_VERTICAL
                BarAnchor.Center -> Gravity.CENTER
                BarAnchor.Right -> Gravity.END or Gravity.CENTER_VERTICAL
            }
        val row =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                this.gravity = gravity
                contentDescription =
                    when (anchor) {
                        BarAnchor.Left -> getString(R.string.bar_anchor_left)
                        BarAnchor.Center -> getString(R.string.bar_anchor_center)
                        BarAnchor.Right -> getString(R.string.bar_anchor_right)
                    }
            }
        arranged.getValue(anchor).forEach { module ->
            row.addView(buildBarModule(module))
        }
        return row
    }

    private fun buildBarModule(module: BarModule): View {
        return when (module) {
            BarModule.Date -> {
                dateView = GridText(this, style)
                dateView
            }
            BarModule.Wifi -> {
                wifiIcon =
                    GridIcon(this, style).apply {
                        setImageResource(R.drawable.ic_wifi)
                    }
                LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(wifiIcon, gridIconParams(style, marginEnd = style.gapPx))
                }
            }
            BarModule.Battery -> {
                batteryIcon =
                    GridIcon(this, style).apply {
                        setImageResource(R.drawable.ic_battery)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                batteryView = GridText(this, style)
                LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(batteryIcon, gridIconParams(style, marginEnd = style.tightGapPx))
                    addView(
                        batteryView,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                }
            }
            BarModule.WorkspaceSwitcher -> {
                workspaceSwitcher =
                    LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        contentDescription = getString(R.string.workspaces_name)
                    }
                workspaceSwitcher
            }
        }
    }

    private fun bindBar() {
        val status = currentBarStatus()
        if (::dateView.isInitialized) {
            dateView.text = status.dateLabel
            dateView.contentDescription = status.dateLabel
        }
        if (::wifiIcon.isInitialized) {
            wifiIcon.contentDescription = wifiContentDescription(status.wifi)
            wifiIcon.tint(
                when (status.wifi) {
                    WifiState.Connected -> theme.green
                    WifiState.Disconnected -> theme.muted
                    WifiState.Off -> theme.red
                },
            )
            wifiIcon.alpha = if (status.wifi == WifiState.Off) 0.45f else 1f
        }
        if (::batteryView.isInitialized) {
            batteryView.text = status.batteryLabel
            batteryView.contentDescription =
                batteryContentDescription(status.batteryPercent, status.charging, Locale.getDefault())
            val batteryColor =
                when {
                    status.charging -> theme.green
                    status.batteryPercent <= 20 -> theme.red
                    else -> theme.foreground
                }
            batteryView.tint(batteryColor)
            if (::batteryIcon.isInitialized) {
                batteryIcon.tint(batteryColor)
            }
        }
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

    private fun buildLauncherBar(): View {
        val bar =
            GridRow(this, style).apply {
                contentDescription = getString(R.string.app_name)
            }
        val field =
            GridField(this, style).apply {
                hint = getString(R.string.launcher_search)
            }
        bar.addView(field, gridStretchParams(style, marginEnd = style.gapPx))
        layoutButton =
            GridIconButton(this, style).apply {
                setImageResource(R.drawable.ic_layout)
                contentDescription = getString(R.string.launcher_layout)
                setOnClickListener {
                    layout = layout.next()
                    bindLayoutButton()
                    bindWorkspaces()
                }
            }
        bar.addView(layoutButton, gridCellParams(style, marginStart = style.tightGapPx))
        val menuButton =
            GridIconButton(this, style).apply {
                setImageResource(R.drawable.ic_menu)
                contentDescription = getString(R.string.launcher_menu)
                setOnClickListener { button -> showAppMenu(button) }
            }
        bar.addView(menuButton, gridCellParams(style, marginStart = style.tightGapPx))
        return bar
    }

    private fun bindLayoutButton() {
        val description =
            when (layout) {
                LauncherLayout.Desktop -> getString(R.string.launcher_layout_desktop)
                LauncherLayout.Focus -> getString(R.string.launcher_layout_focus)
            }
        layoutButton.contentDescription = description
        layoutButton.tint(if (layout == LauncherLayout.Focus) theme.accent else theme.foreground)
    }

    private fun showAppMenu(anchor: View) {
        val infos = launcherApps.getActivityList(null, user)
        val apps =
            visibleLaunchableApps(
                infos.map { info ->
                    LaunchableApp(
                        packageName = info.componentName.packageName,
                        activityName = info.componentName.className,
                        label = info.label.toString(),
                    )
                },
                packageName,
            )
        val menu = PopupMenu(this, anchor)
        apps.forEachIndexed { index, app ->
            menu.menu.add(0, index, index, app.label)
        }
        menu.setOnMenuItemClickListener { item ->
            val app = apps[item.itemId]
            launcherApps.startMainActivity(
                ComponentName(app.packageName, app.activityName),
                user,
                null,
                null,
            )
            true
        }
        menu.show()
    }

    private fun buildWorkspaces(): View {
        workspaceCanvas =
            FrameLayout(this).apply {
                setBackgroundColor(theme.background)
                contentDescription = getString(R.string.workspaces_name)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
        return workspaceCanvas
    }

    private fun bindWorkspaces() {
        if (!::workspaceSwitcher.isInitialized) {
            workspaceCanvas.contentDescription =
                getString(R.string.workspace_label, workspaces.active.name)
            return
        }
        workspaceSwitcher.removeAllViews()
        workspaceButtons.clear()
        visibleWorkspaces(workspaces, layout).forEach { workspace ->
            val button =
                GridButton(this, style).apply {
                    text = workspace.name
                    contentDescription = getString(R.string.workspace_label, workspace.name)
                    setOnClickListener {
                        workspaces = workspaces.select(workspace.id)
                        bindWorkspaces()
                    }
                }
            workspaceButtons[workspace.id] = button
            workspaceSwitcher.addView(button)
        }
        workspaceSwitcher.visibility =
            if (layout == LauncherLayout.Focus) View.GONE else View.VISIBLE
        val active = workspaces.active
        workspaceCanvas.contentDescription = getString(R.string.workspace_label, active.name)
        workspaceButtons.forEach { (id, button) ->
            val selected = id == workspaces.activeId
            button.tint(if (selected) theme.accent else theme.muted)
        }
    }

    companion object {
        private const val STATE_LAYOUT = "launcher_layout"
        private const val STATE_WORKSPACE = "workspace"
    }
}
