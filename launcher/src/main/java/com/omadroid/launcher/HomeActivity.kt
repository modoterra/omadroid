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
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import com.omadroid.launcher.widget.GridButton
import com.omadroid.launcher.widget.GridChrome
import com.omadroid.launcher.widget.GridField
import com.omadroid.launcher.widget.GridIcon
import com.omadroid.launcher.widget.GridIconButton
import com.omadroid.launcher.widget.GridMenu
import com.omadroid.launcher.widget.GridRow
import com.omadroid.launcher.widget.MenuItem
import com.omadroid.launcher.widget.MenuSection
import com.omadroid.launcher.widget.MenuSpec
import com.omadroid.launcher.widget.GridSheet
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.launcher.widget.GridText
import com.omadroid.launcher.widget.IconGlyphs
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
    private var unitPx: Int = 0
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
    private lateinit var sheet: GridSheet
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
        theme = loadSavedTheme()
        if (savedInstanceState != null) {
            layout = LauncherLayout.valueOf(
                savedInstanceState.getString(STATE_LAYOUT, LauncherLayout.Desktop.name),
            )
            workspaces = workspaces.select(
                savedInstanceState.getString(STATE_WORKSPACE, workspaces.activeId) ?: workspaces.activeId,
            )
        }
        window.decorView.setBackgroundColor(theme.background)
        unitPx = unitLengthPx(resources.displayMetrics.density)
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
        bindBar()
    }

    override fun onStop() {
        unregisterReceiver(barReceiver)
        super.onStop()
    }

    private fun buildChrome(): View {
        val chrome =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(theme.background)
            }
        chrome.addView(
            buildBar(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                slots.getValue(BuiltinModule.Bar).pixels.height,
            ),
        )
        chrome.addView(
            buildWorkspaces(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        chrome.addView(
            buildDock(),
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                slots.getValue(BuiltinModule.Dock).pixels.height,
            ),
        )
        val root = FrameLayout(this)
        root.addView(
            chrome,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        sheet =
            GridSheet(this, style).apply {
                render = { route -> buildSheetPage(route) }
            }
        root.addView(
            sheet,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
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
                        text = IconGlyphs.WIFI
                    }
                LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(wifiIcon, gridIconParams(style))
                }
            }
            BarModule.Battery -> {
                batteryIcon =
                    GridIcon(this, style).apply {
                        text = IconGlyphs.BATTERY_FULL
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    }
                batteryView = GridText(this, style)
                LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    addView(batteryIcon, gridIconParams(style))
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
                batteryIcon.text = IconGlyphs.battery(status.batteryPercent, status.charging)
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

    private fun buildDock(): View {
        val dock =
            GridRow(this, style).apply {
                contentDescription = getString(R.string.dock_name)
            }
        val field =
            GridField(this, style).apply {
                hint = getString(R.string.launcher_search)
                isFocusable = false
                isClickable = true
                setOnClickListener { openSheet(searchRoute()) }
            }
        dock.addView(field, gridStretchParams(style))
        layoutButton =
            GridIconButton(this, style).apply {
                text = IconGlyphs.LAYOUT
                contentDescription = getString(R.string.launcher_layout)
                setOnClickListener {
                    this@HomeActivity.layout = this@HomeActivity.layout.next()
                    bindLayoutButton()
                    bindWorkspaces()
                }
            }
        dock.addView(layoutButton, gridCellParams(style))
        val menuButton =
            GridIconButton(this, style).apply {
                text = IconGlyphs.MENU
                contentDescription = getString(R.string.launcher_menu)
                setOnClickListener { openSheet(menuRoute()) }
            }
        dock.addView(menuButton, gridCellParams(style))
        return dock
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (onSheetBack()) {
            return
        }
        super.onBackPressed()
    }

    private fun onSheetBack(): Boolean {
        if (!::sheet.isInitialized || !sheet.isOpen) {
            return false
        }
        return sheet.popOrDismiss()
    }

    private fun searchRoute(): NavRoute =
        NavRoute(ROUTE_SEARCH, getString(R.string.launcher_search))

    private fun menuRoute(): NavRoute =
        NavRoute(ROUTE_MENU, getString(R.string.sheet_menu))

    private fun openSheet(route: NavRoute) {
        if (sheet.isOpen) {
            sheet.push(route)
        } else {
            sheet.show(route)
        }
    }

    private fun buildSheetPage(route: NavRoute): View {
        return when (route.id) {
            ROUTE_SEARCH -> buildSearchPage()
            ROUTE_MENU -> buildMenuPage()
            ROUTE_THEME -> buildThemePage()
            else ->
                GridText(this, style).apply {
                    text = route.title
                }
        }
    }

    private fun buildSearchPage(): View {
        val column =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(style.spacePx, style.spacePx, style.spacePx, style.spacePx)
            }
        val field =
            GridField(this, style).apply {
                hint = getString(R.string.launcher_search)
            }
        column.addView(
            field,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                style.innerPx,
            ),
        )
        val empty =
            GridText(this, style).apply {
                text = getString(R.string.sheet_search_empty)
            }
        column.addView(
            empty,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                style.innerPx,
            ).apply { topMargin = style.spacePx },
        )
        val links =
            GridMenu(this, style).apply {
                bind(
                    MenuSpec(
                        listOf(
                            MenuSection(
                                "",
                                listOf(
                                    MenuItem(
                                        id = ROUTE_MENU,
                                        title = getString(R.string.sheet_apps),
                                        icon = IconGlyphs.APP,
                                    ),
                                ),
                            ),
                        ),
                    ),
                )
                onItemClick = { sheet.push(menuRoute()) }
            }
        column.addView(
            links,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )
        return column
    }

    private fun buildMenuPage(): View {
        val menu = GridMenu(this, style)
        fun paint() {
            menu.bind(menuSpec())
        }
        menu.onItemToggle = { item, on ->
            if (item.id == ITEM_FOCUS) {
                layout = if (on) LauncherLayout.Focus else LauncherLayout.Desktop
                bindLayoutButton()
                bindWorkspaces()
                paint()
            }
        }
        menu.onItemClick = { item ->
            when {
                item.id == ROUTE_THEME -> sheet.push(themeRoute())
                else -> {
                    val parts = item.id.split('/', limit = 2)
                    if (parts.size == 2) {
                        launcherApps.startMainActivity(
                            ComponentName(parts[0], parts[1]),
                            user,
                            null,
                            null,
                        )
                        sheet.dismiss()
                    }
                }
            }
        }
        paint()
        return menu
    }

    private fun menuSpec(): MenuSpec {
        val apps =
            launchableApps().map { app ->
                MenuItem(
                    id = "${app.packageName}/${app.activityName}",
                    title = app.label,
                    icon = IconGlyphs.APP,
                )
            }
        return MenuSpec(
            listOf(
                MenuSection(
                    getString(R.string.sheet_appearance),
                    listOf(
                        MenuItem(
                            id = ROUTE_THEME,
                            title = getString(R.string.sheet_theme),
                            icon = IconGlyphs.PALETTE,
                        ),
                    ),
                ),
                MenuSection(
                    getString(R.string.launcher_layout),
                    listOf(
                        MenuItem(
                            id = ITEM_FOCUS,
                            title = getString(R.string.launcher_layout_focus),
                            icon = IconGlyphs.LAYOUT,
                            toggled = layout == LauncherLayout.Focus,
                        ),
                    ),
                ),
                MenuSection(getString(R.string.sheet_apps), apps),
            ),
        )
    }

    private fun themeRoute(): NavRoute =
        NavRoute(ROUTE_THEME, getString(R.string.sheet_theme))

    private fun buildThemePage(): View {
        val menu = GridMenu(this, style)
        fun paint() {
            val slugs = ThemeCatalog.slugs(assets)
            menu.bind(
                MenuSpec(
                    listOf(
                        MenuSection(
                            getString(R.string.sheet_theme),
                            slugs.map { slug ->
                                MenuItem(
                                    id = slug,
                                    title = ThemeCatalog.displayName(slug),
                                    icon = IconGlyphs.PALETTE,
                                    selected = slug == theme.slug,
                                )
                            },
                        ),
                    ),
                ),
            )
        }
        menu.onItemClick = { item -> activateTheme(item.id) }
        paint()
        return menu
    }

    private fun loadSavedTheme(): ThemeColors {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val slug = prefs.getString(PREF_THEME, ThemeColors.DEFAULT_SLUG) ?: ThemeColors.DEFAULT_SLUG
        return try {
            ThemeCatalog.load(assets, slug)
        } catch (_: com.omadroid.theme.ThemeColorsException) {
            ThemeCatalog.load(assets, ThemeColors.DEFAULT_SLUG)
        }
    }

    private fun activateTheme(slug: String) {
        if (slug == theme.slug) {
            return
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(PREF_THEME, slug).apply()
        theme = ThemeCatalog.load(assets, slug)
        style = GridStyle(unitPx, theme)
        window.decorView.setBackgroundColor(theme.background)
        setContentView(buildChrome())
        bindLayoutButton()
        bindBar()
        bindWorkspaces()
        sheet.restore(NavStack.root(menuRoute()).push(themeRoute()))
    }

    private fun launchableApps(): List<LaunchableApp> {
        val infos = launcherApps.getActivityList(null, user)
        return visibleLaunchableApps(
            infos.map { info ->
                LaunchableApp(
                    packageName = info.componentName.packageName,
                    activityName = info.componentName.className,
                    label = info.label.toString(),
                )
            },
            packageName,
        )
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
            workspaceSwitcher.addView(button, gridCellParams(style))
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
        private const val ROUTE_SEARCH = "search"
        private const val ROUTE_MENU = "menu"
        private const val ROUTE_THEME = "theme"
        private const val ITEM_FOCUS = "focus"
        private const val PREFS = "omadroid"
        private const val PREF_THEME = "theme_slug"
    }
}
