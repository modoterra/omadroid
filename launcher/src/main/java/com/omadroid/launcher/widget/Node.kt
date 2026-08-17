package com.omadroid.launcher.widget

interface Node {
    val nodes: List<Node>

    fun style(next: GridStyle)
}

class NodeHost {
    private val items = mutableListOf<Node>()

    val nodes: List<Node>
        get() = items

    fun add(node: Node) {
        items.add(node)
    }

    fun remove(node: Node) {
        items.remove(node)
    }

    fun clear() {
        items.clear()
    }

    fun style(next: GridStyle) {
        items.forEach { it.style(next) }
    }
}
