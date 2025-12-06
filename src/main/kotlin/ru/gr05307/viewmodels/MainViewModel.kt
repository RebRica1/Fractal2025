package ru.gr05307.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.gr05307.painting.FractalPainter
import ru.gr05307.painting.convertation.Converter
import ru.gr05307.painting.convertation.Plain
import ru.gr05307.ExportFractal.FractalExporter
import ru.gr05307.painting.*
import ru.gr05307.painting.FractalFunction
import ru.gr05307.painting.ColorFunction



class MainViewModel{
import ru.gr05307.rollback.UndoManager

class MainViewModel {
    var fractalImage: ImageBitmap = ImageBitmap(0, 0)
    var selectionOffset by mutableStateOf(Offset(0f, 0f))
    var selectionSize by mutableStateOf(Size(0f, 0f))
    private val plain = Plain(-2.0,1.0,-1.0,1.0)
    //private val fractalPainter = FractalPainter(plain)
    private var mustRepaint by mutableStateOf(true)
    private val undoManager = UndoManager(maxSize = 100)

    private var currentFractalFunc: FractalFunction = mandelbrotFunc
    private var currentColorFunc: ColorFunction = rainbow

    private val fractalPainter = FractalPainter(plain, currentFractalFunc, currentColorFunc)

    // Обновление размеров окна с сохранением пропорций
    private fun updatePlainSize(newWidth: Float, newHeight: Float) {
        plain.width = newWidth
        plain.height = newHeight

        val aspect = plain.aspectRatio
        val newAspect = newWidth / newHeight

        if (newAspect > aspect) {
            // Ширина лишняя, подгоняем по высоте
            val centerX = (plain.xMin + plain.xMax) / 2.0
            val halfWidth = (plain.yMax - plain.yMin) * newAspect / 2.0
            plain.xMin = centerX - halfWidth
            plain.xMax = centerX + halfWidth
        } else {
            // Высота лишняя, подгоняем по ширине
            val centerY = (plain.yMin + plain.yMax) / 2.0
            val halfHeight = (plain.xMax - plain.xMin) / newAspect / 2.0
            plain.yMin = centerY - halfHeight
            plain.yMax = centerY + halfHeight
        }
    }

    // Рисование фрактала
    fun paint(scope: DrawScope) = runBlocking {
        updatePlainSize(scope.size.width, scope.size.height)

        if (mustRepaint
            || fractalImage.width != plain.width.toInt()
            || fractalImage.height != plain.height.toInt()
        ) {
            launch(Dispatchers.Default) {
                fractalPainter.paint(scope)
            }
        } else {
            scope.drawImage(fractalImage)
        }
        mustRepaint = false
    }

    // Обновление ImageBitmap после рисования
    fun onImageUpdate(image: ImageBitmap) {
        fractalImage = image
    }

    // Начало выделения области
    fun onStartSelecting(offset: Offset) {
        selectionOffset = offset
        selectionSize = Size(0f, 0f)
    }

    // Обновление выделяемой области
    fun onSelecting(offset: Offset) {
        selectionSize = Size(selectionSize.width + offset.x, selectionSize.height + offset.y)
    }

    // Завершение выделения и масштабирование
    fun onStopSelecting() {
        if (selectionSize.width == 0f || selectionSize.height == 0f) return

        undoManager.save(plain.copy())

        val aspect = plain.aspectRatio
        var selWidth = selectionSize.width
        var selHeight = selectionSize.height

        // Сохраняем пропорции, центрируя выделение
        val selAspect = selWidth / selHeight
        if (selAspect > aspect) {
            // ширина больше, подгоняем высоту
            selHeight = (selWidth / aspect).toFloat()  // Приведение к Float
        } else {
            // высота больше, подгоняем ширину
            selWidth = (selHeight * aspect).toFloat()  // Приведение к Float
        }

        // Рассчитываем новые границы фрактала
        val xMin = Converter.xScr2Crt(selectionOffset.x, plain)
        val xMax = Converter.xScr2Crt(selectionOffset.x + selWidth, plain)
        val yMin = Converter.yScr2Crt(selectionOffset.y + selHeight, plain)
        val yMax = Converter.yScr2Crt(selectionOffset.y, plain)

        plain.xMin = xMin
        plain.xMax = xMax
        plain.yMin = yMin
        plain.yMax = yMax

        selectionSize = Size(0f, 0f)
        mustRepaint = true
    }

    fun canUndo(): Boolean = undoManager.canUndo()

    fun performUndo() {
        val prevState = undoManager.undo()
        if (prevState != null) {
            plain.xMin = prevState.xMin
            plain.xMax = prevState.xMax
            plain.yMin = prevState.yMin
            plain.yMax = prevState.yMax
            selectionSize = Size(0f, 0f)
            mustRepaint = true
        }
    }

    fun onPanning(offset: Offset){
        // Конвертируем пиксельное смещение в смещение в координатах комплексной плоскости
        val dx = -offset.x / plain.xDen
        val dy = offset.y / plain.yDen

        plain.xMin += dx
        plain.xMax += dx
        plain.yMin += dy
        plain.yMax += dy

        mustRepaint = true
    }

    fun saveFractalToJpg(path: String) {
        val exporter = FractalExporter(plain)
        exporter.saveJPG(path)
    }

    // --- методы переключения функций и цвета ---
    fun setFractalFunction(f: FractalFunction) {
        fractalPainter.fractalFunc = f
        mustRepaint = true
    }

    fun setColorFunction(c: ColorFunction) {
        fractalPainter.colorFunc = c
        mustRepaint = true
    }

    fun switchToRainbow() = setColorFunction(rainbow)
    fun switchToGrayscale() = setColorFunction(grayscale)
    //fun switchToFire() = setColorFunction(fireGradient)
    fun switchToIce() = setColorFunction(iceGradient)
    fun switchToNewtonColor() = setColorFunction(newtonColor)
    fun switchToMandelbrot() = setFractalFunction(mandelbrotFunc)
    fun switchToJulia() = setFractalFunction(juliaFunc)
    fun switchToNewton() = setFractalFunction(newtonFunc)

}
