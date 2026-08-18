package com.omadroid.launcher

import android.content.Context
import android.content.res.AssetManager
import com.omadroid.launcher.widget.IconGlyphs
import com.omadroid.launcher.widget.MenuItem
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

data class PluginApp(
    val id: String,
    val name: String,
    val packageName: String,
    val activityName: String,
    val kinds: List<String>,
) {
    val launchId: String
        get() = "$packageName/$activityName"

    fun toCommand(): CommandItem =
        CommandItem(
            id = launchId,
            title = name,
            module = id,
            icon = iconForKinds(kinds),
            keywords = listOf(name, packageName) + kinds,
        )

    fun toMenuItem(): MenuItem =
        MenuItem(
            id = launchId,
            title = name,
            icon = iconForKinds(kinds),
        )
}

fun iconForKinds(kinds: List<String>): String =
    when {
        "clock" in kinds -> IconGlyphs.CLOCK
        "contacts" in kinds -> IconGlyphs.CONTACTS
        "gallery" in kinds -> IconGlyphs.GALLERY
        else -> IconGlyphs.APP
    }

fun resolvePluginActivity(packageName: String, entry: String): String {
    val slash = entry.indexOf('/')
    val className = if (slash >= 0) entry.substring(slash + 1) else entry
    return if (className.startsWith('.')) packageName + className else className
}

fun parsePluginApp(json: String): PluginApp {
    val obj = JSONObject(json)
    val id = obj.getString("id")
    val kinds =
        buildList {
            val array = obj.getJSONArray("kinds")
            for (index in 0 until array.length()) {
                add(array.getString(index))
            }
        }
    val packageName = obj.getString("package")
    val entries = obj.getJSONObject("entryPoints")
    val kind = kinds.firstOrNull { it != "home" } ?: kinds.first()
    val entry =
        if (entries.has(kind)) {
            entries.getString(kind)
        } else {
            entries.keys().asSequence().firstOrNull()?.let { entries.getString(it) }.orEmpty()
        }
    return PluginApp(
        id = id,
        name = obj.getString("name"),
        packageName = packageName,
        activityName = resolvePluginActivity(packageName, entry),
        kinds = kinds,
    )
}

fun isHomePlugin(plugin: PluginApp): Boolean = "home" in plugin.kinds

fun loadPluginApps(assets: AssetManager): List<PluginApp> {
    val dirs = assets.list("") ?: return emptyList()
    return dirs
        .mapNotNull { dir ->
            val text =
                try {
                    assets.open("$dir/manifest.json").bufferedReader().use { it.readText() }
                } catch (_: IOException) {
                    return@mapNotNull null
                }
            val plugin =
                try {
                    parsePluginApp(text)
                } catch (_: Exception) {
                    return@mapNotNull null
                }
            if (isHomePlugin(plugin)) null else plugin
        }.sortedBy { it.name.lowercase(Locale.US) }
}

fun loadPluginApps(context: Context): List<PluginApp> = loadPluginApps(context.assets)

fun pluginPackages(plugins: List<PluginApp>): Set<String> = plugins.map { it.packageName }.toSet()

fun extraLaunchableApps(
    launchables: List<LaunchableApp>,
    plugins: List<PluginApp>,
): List<LaunchableApp> {
    val owned = pluginPackages(plugins)
    return launchables.filter { it.packageName !in owned }
}
