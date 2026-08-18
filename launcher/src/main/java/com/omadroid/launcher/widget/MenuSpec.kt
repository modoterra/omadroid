package com.omadroid.launcher.widget

data class MenuItem(
    val id: String,
    val title: String,
    val icon: String? = null,
    val toggled: Boolean? = null,
    val selected: Boolean = false,
    val swatches: List<Int> = emptyList(),
)

data class MenuSection(
    val header: String,
    val items: List<MenuItem>,
)

data class MenuSpec(
    val sections: List<MenuSection>,
)

fun menuRows(spec: MenuSpec): List<MenuItem> =
    spec.sections.flatMap { it.items }

fun filterMenu(spec: MenuSpec, query: String): MenuSpec {
    val needle = query.trim()
    if (needle.isEmpty()) {
        return spec
    }
    return MenuSpec(
        spec.sections.mapNotNull { section ->
            val items = section.items.filter { it.title.contains(needle, ignoreCase = true) }
            if (items.isEmpty()) {
                null
            } else {
                section.copy(items = items)
            }
        },
    )
}

fun withToggled(spec: MenuSpec, id: String, toggled: Boolean): MenuSpec =
    spec.copy(
        sections =
            spec.sections.map { section ->
                section.copy(
                    items =
                        section.items.map { item ->
                            if (item.id == id && item.toggled != null) {
                                item.copy(toggled = toggled)
                            } else {
                                item
                            }
                        },
                )
            },
    )
