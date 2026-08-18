package com.omadroid.compose

enum class Direction {
    Horizontal,
    Vertical,
}

data class Slot(
    val grow: Float = 0f,
    val units: Int? = null,
    val square: Boolean = false,
) {
    companion object {
        fun grow(weight: Float = 1f) = Slot(grow = weight)

        fun units(count: Int) = Slot(units = count)

        val square = Slot(square = true)

        val fit = Slot()
    }
}
