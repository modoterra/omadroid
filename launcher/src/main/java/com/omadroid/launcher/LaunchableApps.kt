package com.omadroid.launcher

data class LaunchableApp(
    val packageName: String,
    val activityName: String,
    val label: String,
)

val hiddenLaunchablePackages =
    setOf(
        "com.android.settings",
        "com.android.documentsui",
    )

fun visibleLaunchableApps(
    apps: List<LaunchableApp>,
    selfPackageName: String,
    hiddenPackages: Set<String> = hiddenLaunchablePackages,
): List<LaunchableApp> {
    return apps
        .filter { it.packageName != selfPackageName }
        .filter { it.packageName !in hiddenPackages }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
