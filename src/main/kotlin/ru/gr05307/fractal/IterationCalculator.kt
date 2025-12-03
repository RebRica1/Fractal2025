package ru.gr05307.fractal

import ru.gr05307.painting.convertation.Plain
import kotlin.math.log2
import kotlin.math.max

fun calculateIterations(plain: Plain, baseIterations: Int = 200, zoomFactor: Int = 50): Int {
    val initialWidth = 3.0
    val currentWidth = plain.xMax - plain.xMin
    val zoomLevel = log2(initialWidth / currentWidth).coerceAtLeast(0.0)
    return max(baseIterations, baseIterations + (zoomLevel * zoomFactor).toInt())
}