package com.omadroid.launcher

import android.content.Context

data class CommandItem(
    val id: String,
    val title: String,
    val module: String,
    val icon: String? = null,
    val hint: String? = null,
    val keywords: List<String> = emptyList(),
)

data class CommandScope(
    val context: Context,
    val layout: LauncherLayout,
    val workspaces: Workspaces,
)

fun collectCommands(
    modules: List<ModuleSpec>,
    scope: CommandScope,
): List<CommandItem> = modules.flatMap { it.commands(scope) }

fun fuzzyScore(text: String, query: String): Int? {
    if (query.isEmpty()) {
        return 0
    }
    val hay = text.lowercase()
    val needle = query.lowercase().filter { !it.isWhitespace() }
    if (needle.isEmpty()) {
        return 0
    }
    var at = 0
    var last = -2
    var score = 0
    for (ch in needle) {
        val found = hay.indexOf(ch, at)
        if (found < 0) {
            return null
        }
        score += 2
        if (found == 0 || !hay[found - 1].isLetterOrDigit()) {
            score += 6
        }
        if (found == last + 1) {
            score += 8
        }
        last = found
        at = found + 1
    }
    score -= (hay.length - needle.length).coerceAtMost(16)
    return score
}

fun fuzzySearch(items: List<CommandItem>, query: String): List<CommandItem> {
    if (query.isBlank()) {
        return items
    }
    return items
        .mapNotNull { item ->
            val fields = listOf(item.title) + item.keywords + listOfNotNull(item.hint)
            val best = fields.mapNotNull { fuzzyScore(it, query) }.maxOrNull()
            best?.let { item to it }
        }
        .sortedWith(compareByDescending<Pair<CommandItem, Int>> { it.second }.thenBy { it.first.title })
        .map { it.first }
}
