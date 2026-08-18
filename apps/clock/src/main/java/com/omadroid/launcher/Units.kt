package com.omadroid.launcher

/** Same cell length HOME uses. GridStyle sizes text from this. */
const val UNIT_DP = 32

fun unitLengthPx(density: Float): Int = (UNIT_DP * density).toInt().coerceAtLeast(1)
