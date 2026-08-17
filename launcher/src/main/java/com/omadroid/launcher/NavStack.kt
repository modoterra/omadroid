package com.omadroid.launcher

data class NavRoute(
    val id: String,
    val title: String,
)

data class NavStack(
    val routes: List<NavRoute> = emptyList(),
) {
    val current: NavRoute? = routes.lastOrNull()
    val canPop: Boolean = routes.size > 1
    val isOpen: Boolean = routes.isNotEmpty()

    fun push(route: NavRoute): NavStack = copy(routes = routes + route)

    fun pop(): NavStack =
        if (routes.isEmpty()) {
            this
        } else {
            copy(routes = routes.dropLast(1))
        }

    companion object {
        fun root(route: NavRoute): NavStack = NavStack(listOf(route))
    }
}
