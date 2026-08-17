package com.omadroid.launcher

data class Workspace(
    val id: String,
    val name: String,
)

data class Workspaces(
    val items: List<Workspace>,
    val activeId: String,
) {
    init {
        require(items.isNotEmpty()) { "workspaces must not be empty" }
        require(items.any { it.id == activeId }) { "active workspace $activeId is missing" }
    }

    val active: Workspace = items.first { it.id == activeId }

    fun select(id: String): Workspaces =
        if (items.any { it.id == id }) copy(activeId = id) else this
}

fun defaultWorkspaces(): Workspaces =
    Workspaces(
        items =
            listOf(
                Workspace("1", "1"),
                Workspace("2", "2"),
                Workspace("3", "3"),
            ),
        activeId = "1",
    )

fun visibleWorkspaces(workspaces: Workspaces, layout: LauncherLayout): List<Workspace> =
    when (layout) {
        LauncherLayout.Desktop -> workspaces.items
        LauncherLayout.Focus -> listOf(workspaces.active)
    }
