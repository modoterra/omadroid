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

    @Test
    fun firstPartyAppsStayVisible() {
        val apps =
            listOf(
                LaunchableApp("com.omadroid.clock", "ClockActivity", "Clock"),
                LaunchableApp("com.omadroid.contacts", "ContactsActivity", "Contacts"),
                LaunchableApp("com.omadroid.gallery", "GalleryActivity", "Gallery"),
                LaunchableApp("com.android.settings", "Settings", "Settings"),
            )
        val visible = visibleLaunchableApps(apps, "com.omadroid.launcher")
        assertEquals(listOf("Clock", "Contacts", "Gallery"), visible.map { it.label })
    }

    @Test
    fun launchIdsArePackageAndActivity() {
        assertEquals(true, isLaunchId("com.omadroid.clock/com.omadroid.clock.ClockActivity"))
        assertEquals(false, isLaunchId("theme"))
        assertEquals(false, isLaunchId("focus"))
        assertEquals(false, isLaunchId("/ClockActivity"))
    }

    @Test
    fun hostedPackageReadsTheAppId() {
        assertEquals(
            "com.omadroid.clock",
            hostedPackage("com.omadroid.clock/com.omadroid.clock.ClockActivity"),
        )
        assertEquals(null, hostedPackage("theme"))
    }
}
