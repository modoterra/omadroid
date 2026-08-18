package com.omadroid.launcher

import com.omadroid.launcher.widget.IconGlyphs

enum class BuiltinModule(val id: String) {
    Grid("omadroid.grid"),
    Widgets("omadroid.widgets"),
    Bar("omadroid.bar"),
    Workspaces("omadroid.workspaces"),
    WorkspaceSwitcher("omadroid.workspace-switcher"),
    Dock("omadroid.dock"),
    Sheet("omadroid.sheet"),
}

data class ModuleSpec(
    val module: BuiltinModule,
    val barPlacements: List<BarPlacement> = emptyList(),
    val commands: (CommandScope) -> List<CommandItem> = { emptyList() },
)

fun builtinModules(): List<ModuleSpec> =
    listOf(
        ModuleSpec(BuiltinModule.Grid),
        ModuleSpec(BuiltinModule.Widgets),
        ModuleSpec(BuiltinModule.Bar),
        ModuleSpec(
            BuiltinModule.Workspaces,
            commands = { scope ->
                listOf(
                    CommandItem(
                        id = ITEM_DWINDLE,
                        title = scope.context.getString(R.string.workspace_layout_dwindle),
                        module = BuiltinModule.Workspaces.id,
                        icon = IconGlyphs.LAYOUT,
                        hint = "Workspace",
                        keywords = listOf("dwindle", "tile", "split", "layout"),
                    ),
                    CommandItem(
                        id = ITEM_SCROLLING,
                        title = scope.context.getString(R.string.workspace_layout_scrolling),
                        module = BuiltinModule.Workspaces.id,
                        icon = IconGlyphs.LAYOUT,
                        hint = "Workspace",
                        keywords = listOf("scrolling", "pager", "swipe", "layout"),
                    ),
                )
            },
        ),
        ModuleSpec(
            BuiltinModule.WorkspaceSwitcher,
            barPlacements =
                listOf(BarPlacement(BarModule.WorkspaceSwitcher, BarAnchor.Left)),
            commands = { scope ->
                scope.workspaces.items.map { workspace ->
                    CommandItem(
                        id = "workspace/${workspace.id}",
                        title = scope.context.getString(R.string.workspace_label, workspace.name),
                        module = BuiltinModule.WorkspaceSwitcher.id,
                        icon = IconGlyphs.LAYOUT,
                        hint = "Workspace",
                        keywords = listOf(workspace.name, workspace.id),
                    )
                }
            },
        ),
        ModuleSpec(
            BuiltinModule.Dock,
            commands = { scope ->
                val focus =
                    CommandItem(
                        id = ITEM_FOCUS,
                        title = scope.context.getString(R.string.launcher_layout_focus),
                        module = BuiltinModule.Dock.id,
                        icon = IconGlyphs.LAYOUT,
                        hint = "Layout",
                        keywords = listOf("desktop", "focus", "layout"),
                    )
                val plugins = loadPluginApps(scope.context)
                val others =
                    extraLaunchableApps(launchableApps(scope.context), plugins).map { app ->
                        CommandItem(
                            id = "${app.packageName}/${app.activityName}",
                            title = app.label,
                            module = BuiltinModule.Dock.id,
                            icon = IconGlyphs.APP,
                            hint = "App",
                            keywords = listOf(app.packageName),
                        )
                    }
                listOf(focus) + plugins.map { it.toCommand() } + others
            },
        ),
        ModuleSpec(
            BuiltinModule.Sheet,
            commands = { scope ->
                listOf(
                    CommandItem(
                        id = ROUTE_THEME,
                        title = scope.context.getString(R.string.sheet_theme),
                        module = BuiltinModule.Sheet.id,
                        icon = IconGlyphs.PAINT,
                        hint = "Appearance",
                        keywords = listOf("color", "palette", "theme"),
                    ),
                )
            },
        ),
    )

fun composeBarPlacements(
    modules: List<ModuleSpec>,
    base: List<BarPlacement>,
): List<BarPlacement> = modules.flatMap { it.barPlacements } + base
