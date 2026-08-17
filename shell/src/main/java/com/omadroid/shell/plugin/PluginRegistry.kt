package com.omadroid.shell.plugin

class PluginRegistry(
    val plugins: List<PluginManifest>,
) {
    private val byId: Map<String, PluginManifest> = plugins.associateBy { it.id }
    private val byKind: Map<String, PluginManifest>

    init {
        val kinds = mutableMapOf<String, PluginManifest>()
        for (plugin in plugins) {
            for (kind in plugin.kinds) {
                val existing = kinds.put(kind, plugin)
                if (existing != null) {
                    throw PluginManifestException(
                        "kind $kind is claimed by ${existing.id} and ${plugin.id}",
                    )
                }
            }
        }
        byKind = kinds
    }

    fun plugin(id: String): PluginManifest? = byId[id]

    fun holder(kind: String): PluginManifest? = byKind[kind]

    companion object {
        fun parseAll(manifests: List<String>): PluginRegistry {
            val parsed = manifests.map { PluginManifest.parse(it) }
            val seen = mutableSetOf<String>()
            for (plugin in parsed) {
                if (!seen.add(plugin.id)) {
                    throw PluginManifestException("duplicate plugin id ${plugin.id}")
                }
            }
            return PluginRegistry(parsed)
        }
    }
}
