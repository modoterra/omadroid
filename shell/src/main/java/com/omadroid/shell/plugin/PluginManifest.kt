package com.omadroid.shell.plugin

import org.json.JSONObject

class PluginManifestException(message: String) : Exception(message)

data class PluginManifest(
    val schemaVersion: Int,
    val id: String,
    val name: String,
    val version: String,
    val kinds: List<String>,
    val packageName: String,
    val entryPoints: Map<String, String>,
    val soongModule: String,
) {
    companion object {
        const val FIRST_PARTY_PREFIX = "omadroid."
        const val SUPPORTED_SCHEMA = 1

        fun parse(json: String): PluginManifest {
            val obj = try {
                JSONObject(json)
            } catch (error: Exception) {
                throw PluginManifestException("manifest is not JSON: ${error.message}")
            }
            val schemaVersion = obj.requiredInt("schemaVersion")
            if (schemaVersion != SUPPORTED_SCHEMA) {
                throw PluginManifestException("unsupported schemaVersion $schemaVersion")
            }
            val id = obj.requiredString("id")
            if (!id.startsWith(FIRST_PARTY_PREFIX) || id == FIRST_PARTY_PREFIX) {
                throw PluginManifestException("plugin id must use the $FIRST_PARTY_PREFIX prefix: $id")
            }
            val kinds = obj.requiredStringList("kinds")
            if (kinds.isEmpty()) {
                throw PluginManifestException("plugin $id has no kinds")
            }
            val entryPoints = obj.requiredStringMap("entryPoints")
            return PluginManifest(
                schemaVersion = schemaVersion,
                id = id,
                name = obj.requiredString("name"),
                version = obj.requiredString("version"),
                kinds = kinds,
                packageName = obj.requiredString("package"),
                entryPoints = entryPoints,
                soongModule = obj.requiredString("soongModule"),
            )
        }

        private fun JSONObject.requiredString(key: String): String {
            if (!has(key) || isNull(key)) {
                throw PluginManifestException("missing $key")
            }
            val value = get(key)
            if (value !is String || value.isEmpty()) {
                throw PluginManifestException("$key must be a non-empty string")
            }
            return value
        }

        private fun JSONObject.requiredInt(key: String): Int {
            if (!has(key) || isNull(key)) {
                throw PluginManifestException("missing $key")
            }
            return getInt(key)
        }

        private fun JSONObject.requiredStringList(key: String): List<String> {
            if (!has(key) || isNull(key)) {
                throw PluginManifestException("missing $key")
            }
            val array = getJSONArray(key)
            return buildList {
                for (index in 0 until array.length()) {
                    val value = array.get(index)
                    if (value !is String || value.isEmpty()) {
                        throw PluginManifestException("$key entries must be non-empty strings")
                    }
                    add(value)
                }
            }
        }

        private fun JSONObject.requiredStringMap(key: String): Map<String, String> {
            if (!has(key) || isNull(key)) {
                throw PluginManifestException("missing $key")
            }
            val obj = getJSONObject(key)
            return buildMap {
                val names = obj.keys()
                while (names.hasNext()) {
                    val name = names.next()
                    val value = obj.get(name)
                    if (value !is String || value.isEmpty()) {
                        throw PluginManifestException("$key.$name must be a non-empty string")
                    }
                    put(name, value)
                }
            }
        }
    }
}
