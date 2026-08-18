package com.omadroid.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Process
import android.os.UserHandle
import android.view.View
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.omadroid.compose.Compose
import com.omadroid.compose.Direction
import com.omadroid.compose.Glyph
import com.omadroid.compose.Input
import com.omadroid.compose.IconButton
import com.omadroid.compose.CommandList
import com.omadroid.compose.Menu
import com.omadroid.compose.SHEET_BACK_SCALE
import com.omadroid.compose.Sheet
import com.omadroid.compose.Slot
import com.omadroid.compose.rememberSheetProgress
import com.omadroid.compose.Spacer
import com.omadroid.compose.Stack
import com.omadroid.compose.Text
import com.omadroid.compose.ThemeWipe
import com.omadroid.launcher.widget.GridStyle
import com.omadroid.launcher.widget.IconGlyphs
import com.omadroid.launcher.widget.MenuItem
import com.omadroid.launcher.widget.MenuSection
import com.omadroid.launcher.widget.MenuSpec

import com.omadroid.theme.ThemeCatalog
import com.omadroid.theme.ThemeColors

internal const val ROUTE_SEARCH = "search"
internal const val ROUTE_MENU = "menu"
internal const val ROUTE_THEME = "theme"
internal const val ITEM_FOCUS = "focus"

data class HomeState(
    val style: GridStyle,
    val layout: LauncherLayout,
    val workspaces: Workspaces,
    val bar: BarStatus,
    val sheet: NavStack,
    val commandOpen: Boolean,
    val commandQuery: String,
    val themeListY: Int,
    val wipe: ImageBitmap?,
)

@Composable
fun Home(
    state: HomeState,
    onLayout: () -> Unit,
    onSelectWorkspace: (String) -> Unit,
    onCommand: (String) -> Unit,
    onCommandOpen: () -> Unit,
    onCommandDismiss: () -> Unit,
    onOpenMenu: () -> Unit,
    onSheetBack: () -> Unit,
    onSheetDismiss: () -> Unit,
    onMenuClick: (MenuItem) -> Unit,
    onCommandClick: (CommandItem) -> Unit,
    onMenuToggle: (MenuItem, Boolean) -> Unit,
    onThemeClick: (String) -> Unit,
    onThemeScroll: (Int) -> Unit,
    onWipeDone: () -> Unit,
) {
    val context = LocalContext.current
    val sheetProgress = rememberSheetProgress(state.sheet.isOpen)
    var wallpaper by remember { mutableStateOf<ImageBitmap?>(null) }
    val slug = state.style.colors.slug
    LaunchedEffect(slug) {
        wallpaper =
            withContext(Dispatchers.IO) {
                loadThemeBackground(context.assets, slug)
            }
    }
    Compose(state.style, wallpaper = wallpaper) {
        Box(Modifier.fillMaxSize()) {
            val backScale = 1f - sheetProgress.value * (1f - SHEET_BACK_SCALE)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = backScale
                        scaleY = backScale
                    },
            ) {
                Stack(Direction.Vertical, gap = false) {
                    Node(Slot.units(1)) {
                        Bar(state, onSelectWorkspace)
                    }
                    Node(Slot.grow()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .semantics {
                                    contentDescription =
                                        context.getString(
                                            R.string.workspace_label,
                                            state.workspaces.active.name,
                                        )
                                },
                        )
                    }
                    Node(Slot.units(1)) {
                        Dock(
                            layout = state.layout,
                            query = state.commandQuery,
                            commandOpen = state.commandOpen,
                            onQuery = onCommand,
                            onCommandOpen = onCommandOpen,
                            onLayout = onLayout,
                            onOpenMenu = onOpenMenu,
                        )
                    }
                }
            }
            CommandPalette(
                open = state.commandOpen,
                query = state.commandQuery,
                layout = state.layout,
                workspaces = state.workspaces,
                onDismiss = onCommandDismiss,
                onItemClick = onCommandClick,
            )
            Sheet(
                state.sheet,
                sheetProgress,
                onBack = onSheetBack,
                onDismiss = onSheetDismiss,
            ) { route ->
                when (route.id) {
                    ROUTE_MENU ->
                        Menu(
                            spec = menuSpec(context, state.layout),
                            onItemClick = onMenuClick,
                            onItemToggle = onMenuToggle,
                        )
                    ROUTE_THEME ->
                        Menu(
                            spec = themeSpec(context, state.style.colors),
                            scrollY = state.themeListY,
                            onScroll = onThemeScroll,
                            onItemClick = { onThemeClick(it.id) },
                        )
                    else -> Text(route.title)
                }
            }
            state.wipe?.let { frame ->
                ThemeWipe(frame, onDone = onWipeDone)
            }
        }
    }
}

