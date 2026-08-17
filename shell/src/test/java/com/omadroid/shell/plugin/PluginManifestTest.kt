package com.omadroid.shell.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PluginManifestTest {
    @Test
    fun parsesFirstPartyHomeManifest() {
        val plugin = PluginManifest.parse(HOME_MANIFEST)

        assertEquals(1, plugin.schemaVersion)
        assertEquals("omadroid.home", plugin.id)
        assertEquals("Home", plugin.name)
        assertEquals("0.1.0", plugin.version)
        assertEquals(listOf("home"), plugin.kinds)
        assertEquals("com.omadroid.launcher", plugin.packageName)
        assertEquals(
            mapOf("home" to "com.omadroid.launcher/.HomeActivity"),
            plugin.entryPoints,
        )
        assertEquals("OmadroidLauncher", plugin.soongModule)
    }

    @Test
    fun rejectsMissingId() {
        assertThrows(PluginManifestException::class.java) {
            PluginManifest.parse("""{"schemaVersion":1,"name":"Home","version":"0.1.0","kinds":["home"],"package":"com.omadroid.launcher","entryPoints":{},"soongModule":"OmadroidLauncher"}""")
        }
    }

    @Test
    fun rejectsIdWithoutFirstPartyPrefix() {
        assertThrows(PluginManifestException::class.java) {
            PluginManifest.parse(HOME_MANIFEST.replace("omadroid.home", "other.home"))
        }
    }

    companion object {
        const val HOME_MANIFEST = """
{
  "schemaVersion": 1,
  "id": "omadroid.home",
  "name": "Home",
  "version": "0.1.0",
  "kinds": ["home"],
  "package": "com.omadroid.launcher",
  "entryPoints": {
    "home": "com.omadroid.launcher/.HomeActivity"
  },
  "soongModule": "OmadroidLauncher"
}
"""
    }
}
