package com.omadroid.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class LauncherLayoutTest {
    @Test
    fun cyclesDesktopAndFocus() {
        assertEquals(LauncherLayout.Focus, LauncherLayout.Desktop.next())
        assertEquals(LauncherLayout.Desktop, LauncherLayout.Focus.next())
    }
}
