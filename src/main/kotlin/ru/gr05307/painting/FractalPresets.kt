package ru.gr05307.painting

import androidx.compose.ui.graphics.Color
import ru.gr05307.math.Complex
import ru.gr05307.fractal.Mandelbrot
import ru.gr05307.fractal.Julia
import ru.gr05307.fractal.NewtonFractal
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.pow


val mandelbrotFunc: FractalFunction = { z, nMax ->
    val m = Mandelbrot(nMax = nMax)
    m.isInSet(z).coerceIn(0f, 1f)
}
val juliaFunc: FractalFunction = { z, nMax ->
    val j = Julia(nMax = nMax)
    j.iterate(z).coerceIn(0f, 1f)
}
val newtonFunc: FractalFunction = { z, nMax ->
    val n = NewtonFractal(nMax)
    n.iterate(z).coerceIn(0f, 1f)
}


// --- Цветовые схемы ---
val grayscale: ColorFunction = { p ->
    val v = p.coerceIn(0f, 1f)
    Color(v, v, v)
}

val rainbow: ColorFunction = { p ->
    val pp = p.coerceIn(0f, 1f)
    if (pp == 1f) Color.Black else Color(
        red = abs(cos(7 * pp)),
        green = abs(sin(12 * (1f - pp))),
        blue = abs(sin(4 * pp) * cos(4 * (1 - pp)))
    )
}
/*val fireGradient: ColorFunction = { p ->
    val pp = p.coerceIn(0f, 1f)
    Color(
        red = pp,
        green = (pp * 0.5f).coerceIn(0f, 1f),
        blue = 0f
    )
}*/

val iceGradient: ColorFunction = { p ->
    val pp = p.coerceIn(0f, 1f)
    Color(
        red = (pp * 0.5f).coerceIn(0f, 1f),
        green = pp,
        blue = 1f
    )
}
val newtonColor: ColorFunction = { p ->
    val pp = p.coerceIn(0f, 1f)

    // Мерцание для звёзд
    val twinkle = abs(sin(15f * pp) * cos(9f * pp))

    // ЖЁЛТЫЙ НА ПЕТЛЯХ - делаем нелинейное усиление
    // Чем ближе pp к 1, тем более жёлтый цвет
    val yellowOnLoops = when {
        pp > 0.9f -> (pp - 0.9f) * 10f  // Максимально жёлтые петли
        pp > 0.7f -> (pp - 0.7f) * 5f   // Сильно жёлтые
        pp > 0.5f -> (pp - 0.5f) * 2f   // Умеренно жёлтые
        else -> 0f
    }

    // Затемняем фон сильнее
    val darkBackground = (1f - pp).pow(3f) * 0.15f

    Color(
        red = (0.8f * pp + 0.4f * twinkle + yellowOnLoops).coerceIn(0f, 1f),
        green = (0.75f * pp + 0.3f * twinkle + yellowOnLoops * 0.85f).coerceIn(0f, 1f),
        blue = (0.3f * pp + 0.2f * twinkle + darkBackground).coerceIn(0f, 1f)
    )
}




