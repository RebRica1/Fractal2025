package ru.gr05307.painting

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.coroutineScope
import ru.gr05307.painting.convertation.Converter
import ru.gr05307.painting.convertation.Plain
import ru.gr05307.math.Complex
import ru.gr05307.fractal.calculateIterations

// FractalPainter теперь принимает лямбды для фрактала и цвета
class FractalPainter(
    private val plain: Plain,
    var fractalFunc: FractalFunction,
    var colorFunc: ColorFunction,
) : Painter {

    override suspend fun paint(scope: DrawScope) {
        // обновляем размеры плоскости
        plain.width = scope.size.width
        plain.height = scope.size.height

        val nMax = calculateIterations(plain)
        val w = plain.width.toInt()
        val h = plain.height.toInt()

        // простой пиксельный рендер (как у вас было) — заменяем getColor вызовом colorFunc
        for (iX in 0 until w) {
            coroutineScope {
                val x = iX.toFloat()
                repeat(h) { iY ->
                    val y = iY.toFloat()
                    val c = Complex(
                        Converter.xScr2Crt(x, plain),
                        Converter.yScr2Crt(y, plain)
                    )
                    val value = fractalFunc(c, nMax).coerceIn(0f, 1f)
                    val color = colorFunc(value)
                    scope.drawRect(
                        color,
                        Offset(x, y),
                        Size(1f, 1f)
                    )
                }
            }
        }
    }
}
