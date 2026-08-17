package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchableAppsTest {
    @Test
    fun hidesSelfAndSortsByLabel() {
        val apps =
            listOf(
                LaunchableApp("com.android.settings", "Settings", "Settings"),
                LaunchableApp("com.android.documentsui", "Files", "Files"),
                LaunchableApp("com.omadroid.launcher", "HomeActivity", "Omadroid"),
                LaunchableApp("com.android.inputmethod.latin", "LatinIME", "Android Keyboard"),
            )

        val visible = visibleLaunchableApps(apps, "com.omadroid.launcher")

        assertEquals(
            listOf("Android Keyboard"),
            visible.map { it.label },
        )
    }
}
