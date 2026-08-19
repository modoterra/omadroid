package com.omadroid.launcher

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
)

fun builtinModules(): List<ModuleSpec> =
    listOf(
        ModuleSpec(BuiltinModule.Grid),
        ModuleSpec(BuiltinModule.Widgets),
        ModuleSpec(BuiltinModule.Bar),
        ModuleSpec(BuiltinModule.Workspaces),
        ModuleSpec(
            BuiltinModule.WorkspaceSwitcher,
            barPlacements =
                listOf(BarPlacement(BarModule.WorkspaceSwitcher, BarAnchor.Left)),
        ),
        ModuleSpec(BuiltinModule.Dock),
        ModuleSpec(BuiltinModule.Sheet),
    )

fun composeBarPlacements(
    modules: List<ModuleSpec>,
    base: List<BarPlacement>,
): List<BarPlacement> = modules.flatMap { it.barPlacements } + base