@Composable
private fun Bar(
    state: HomeState,
    onSelectWorkspace: (String) -> Unit,
) {
    val context = LocalContext.current
    val arranged = arrangeBar(composeBarPlacements(builtinModules(), defaultBarPlacements))
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(state.style.colors.lighterBackground).copy(alpha = 0.86f))
            .semantics { contentDescription = context.getString(R.string.bar_name) },
    ) {
        BarAnchor.entries.forEach { anchor ->
            Anchor(anchor, arranged.getValue(anchor), state, onSelectWorkspace)
        }
    }
}

@Composable
private fun Anchor(
    anchor: BarAnchor,
    modules: List<BarModule>,
    state: HomeState,
    onSelectWorkspace: (String) -> Unit,
) {
    val context = LocalContext.current
    val description =
        when (anchor) {
            BarAnchor.Left -> context.getString(R.string.bar_anchor_left)
            BarAnchor.Center -> context.getString(R.string.bar_anchor_center)
            BarAnchor.Right -> context.getString(R.string.bar_anchor_right)
        }
    Box(Modifier.fillMaxSize().semantics { contentDescription = description }) {
        Stack(Direction.Horizontal) {
            if (anchor == BarAnchor.Right || anchor == BarAnchor.Center) {
                Spacer(Slot.grow())
            }
            modules.forEach { module ->
                Node(Slot.fit) {
                    BarModuleView(module, state, onSelectWorkspace)
                }
            }
            if (anchor == BarAnchor.Center) {
                Spacer(Slot.grow())
            }
        }
    }
}

