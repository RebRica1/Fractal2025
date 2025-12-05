package ru.gr05307.julia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.gr05307.math.Complex

@Composable
fun ResizableJuliaPanel(
    c: Complex,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var imageState by remember { mutableStateOf<List<List<Color>>?>(null) }
    var panelSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(c, panelSize) {
        val width = panelSize.width.toInt()
        val height = panelSize.height.toInt()

        if (width > 0 && height > 0) {
            imageState = withContext(Dispatchers.Default) {
                renderResizableJulia(c, width, height)
            }
        }
    }

    Box(
        modifier = modifier
            .border(2.dp, Color.Gray)
            .background(Color.White)
    ) {
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

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 30.dp)
        ) {
            val newSize = Size(size.width, size.height)
            if (newSize != panelSize) {
                panelSize = newSize
            }

            val img = imageState ?: return@Canvas

            val width = size.width.toInt()
            val height = size.height.toInt()

            val safeWidth = minOf(width, img.size)
            val safeHeight = if (safeWidth > 0) minOf(height, img[0].size) else 0

            if (safeWidth > 0 && safeHeight > 0) {
                for (x in 0 until safeWidth) {
                    for (y in 0 until safeHeight) {
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
}

private fun renderResizableJulia(c: Complex, width: Int, height: Int): List<List<Color>> {
    val safeWidth = maxOf(1, width)
    val safeHeight = maxOf(1, height)

    val totalPixels = safeWidth * safeHeight
    val maxIter = when {
        totalPixels > 500000 -> 50
        totalPixels > 200000 -> 100
        totalPixels > 50000 -> 150
        else -> 200
    }

    val result = List(safeWidth) { MutableList(safeHeight) { Color.Black } }

    val scale = 2.0
    for (x in 0 until safeWidth) {
        val re = (x - safeWidth / 2.0) / (safeWidth / 2.0) * scale
        for (y in 0 until safeHeight) {
            val im = (y - safeHeight / 2.0) / (safeHeight / 2.0) * scale
            var z = Complex(re, im)
            var iter = 0
            while (iter < maxIter && z.absoluteValue2 < 4) {
                z = z * z
                z = z + c
                iter++
            }
            val t = iter / maxIter.toFloat()
            result[x][y] = if (iter == maxIter) Color.Black
            else Color.hsv(t * 360f, 0.8f, 0.9f)
        }
    }
    return result
}