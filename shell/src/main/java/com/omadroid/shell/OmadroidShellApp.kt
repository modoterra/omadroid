package com.omadroid.shell

import android.app.Application
import android.content.res.AssetManager
import android.util.Log
import com.omadroid.shell.plugin.PluginRegistry
import java.io.IOException

class OmadroidShellApp : Application() {
    lateinit var plugins: PluginRegistry
        private set

    override fun onCreate() {
        super.onCreate()
        val texts = readPluginManifests(assets)
        if (texts.isEmpty()) {
            throw IllegalStateException("no plugin manifests in assets")
        }
        plugins = PluginRegistry.parseAll(texts)
        Log.i(TAG, "loaded ${plugins.plugins.size} plugin(s)")
    }

    companion object {
        private const val TAG = "OmadroidShell"

        fun readPluginManifests(assets: AssetManager): List<String> {
            val dirs = assets.list("") ?: return emptyList()
            return dirs.mapNotNull { dir ->
                try {
                    assets.open("$dir/manifest.json").bufferedReader().use { it.readText() }
                } catch (_: IOException) {
                    null
                }
            }
        }
    }
}
