package com.omadroid.launcher

enum class BarAnchor {
    Left,
    Center,
    Right,
}

enum class BarModule {
    Date,
    Wifi,
    Battery,
    WorkspaceSwitcher,
}

data class BarPlacement(
    val module: BarModule,
    val anchor: BarAnchor,
)

val defaultBarPlacements: List<BarPlacement> =
    listOf(
        BarPlacement(BarModule.Date, BarAnchor.Center),
        BarPlacement(BarModule.Wifi, BarAnchor.Right),
        BarPlacement(BarModule.Battery, BarAnchor.Right),
    )

fun arrangeBar(placements: List<BarPlacement>): Map<BarAnchor, List<BarModule>> =
    BarAnchor.entries.associateWith { anchor ->
        placements.filter { it.anchor == anchor }.map { it.module }
    }
