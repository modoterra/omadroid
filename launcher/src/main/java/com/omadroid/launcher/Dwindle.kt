package com.omadroid.launcher

typealias ClientId = String

data class DwindleRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    val area: Long
        get() = width.toLong() * height.toLong()
}

enum class DwindleAxis {
    Vertical,
    Horizontal,
    ;

    fun next(): DwindleAxis =
        when (this) {
            Vertical -> Horizontal
            Horizontal -> Vertical
        }
}

sealed class DwindleNode {
    data class Leaf(val id: ClientId) : DwindleNode()

    data class Split(
        val axis: DwindleAxis,
        val first: DwindleNode,
        val second: DwindleNode,
    ) : DwindleNode()
}

/** Hyprland-style binary split: each new client splits the last leaf. First axis is vertical. */
fun dwindleTree(ids: List<ClientId>): DwindleNode? {
    if (ids.isEmpty()) {
        return null
    }
    var node: DwindleNode = DwindleNode.Leaf(ids.first())
    var axis = DwindleAxis.Vertical
    for (id in ids.drop(1)) {
        node = splitLastLeaf(node, id, axis)
        axis = axis.next()
    }
    return node
}

fun dwindleRects(width: Int, height: Int, tree: DwindleNode?): List<Pair<ClientId, DwindleRect>> {
    if (tree == null || width <= 0 || height <= 0) {
        return emptyList()
    }
    val out = ArrayList<Pair<ClientId, DwindleRect>>()
    walkRects(tree, 0, 0, width, height, out)
    return out
}

private fun splitLastLeaf(node: DwindleNode, id: ClientId, axis: DwindleAxis): DwindleNode =
    when (node) {
        is DwindleNode.Leaf -> DwindleNode.Split(axis, node, DwindleNode.Leaf(id))
        is DwindleNode.Split -> node.copy(second = splitLastLeaf(node.second, id, axis))
    }

private fun walkRects(
    node: DwindleNode,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    out: MutableList<Pair<ClientId, DwindleRect>>,
) {
    when (node) {
        is DwindleNode.Leaf -> out.add(node.id to DwindleRect(x, y, width, height))
        is DwindleNode.Split ->
            when (node.axis) {
                DwindleAxis.Vertical -> {
                    val first = width / 2
                    walkRects(node.first, x, y, first, height, out)
                    walkRects(node.second, x + first, y, width - first, height, out)
                }
                DwindleAxis.Horizontal -> {
                    val first = height / 2
                    walkRects(node.first, x, y, width, first, out)
                    walkRects(node.second, x, y + first, width, height - first, out)
                }
            }
    }
}
