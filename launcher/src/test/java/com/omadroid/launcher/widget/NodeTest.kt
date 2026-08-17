package com.omadroid.launcher.widget

import com.omadroid.theme.ThemeColors
import org.junit.Assert.assertEquals
import org.junit.Test

class NodeTest {
    @Test
    fun styleWalksTheTree() {
        val leaf = CountingNode()
        val branch = CountingNode(mutableListOf(leaf))
        val root = CountingNode(mutableListOf(branch))
        root.style(stubStyle())
        assertEquals(1, root.hits)
        assertEquals(1, branch.hits)
        assertEquals(1, leaf.hits)
    }

    private class CountingNode(
        private val kids: MutableList<Node> = mutableListOf(),
    ) : Node {
        var hits: Int = 0
        override val nodes: List<Node>
            get() = kids

        override fun style(next: GridStyle) {
            hits += 1
            kids.forEach { it.style(next) }
        }
    }

    private fun stubStyle(): GridStyle =
        GridStyle(
            unitPx = 32,
            colors =
                ThemeColors.parse(
                    "test",
                    """
                    background = "#000000"
                    foreground = "#ffffff"
                    lighter_background = "#111111"
                    muted = "#888888"
                    accent = "#00ff00"
                    red = "#ff0000"
                    yellow = "#ffff00"
                    green = "#00ff00"
                    cyan = "#00ffff"
                    blue = "#0000ff"
                    magenta = "#ff00ff"
                    """.trimIndent(),
                ),
        )
}
