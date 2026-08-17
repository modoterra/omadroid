package com.omadroid.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.omadroid.theme.ThemeCatalog
import com.omadroid.theme.ThemeColors
import java.time.LocalDate
import java.util.Locale

class HomeActivity : Activity() {
    private lateinit var launcherApps: LauncherApps
    private lateinit var theme: ThemeColors
    private lateinit var layoutButton: ImageButton
    private lateinit var dateView: TextView
    private lateinit var wifiIcon: ImageView
    private lateinit var batteryIcon: ImageView
    private lateinit var batteryView: TextView
    private var layout: LauncherLayout = LauncherLayout.Desktop
    private var workspaces: Workspaces = defaultWorkspaces()
    private lateinit var workspaceSwitcher: LinearLayout
    private lateinit var workspaceCanvas: FrameLayout
    private val workspaceButtons = mutableMapOf<String, TextView>()
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
                ViewGroup.LayoutParams.WRAP_CONTENT,
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
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        return root
    }

    private fun buildBar(): View {
        val bar =
            FrameLayout(this).apply {
                setBackgroundColor(theme.lighterBackground)
                val pad = dp(12)
                setPadding(pad, pad + dp(4), pad, pad)
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
            ViewGroup.LayoutParams.WRAP_CONTENT,
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
                dateView =
                    TextView(this).apply {
                        setTextColor(theme.foreground)
                        textSize = 14f
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    }
                dateView
            }
            BarModule.Wifi -> {
                wifiIcon =
                    ImageView(this).apply {
                        setImageResource(R.drawable.ic_wifi)
                        imageTintList = ColorStateList.valueOf(theme.foreground)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    }
                LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(wifiIcon, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(10) })
                }
            }
            BarModule.Battery -> {
                batteryIcon =
                    ImageView(this).apply {
                        setImageResource(R.drawable.ic_battery)
                        imageTintList = ColorStateList.valueOf(theme.foreground)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                batteryView =
                    TextView(this).apply {
                        setTextColor(theme.foreground)
                        textSize = 14f
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
                    }
                LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(batteryIcon, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(4) })
                    addView(
                        batteryView,
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
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
            wifiIcon.imageTintList =
                ColorStateList.valueOf(
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
            batteryView.setTextColor(batteryColor)
            if (::batteryIcon.isInitialized) {
                batteryIcon.imageTintList = ColorStateList.valueOf(batteryColor)
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
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(theme.lighterBackground)
                val pad = dp(12)
                setPadding(pad, pad, pad, pad + dp(8))
                contentDescription = getString(R.string.app_name)
            }
        val field = EditText(this).apply {
            hint = getString(R.string.launcher_search)
            setHintTextColor(theme.muted)
            setTextColor(theme.foreground)
            setBackground(inputBackground())
            setPadding(dp(14), dp(10), dp(14), dp(10))
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            inputType = EditorInfo.TYPE_CLASS_TEXT
            isSingleLine = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        bar.addView(
            field,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            },
        )
        layoutButton = iconButton(R.drawable.ic_layout, getString(R.string.launcher_layout)) {
            layout = layout.next()
            bindLayoutButton()
            bindWorkspaces()
        }
        bar.addView(layoutButton, buttonParams())
        val menuButton =
            iconButton(R.drawable.ic_menu, getString(R.string.launcher_menu)) { button ->
                showAppMenu(button)
            }
        bar.addView(menuButton, buttonParams())
        return bar
    }

    private fun bindLayoutButton() {
        val description =
            when (layout) {
                LauncherLayout.Desktop -> getString(R.string.launcher_layout_desktop)
                LauncherLayout.Focus -> getString(R.string.launcher_layout_focus)
            }
        layoutButton.contentDescription = description
        val tint = if (layout == LauncherLayout.Focus) theme.accent else theme.foreground
        layoutButton.imageTintList = ColorStateList.valueOf(tint)
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

    private fun iconButton(drawable: Int, description: String, onClick: (View) -> Unit): ImageButton {
        return ImageButton(this).apply {
            setImageResource(drawable)
            imageTintList = ColorStateList.valueOf(theme.foreground)
            setBackgroundColor(0)
            contentDescription = description
            minimumWidth = dp(48)
            minimumHeight = dp(48)
            setOnClickListener(onClick)
        }
    }

    private fun buttonParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginStart = dp(4) }

    private fun inputBackground(): GradientDrawable =
        GradientDrawable().apply {
            setColor(theme.darkBackground)
            cornerRadius = dp(8).toFloat()
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

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
                TextView(this).apply {
                    text = workspace.name
                    gravity = Gravity.CENTER
                    minWidth = dp(48)
                    minHeight = dp(48)
                    textSize = 16f
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
            button.setTextColor(if (selected) theme.accent else theme.muted)
        }
    }

    companion object {
        private const val STATE_LAYOUT = "launcher_layout"
        private const val STATE_WORKSPACE = "workspace"
    }
}
