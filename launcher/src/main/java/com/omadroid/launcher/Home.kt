package com.omadroid.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.view.View
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer as Flex
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import com.omadroid.compose.InputWell
import com.omadroid.compose.IconButton
import com.omadroid.compose.LocalGridStyle
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
    onSelectWorkspace: (String) -> Unit,
    onCycleWorkspaceLayout: () -> Unit,
    onClientClick: (WorkspaceClient) -> Unit,
    onFocusWorkspacePage: (Int) -> Unit,
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
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
                        Bar(state, wallpaper, screenHeight, onSelectWorkspace)
                    }
                    Node(Slot.grow()) {
                        val inset = with(LocalDensity.current) { state.style.workspaceInsetPx.toDp() }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(inset)
                                .semantics {
                                    contentDescription =
                                        context.getString(
                                            R.string.workspace_label,
                                            state.workspaces.active.name,
                                        )
                                },
                        ) {
                            ActiveWorkspace(
                                workspace = state.workspaces.active,
                                onClientClick = onClientClick,
                                onFocusPage = onFocusWorkspacePage,
                            )
                        }
                    }
                    Node(Slot.units(1)) {
                        Dock(
                            workspaceLayout = state.workspaces.active.layout,
                            query = state.commandQuery,
                            commandOpen = state.commandOpen,
                            wallpaper = wallpaper,
                            screenHeight = screenHeight,
                            onQuery = onCommand,
                            onCommandOpen = onCommandOpen,
                            onCycleWorkspaceLayout = onCycleWorkspaceLayout,
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
    wallpaper: ImageBitmap?,
    screenHeight: Dp,
    onSelectWorkspace: (String) -> Unit,
) {
    val context = LocalContext.current
    val arranged = arrangeBar(composeBarPlacements(builtinModules(), defaultBarPlacements))
    ChromePlate(
        wallpaper = wallpaper,
        screenHeight = screenHeight,
        align = Alignment.TopCenter,
        description = context.getString(R.string.bar_name),
    ) {
        BarAnchor.entries.forEach { anchor ->
            Anchor(anchor, arranged.getValue(anchor), state, onSelectWorkspace)
        }
    }
}

@Composable
private fun ChromePlate(
    wallpaper: ImageBitmap?,
    screenHeight: Dp,
    align: Alignment,
    description: String,
    content: @Composable BoxScope.() -> Unit,
) {
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val canBlur = Build.VERSION.SDK_INT >= 31 && style.chromeBlurPx > 0
    val fill = Color(style.colors.lighterBackground)
    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .semantics { contentDescription = description },
    ) {
        if (wallpaper != null && canBlur) {
            Image(
                bitmap = wallpaper,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(screenHeight)
                        .align(align)
                        .blur(with(density) { style.chromeBlurPx.toDp() }),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(fill.copy(alpha = if (canBlur) style.chromeFillAlpha else 1f)),
        )
        content()
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
    workspaceLayout: WorkspaceLayout,
    query: String,
    commandOpen: Boolean,
    wallpaper: ImageBitmap?,
    screenHeight: Dp,
    onQuery: (String) -> Unit,
    onCommandOpen: () -> Unit,
    onCycleWorkspaceLayout: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    val context = LocalContext.current
    val style = LocalGridStyle.current
    val density = LocalDensity.current
    val gap = with(density) { style.spacePx.toDp() }
    val commandLabel = context.getString(R.string.launcher_command)
    val layoutLabel =
        when (workspaceLayout) {
            WorkspaceLayout.Dwindle -> context.getString(R.string.workspace_layout_dwindle)
            WorkspaceLayout.Scrolling -> context.getString(R.string.workspace_layout_scrolling)
        }
    ChromePlate(
        wallpaper = wallpaper,
        screenHeight = screenHeight,
        align = Alignment.BottomCenter,
        description = context.getString(R.string.dock_name),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cell = maxHeight
            val target =
                with(density) {
                    commandFieldWidthPx(
                        open = commandOpen,
                        dockWidthPx = constraints.maxWidth,
                        cellPx = style.cellPx,
                        spacePx = style.spacePx,
                    ).toDp()
                }
            val commandWidth by animateDpAsState(
                targetValue = target,
                animationSpec = tween(180),
                label = "command-field",
            )
            val expand by animateFloatAsState(
                targetValue = if (commandOpen) 1f else 0f,
                animationSpec = tween(180),
                label = "command-well",
            )
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .width(commandWidth)
                        .fillMaxHeight()
                        .clipToBounds(),
                ) {
                    InputWell(
                        value = query,
                        onValueChange = onQuery,
                        icon = IconGlyphs.COMMAND,
                        accent = false,
                        autoFocus = commandOpen,
                        expanded = expand > 0.02f,
                        description = commandLabel,
                        wellAlpha = expand,
                        onClick = if (commandOpen) null else onCommandOpen,
                        onFocus = onCommandOpen,
                    )
                }
                Flex(Modifier.width(gap))
                Flex(Modifier.weight(1f))
                Box(Modifier.size(cell)) {
                    IconButton(
                        IconGlyphs.LAYOUT,
                        layoutLabel,
                        color =
                            if (workspaceLayout == WorkspaceLayout.Scrolling) {
                                style.colors.accent
                            } else {
                                style.colors.foreground
                            },
                        onClick = onCycleWorkspaceLayout,
                    )
                }
                Flex(Modifier.width(gap))
                Box(Modifier.size(cell)) {
                    IconButton(
                        IconGlyphs.MENU,
                        context.getString(R.string.launcher_menu),
                        onClick = onOpenMenu,
                    )
                }
            }
        }
    }
}

internal fun commandFieldWidthPx(
    open: Boolean,
    dockWidthPx: Int,
    cellPx: Int,
    spacePx: Int,
): Int {
    val expanded = (dockWidthPx - 2 * cellPx - 2 * spacePx).coerceAtLeast(cellPx)
    return if (open) expanded else cellPx
}

internal fun menuSpec(context: Context, layout: LauncherLayout): MenuSpec {
    val plugins = loadPluginApps(context)
    val apps =
        plugins.map { it.toMenuItem() } +
            extraLaunchableApps(launchableApps(context), plugins).map { app ->
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

internal fun isLaunchId(itemId: String): Boolean {
    val parts = itemId.split('/', limit = 2)
    return parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()
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
