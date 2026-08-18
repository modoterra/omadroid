package com.omadroid.launcher

import com.omadroid.launcher.widget.IconGlyphs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PluginAppTest {
    @Test
    fun resolveRelativeActivity() {
        assertEquals(
            "com.omadroid.clock.ClockActivity",
            resolvePluginActivity("com.omadroid.clock", "com.omadroid.clock/.ClockActivity"),
        )
    }

    @Test
    fun contactsManifestRegistersACommandAndMenuItem() {
        val plugin = parsePluginApp(manifest("contacts"))
        assertEquals("omadroid.contacts", plugin.id)
        assertEquals("Contacts", plugin.name)
        assertEquals("com.omadroid.contacts/com.omadroid.contacts.ContactsActivity", plugin.launchId)
        assertEquals("omadroid.contacts", plugin.toCommand().module)
        assertEquals(IconGlyphs.CONTACTS, plugin.toCommand().icon)
        assertEquals(plugin.launchId, plugin.toMenuItem().id)
        assertFalse(isHomePlugin(plugin))
    }

    @Test
    fun galleryAndClockRegisterUnderTheirModuleIds() {
        val gallery = parsePluginApp(manifest("gallery"))
        val clock = parsePluginApp(manifest("clock"))
        assertEquals("omadroid.gallery", gallery.toCommand().module)
        assertEquals(IconGlyphs.GALLERY, gallery.toCommand().icon)
        assertEquals("omadroid.clock", clock.toCommand().module)
        assertEquals(IconGlyphs.CLOCK, clock.toCommand().icon)
    }

    @Test
    fun homePluginIsNotAnApp() {
        assertTrue(isHomePlugin(parsePluginApp(manifest("home"))))
    }

    @Test
    fun extraLaunchablesDropPackagesThePluginsOwn() {
        val plugins = listOf(parsePluginApp(manifest("clock")))
        val launchables =
            listOf(
                LaunchableApp("com.omadroid.clock", "ClockActivity", "Clock"),
                LaunchableApp("com.android.inputmethod.latin", "LatinIME", "Android Keyboard"),
            )
        assertEquals(
            listOf("Android Keyboard"),
            extraLaunchableApps(launchables, plugins).map { it.label },
        )
    }

    private fun manifest(name: String): String =
        File("../shell/plugins/$name/manifest.json").readText()
}
