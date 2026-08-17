package com.omadroid.shell.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PluginRegistryTest {
    @Test
    fun resolvesHomeKindToFirstPartyPlugin() {
        val registry = PluginRegistry.parseAll(listOf(PluginManifestTest.HOME_MANIFEST))

        assertEquals("omadroid.home", registry.plugin("omadroid.home")?.id)
        assertEquals("omadroid.home", registry.holder("home")?.id)
        assertEquals(
            "com.omadroid.launcher/.HomeActivity",
            registry.holder("home")?.entryPoint("home"),
        )
        assertNull(registry.holder("status"))
    }

    @Test
    fun rejectsTwoPluginsForTheSameKind() {
        val other = PluginManifestTest.HOME_MANIFEST
            .replace("omadroid.home", "omadroid.home.alt")
            .replace("OmadroidLauncher", "OmadroidLauncherAlt")
        assertThrows(PluginManifestException::class.java) {
            PluginRegistry.parseAll(listOf(PluginManifestTest.HOME_MANIFEST, other))
        }
    }

    @Test
    fun rejectsDuplicatePluginIds() {
        assertThrows(PluginManifestException::class.java) {
            PluginRegistry.parseAll(
                listOf(PluginManifestTest.HOME_MANIFEST, PluginManifestTest.HOME_MANIFEST),
            )
        }
    }
}
