package com.omadroid.launcher

const val UNIT_DP = 32

fun unitLengthPx(density: Float): Int = (UNIT_DP * density).toInt().coerceAtLeast(1)
