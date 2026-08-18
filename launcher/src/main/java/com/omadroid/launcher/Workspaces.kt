package com.omadroid.launcher

internal const val ITEM_DWINDLE = "dwindle"
internal const val ITEM_SCROLLING = "scrolling"

enum class WorkspaceLayout {
    Dwindle,
    Scrolling,
    ;

    fun next(): WorkspaceLayout =
        when (this) {
            Dwindle -> Scrolling
            Scrolling -> Dwindle
        }
}

data class WorkspaceClient(
    val id: String,
    val title: String,
    val launchId: String,
)

data class Workspace(
    val id: String,
    val name: String,
    val layout: WorkspaceLayout = WorkspaceLayout.Dwindle,
    val clients: List<WorkspaceClient> = emptyList(),
    val focusedIndex: Int = 0,
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

    fun addClient(client: WorkspaceClient): Workspaces = addClient(activeId, client)

    fun addClient(workspaceId: String, client: WorkspaceClient): Workspaces =
        update(workspaceId) { workspace ->
            val clients = workspace.clients + client
            workspace.copy(
                clients = clients,
                focusedIndex = clampScrollIndex(clients.lastIndex, clients.size),
            )
        }

    fun setLayout(layout: WorkspaceLayout, workspaceId: String = activeId): Workspaces =
        update(workspaceId) { it.copy(layout = layout) }

    fun cycleLayout(workspaceId: String = activeId): Workspaces =
        update(workspaceId) { it.copy(layout = it.layout.next()) }

    fun focusPage(index: Int, workspaceId: String = activeId): Workspaces =
        update(workspaceId) { it.copy(focusedIndex = clampScrollIndex(index, it.clients.size)) }

    private fun update(workspaceId: String, transform: (Workspace) -> Workspace): Workspaces {
        if (items.none { it.id == workspaceId }) {
            return this
        }
        return copy(
            items =
                items.map { workspace ->
                    if (workspace.id == workspaceId) transform(workspace) else workspace
                },
        )
    }
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

fun clampScrollIndex(index: Int, count: Int): Int {
    if (count <= 0) {
        return 0
    }
    return index.coerceIn(0, count - 1)
}

fun nextScrollIndex(index: Int, count: Int): Int = clampScrollIndex(index + 1, count)

fun prevScrollIndex(index: Int, count: Int): Int = clampScrollIndex(index - 1, count)