@Composable
private fun BarModuleView(
    module: BarModule,
    state: HomeState,
    onSelectWorkspace: (String) -> Unit,
) {
    val theme = state.style.colors
    when (module) {
        BarModule.Date -> Text(state.bar.dateLabel)
        BarModule.Wifi -> {
            val color =
                when (state.bar.wifi) {
                    WifiState.Connected -> theme.green
                    WifiState.Disconnected -> theme.muted
                    WifiState.Off -> theme.red
                }
            Glyph(
                IconGlyphs.WIFI,
                color = color,
                description = wifiContentDescription(state.bar.wifi),
                alpha = if (state.bar.wifi == WifiState.Off) 0.45f else 1f,
            )
        }
        BarModule.Battery -> {
            val color =
                when {
                    state.bar.charging -> theme.green
                    state.bar.batteryPercent <= 20 -> theme.red
                    else -> theme.foreground
                }
            Text(
                state.bar.batteryLabel,
                color = color,
                description =
                    batteryContentDescription(
                        state.bar.batteryPercent,
                        state.bar.charging,
                        java.util.Locale.getDefault(),
                    ),
            )
        }
        BarModule.WorkspaceSwitcher -> {
            if (state.layout == LauncherLayout.Focus) {
                return
            }
            val context = LocalContext.current
            Stack(Direction.Horizontal) {
                visibleWorkspaces(state.workspaces, state.layout).forEach { workspace ->
                    Node(Slot.square) {
                        val selected = workspace.id == state.workspaces.activeId
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            IconButton(
                                glyph = workspace.name,
                                description =
                                    context.getString(R.string.workspace_label, workspace.name),
                                color = if (selected) theme.accent else theme.muted,
                                onClick = { onSelectWorkspace(workspace.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandPalette(
    open: Boolean,
    query: String,
    layout: LauncherLayout,
    workspaces: Workspaces,
    onDismiss: () -> Unit,
    onItemClick: (CommandItem) -> Unit,
) {
    val context = LocalContext.current
    val style = com.omadroid.compose.LocalGridStyle.current
    val density = LocalDensity.current
    val keyboard = LocalSoftwareKeyboardController.current
    val alpha by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = tween(180),
        label = "command",
    )
    if (alpha <= 0.01f) {
        return
    }
    val dock = with(density) { style.unitPx.toDp() }
    val items =
        fuzzySearch(
            collectCommands(
                builtinModules(),
                CommandScope(context, layout, workspaces),
            ),
            query,
        )
    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = dock)
            .graphicsLayer { this.alpha = alpha }
            .background(Color(style.colors.background).copy(alpha = 0.94f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    keyboard?.hide()
                    onDismiss()
                },
            ),
    ) {
        CommandList(items = items, onItemClick = onItemClick)
    }
}

@Composable
private fun Dock(
    layout: LauncherLayout,
    query: String,
    commandOpen: Boolean,
    onQuery: (String) -> Unit,
    onCommandOpen: () -> Unit,
    onLayout: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val context = LocalContext.current
    val style = com.omadroid.compose.LocalGridStyle.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(style.colors.lighterBackground).copy(alpha = 0.86f))
            .semantics { contentDescription = context.getString(R.string.dock_name) },
    ) {
        Stack(Direction.Horizontal, gap = true, pad = false) {
            Input(
                value = query,
                onValueChange = onQuery,
                hint = context.getString(R.string.launcher_command),
                slot = Slot.grow(),
                icon = IconGlyphs.COMMAND,
                accent = false,
                autoFocus = commandOpen,
                onFocus = onCommandOpen,
            )
            Node(Slot.square) {
                IconButton(
                    IconGlyphs.LAYOUT,
                    if (layout == LauncherLayout.Focus) {
                        context.getString(R.string.launcher_layout_focus)
                    } else {
                        context.getString(R.string.launcher_layout_desktop)
                    },
                    color = if (layout == LauncherLayout.Focus) style.colors.accent else style.colors.foreground,
                    onClick = onLayout,
                )
            }
            Node(Slot.square) {
                IconButton(
                    IconGlyphs.MENU,
                    context.getString(R.string.launcher_menu),
                    onClick = onOpenMenu,
                )
            }
        }
    }
}

internal fun menuSpec(context: Context, layout: LauncherLayout): MenuSpec {
    val apps =
        launchableApps(context).map { app ->
            MenuItem(
                id = "${app.packageName}/${app.activityName}",
                title = app.label,
                icon = IconGlyphs.APP,
            )
        }
    return MenuSpec(
        listOf(
            MenuSection(
                context.getString(R.string.sheet_appearance),
                listOf(
                    MenuItem(
                        id = ROUTE_THEME,
                        title = context.getString(R.string.sheet_theme),
                        icon = IconGlyphs.PAINT,
                    ),
                ),
            ),
            MenuSection(
                context.getString(R.string.launcher_layout),
                listOf(
                    MenuItem(
                        id = ITEM_FOCUS,
                        title = context.getString(R.string.launcher_layout_focus),
                        icon = IconGlyphs.LAYOUT,
                        toggled = layout == LauncherLayout.Focus,
                    ),
                ),
            ),
            MenuSection(context.getString(R.string.sheet_apps), apps),
        ),
    )
}

internal fun themeSpec(context: Context, theme: ThemeColors): MenuSpec {
    val slugs = ThemeCatalog.slugs(context.assets)
    return MenuSpec(
        listOf(
            MenuSection(
                "",
                slugs.map { slug ->
                    MenuItem(
                        id = slug,
                        title = ThemeCatalog.displayName(slug),
                        selected = slug == theme.slug,
                        swatches =
                            try {
                                ThemeCatalog.load(context.assets, slug).previewSwatches()
                            } catch (_: com.omadroid.theme.ThemeColorsException) {
                                emptyList()
                            },
                    )
                },
            ),
        ),
    )
}

internal fun launchableApps(context: Context): List<LaunchableApp> {
    val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)
    val user: UserHandle = Process.myUserHandle()
    val infos = launcherApps.getActivityList(null, user)
    return visibleLaunchableApps(
        infos.map { info ->
            LaunchableApp(
                packageName = info.componentName.packageName,
                activityName = info.componentName.className,
                label = info.label.toString(),
            )
        },
        context.packageName,
    )
}

internal fun startLaunchable(context: Context, itemId: String): Boolean {
    val parts = itemId.split('/', limit = 2)
    if (parts.size != 2) {
        return false
    }
    val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)
    launcherApps.startMainActivity(
        ComponentName(parts[0], parts[1]),
        Process.myUserHandle(),
        null,
        null,
    )
    return true
}

internal fun snapshotView(view: View): ImageBitmap? {
    if (view.width <= 0 || view.height <= 0) {
        return null
    }
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return bitmap.asImageBitmap()
}
