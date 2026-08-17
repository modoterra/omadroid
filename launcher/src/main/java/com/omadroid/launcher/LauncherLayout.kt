package com.omadroid.launcher

enum class LauncherLayout {
    Desktop,
    Focus,
    ;

    fun next(): LauncherLayout =
        when (this) {
            Desktop -> Focus
            Focus -> Desktop
        }
}
