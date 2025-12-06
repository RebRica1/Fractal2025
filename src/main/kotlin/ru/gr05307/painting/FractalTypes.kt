package ru.gr05307.painting

import ru.gr05307.math.Complex
import androidx.compose.ui.graphics.Color

typealias FractalFunction = (Complex, Int) -> Float
typealias ColorFunction = (Float) -> Color
