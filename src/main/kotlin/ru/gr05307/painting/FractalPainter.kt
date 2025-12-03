package ru.gr05307.painting
import ru.gr05307.fractal.calculateIterations
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.coroutineScope
import ru.gr05307.fractal.Mandelbrot
import ru.gr05307.math.Complex
import ru.gr05307.painting.convertation.Converter
import ru.gr05307.painting.convertation.Plain
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

class FractalPainter(private val plain: Plain): Painter {

    //private val fractalCoroutine = CoroutineScope(Dispatchers.Default)

    private fun getColor(probability: Float) = if (probability == 1f)
        Color.Black
    else Color(
        red = cos(7 * probability).absoluteValue,
        green = sin(12 * (1f - probability)).absoluteValue,
        blue = (sin(4 * probability) * cos(4 * (1 - probability))).absoluteValue
    )

    override suspend fun paint(scope: DrawScope) {
        plain.width = scope.size.width
        plain.height = scope.size.height
        val nMax = calculateIterations(plain)
        val m = Mandelbrot(nMax = nMax)
        for (iX in 0..<plain.width.toInt()) {
            coroutineScope {
                val x = iX.toFloat()
                repeat(plain.height.toInt()) { iY ->
                    val y = iY.toFloat()
                    scope.drawRect(
                        getColor(
                            m.isInSet(
                                Complex(
                                    Converter.xScr2Crt(x, plain),
                                    Converter.yScr2Crt(y, plain),
                                )
                            )
                        ),
                        Offset(x, y),
                        Size(1f, 1f),
                    )
                }
            }
        }
    }

}