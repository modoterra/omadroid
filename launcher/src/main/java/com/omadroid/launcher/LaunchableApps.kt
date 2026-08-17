package com.omadroid.launcher

data class LaunchableApp(
    val packageName: String,
    val activityName: String,
    val label: String,
)

fun visibleLaunchableApps(
    apps: List<LaunchableApp>,
    selfPackageName: String,
): List<LaunchableApp> {
    return apps
        .filter { it.packageName != selfPackageName }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
