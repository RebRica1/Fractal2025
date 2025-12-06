package ru.gr05307.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.launch


@Composable
fun PaintPanel(
    modifier: Modifier = Modifier,
    onImageUpdate: (ImageBitmap) -> Unit = {},
    onPaint: (DrawScope) -> Unit = {} // оставляем обычную лямбду, paint внутри уже runBlocking
) {
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()

    Canvas(modifier.drawWithContent {
        graphicsLayer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(graphicsLayer)

        scope.launch {
            onImageUpdate(graphicsLayer.toImageBitmap())
        }
    }) {
        onPaint(this)
    }
}


@Composable
fun SelectionPanel(
    offset: Offset,
    size: Size,
    modifier: Modifier = Modifier,
    onDragStart: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDrag: (Offset) -> Unit = {},
    onPanStart: (Offset) -> Unit = {},
    onPanEnd: () -> Unit = {},
    onPan: (Offset) -> Unit = {},
){
    var dragButton by remember { mutableStateOf<PointerButton?>(null) }

    Canvas(modifier = modifier.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()

                when (event.type) {
                    PointerEventType.Press -> {
                        val buttons = event.buttons
                        dragButton = when {
                            buttons.isPrimaryPressed -> PointerButton.Primary
                            buttons.isSecondaryPressed -> PointerButton.Secondary
                            else -> null
                        }

                        val position = event.changes.first().position
                        when (dragButton) {
                            PointerButton.Primary -> onDragStart(position)
                            PointerButton.Secondary -> onPanStart(position)
                            else -> {}
                        }
                    }

                    PointerEventType.Move -> {
                        if (dragButton != null) {
                            val change = event.changes.first()
                            val dragAmount = change.position - change.previousPosition

                            when (dragButton) {
                                PointerButton.Primary -> onDrag(dragAmount)
                                PointerButton.Secondary -> onPan(dragAmount)
                                else -> {}
                            }
                            change.consume()
                        }
                    }

                    PointerEventType.Release -> {
                        if (dragButton != null) {
                            when (dragButton) {
                                PointerButton.Primary -> onDragEnd()
                                PointerButton.Secondary -> onPanEnd()
                                else -> {}
                            }
                            dragButton = null
                        }
                    }
                }
            }
        }
    }){
        this.drawRect(Color.Blue, offset, size, alpha = 0.2f)
    }
}