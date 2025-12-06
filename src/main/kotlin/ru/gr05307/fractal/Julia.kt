package ru.gr05307.fractal

import ru.gr05307.math.Complex

class Julia(
    val c: Complex = Complex(-0.7, 0.27015), // стандартная «красивая» Julia
    val nMax: Int = 200,
    val r: Double = 2.0
) {
    fun iterate(z0: Complex): Float {
        var z = z0
        var i = 0
        while (i < nMax && z.absoluteValue2 <= r * r) {
            z = z * z + c
            i++
        }
        return i.toFloat() / nMax
    }
}
