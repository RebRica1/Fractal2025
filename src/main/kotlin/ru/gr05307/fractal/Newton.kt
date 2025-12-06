package ru.gr05307.fractal
import ru.gr05307.math.Complex

class NewtonFractal(val nMax: Int = 50, val tol: Double = 1e-6) {
    fun iterate(z0: Complex): Float {
        var z = z0
        var i = 0
        while (i < nMax) {
            val dz = (z*z*z - Complex(1.0, 0.0)) / (Complex(3.0,0.0)*z*z)
            z = z - dz
            if (dz.absoluteValue2 < tol) break
            i++
        }
        return i.toFloat() / nMax
    }
}


