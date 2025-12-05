package ru.gr05307.julia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.gr05307.math.Complex

@Composable
fun JuliaPanel(
    c: Complex?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Если нет точки, не показываем панель
    if (c == null) return

    var imageState by remember { mutableStateOf<List<List<Color>>?>(null) }
    var panelSize by remember { mutableStateOf(IntSize(300, 200)) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(c, panelSize) {
        scope.launch(Dispatchers.Default) {
            imageState = renderJulia(c, panelSize.width, panelSize.height)
        }
    }

    Box(
        modifier = modifier
            .size(320.dp, 240.dp) // Фиксированный размер
            .border(2.dp, Color.Gray)
            .background(Color.White)
    ) {
        // Заголовок с кнопкой закрытия
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Жюлиа: c = ${"%.3f".format(c.re)} + ${"%.3f".format(c.im)}i",
                style = MaterialTheme.typography.caption
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.Red
                )
            }
        }

        // Область с изображением Жюлиа
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp) // Отступ для заголовка
        ) {
            panelSize = IntSize(size.width.toInt(), size.height.toInt())
            val img = imageState ?: return@Canvas
            val w = size.width.toInt()
            val h = size.height.toInt()

            for (x in 0 until w) {
                for (y in 0 until h) {
                    drawRect(
                        color = img[x][y],
                        topLeft = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat()),
                        size = androidx.compose.ui.geometry.Size(1f, 1f)
                    )
                }
            }
        }
    }
}

// Оптимизированная версия рендеринга для маленького размера
private fun renderJulia(c: Complex, w: Int, h: Int): List<List<Color>> {
    val maxIter = 200 // Меньше итераций для быстрого отображения
    val result = List(w) { MutableList(h) { Color.Black } }

    val scale = 2.0 // Больший масштаб для лучшей детализации в маленьком окне
    for (xi in 0 until w) {
        val re = (xi - w/2.0) / (w/2.0) * scale
        for (yi in 0 until h) {
            val im = (yi - h/2.0) / (h/2.0) * scale
            var z = Complex(re, im)
            var iter = 0
            while (iter < maxIter && z.absoluteValue2 < 4) {
                z = z * z
                z = z + c
                iter++
            }
            val t = iter / maxIter.toFloat()
            result[xi][yi] = if (iter == maxIter) Color.Black
            else Color.hsv(t * 360f, 0.8f, 0.9f)
        }
    }
    return result
}