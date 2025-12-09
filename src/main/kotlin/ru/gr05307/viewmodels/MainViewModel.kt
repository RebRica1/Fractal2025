package ru.gr05307.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.gr05307.painting.FractalPainter
import ru.gr05307.painting.convertation.Converter
import ru.gr05307.painting.convertation.Plain
import java.util.Date
import java.util.UUID
import kotlin.math.pow
import ru.gr05307.ExportFractal.FractalExporter
import ru.gr05307.painting.*
import ru.gr05307.painting.FractalFunction
import ru.gr05307.painting.ColorFunction
import ru.gr05307.math.Complex
import ru.gr05307.painting.*
import ru.gr05307.rollback.UndoManager

class MainViewModel {
    var showJulia by mutableStateOf(true)
    fun setJuliaEnabled(value: Boolean) {
        showJulia = value
    }
    var fractalImage: ImageBitmap = ImageBitmap(0, 0)
    var selectionOffset by mutableStateOf(Offset(0f, 0f))
    var selectionSize by mutableStateOf(Size(0f, 0f))
    val plain = Plain(-2.0,1.0,-1.0,1.0)
    //private val fractalPainter = FractalPainter(plain)
    private var mustRepaint by mutableStateOf(true)
    private val undoManager = UndoManager(maxSize = 100)

    private var currentFractalFunc: FractalFunction = mandelbrotFunc
    var currentFractalType: String = "mandelbrot"
        private set
    var currentColorFunc: ColorFunction = rainbow
        private set
    var currentColorType: String = "rainbow"
        private set

    val currentFractalFuncPublic: FractalFunction
        get() = currentFractalFunc

    val currentColorFuncPublic: ColorFunction
        get() = currentColorFunc



    private val fractalPainter = FractalPainter(plain, currentFractalFunc, currentColorFunc)

    var shouldCloseJuliaPanel: ((Boolean) -> Unit)? = null

    var onJuliaPointSelected: ((Complex) -> Unit)? = null

    private var _shouldCloseJuliaPanel by mutableStateOf(false)

    // animation variables
    val tourKeyframes = mutableStateListOf<TourKeyframe>()
    var isTourPlaying by mutableStateOf(false)
    var currentTourFrame by mutableStateOf(0)
    var totalTourFrames by mutableStateOf(0)
    var tourProgress by mutableStateOf(0f)
    var showTourControls by mutableStateOf(false)
    var isMenuOpened by mutableStateOf(false)
    private var tourJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())


    // track the highest frame number used
    private var frameCounter = 1

    data class TourKeyframe(
        val id: String = UUID.randomUUID().toString(),
        val name: String = "Frame ${Date().time}",
        val xMin: Double,
        val xMax: Double,
        val yMin: Double,
        val yMax: Double,
        val frameNumber: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        override fun toString(): String {
            return "$name (Frame #$frameNumber): X[$xMin, $xMax], Y[$yMin, $yMax]"
        }
    }

    init {
        addCurrentViewAsKeyframe("Initial view")
        undoManager.save(plain.copy())

    }


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
        if (!isTourPlaying) {
            selectionOffset = offset
            selectionSize = Size(0f, 0f)
        }
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
        if (!isTourPlaying && selectionSize.width > 10f && selectionSize.height > 10f) {
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
            _shouldCloseJuliaPanel = true
            shouldCloseJuliaPanel?.invoke(true)
        }
    }

    // Обновление выделяемой области
    fun onSelecting(offset: Offset) {
        if (!isTourPlaying)
            selectionSize = Size(selectionSize.width + offset.x, selectionSize.height + offset.y)
    }

    // Tour functions
    //Add current view as a keyframe with sequential numbering
    fun addCurrentViewAsKeyframe(name: String? = null) {
        // Get the next available frame number
        val nextFrameNumber = getNextFrameNumber()
        val frameName = name ?: "Frame #$nextFrameNumber"

        tourKeyframes.add(
            TourKeyframe(
                name = frameName,
                xMin = plain.xMin,
                xMax = plain.xMax,
                yMin = plain.yMin,
                yMax = plain.yMax,
                frameNumber = nextFrameNumber
            )
        )

        // Update the frame counter
        frameCounter = maxOf(frameCounter, nextFrameNumber + 1)

        // Reorder frames by frame number to maintain order
        reorderFramesByNumber()
    }


    private fun getNextFrameNumber(): Int {
        if (tourKeyframes.isEmpty()) {
            return 1
        }

        // Find gaps in frame numbers
        val existingNumbers = tourKeyframes.map { it.frameNumber }.sorted()

        // Check for gaps starting from 1
        for (i in 1..existingNumbers.size + 1) {
            if (!existingNumbers.contains(i)) {
                return i
            }
        }

        // If no gaps, return next sequential number
        return (existingNumbers.maxOrNull() ?: 0) + 1
    }

    //Reorder frames by their frame number to maintain proper order
    private fun reorderFramesByNumber() {
        if (tourKeyframes.size <= 1) return

        // Sort by frame number
        val sortedFrames = tourKeyframes.sortedBy { it.frameNumber }

        // Clear and add back in sorted order
        tourKeyframes.clear()
        tourKeyframes.addAll(sortedFrames)
    }

    fun removeKeyframe(id: String) {
        val index = tourKeyframes.indexOfFirst { it.id == id }
        if (index != -1) {
            val removedFrame = tourKeyframes[index]
            tourKeyframes.removeAt(index)

            // After removal, we could optionally renumber frames
            renumberAllFrames()

        }
    }

    fun goToKeyframe(keyframe: TourKeyframe) {
        plain.xMin = keyframe.xMin
        plain.xMax = keyframe.xMax
        plain.yMin = keyframe.yMin
        plain.yMax = keyframe.yMax
        mustRepaint = true
    }

    //Renumber all frames sequentially starting from 1
    fun renumberAllFrames() {
        if (tourKeyframes.isEmpty()) return

        val updatedFrames = mutableListOf<TourKeyframe>()

        // Sort frames by current frame number first
        val sortedFrames = tourKeyframes.sortedBy { it.frameNumber }

        // Renumber sequentially
        for ((index, frame) in sortedFrames.withIndex()) {
            val newFrameNumber = index + 1
            updatedFrames.add(
                frame.copy(
                    name = if (frame.name.startsWith("Frame #")) {
                        "Frame #$newFrameNumber"
                    } else {
                        "${frame.name} (#$newFrameNumber)"
                    },
                    frameNumber = newFrameNumber
                )
            )
        }

        // Update the list
        tourKeyframes.clear()
        tourKeyframes.addAll(updatedFrames)

        // Update frame counter
        frameCounter = updatedFrames.size + 1
    }

    //Move a frame up in the list and renumber if needed
    fun moveKeyframeUp(index: Int) {
        if (index > 0) {
            // Swap frames
            val temp = tourKeyframes[index]
            tourKeyframes[index] = tourKeyframes[index - 1]
            tourKeyframes[index - 1] = temp

            // Optionally renumber after moving
            renumberAllFrames()
        }
    }

    //Move a frame down in the list and renumber if needed
    fun moveKeyframeDown(index: Int) {
        if (index < tourKeyframes.size - 1) {
            // Swap frames
            val temp = tourKeyframes[index]
            tourKeyframes[index] = tourKeyframes[index + 1]
            tourKeyframes[index + 1] = temp

            // Optionally renumber after moving
            renumberAllFrames()
        }
    }

    fun startTour() {
        if (tourKeyframes.size < 2) return

        stopTour() // stop any existing tour

        isTourPlaying = true
        currentTourFrame = 0
        tourProgress = 0f

        // calculate the total frames (i.e 3 seconds per keyframe at 60fps)
        val fps = 60
        val secondPerKeyframe = 3.0
        totalTourFrames = ((tourKeyframes.size - 1) * secondPerKeyframe * fps).toInt()

        tourJob = coroutineScope.launch {
            try {
                for (frame in 0 until totalTourFrames) {
                    if (!isTourPlaying) break

                    val time = frame.toDouble() / fps
                    val keyframeIndex = (time / secondPerKeyframe).toInt()
                    val segmentProgress = (time % secondPerKeyframe) / secondPerKeyframe

                    if (keyframeIndex < tourKeyframes.size - 1) {
                        val from = tourKeyframes[keyframeIndex]
                        val to = tourKeyframes[keyframeIndex + 1]

                        val easedProgress = easeInOutCubic(segmentProgress)

                        // update plane coordinates directly
                        interpolateView(from, to, easedProgress)

                        currentTourFrame = frame
                        tourProgress = frame.toFloat() /totalTourFrames
                        mustRepaint = true
                    }
                    delay((1000 / fps).toLong())
                }

            } catch (e: CancellationException) {
            } finally {
                isTourPlaying = false

                if (tourKeyframes.isNotEmpty()) {
                    val lastFrame = tourKeyframes.last()
                    plain.xMin = lastFrame.xMin
                    plain.xMax = lastFrame.xMax
                    plain.yMin = lastFrame.yMin
                    plain.yMax = lastFrame.yMax
                    mustRepaint = true
                }

            }
        }
    }

    fun stopTour() {
        isTourPlaying = false
        tourJob?.cancel()
    }

    fun toggleTourControls() {
        showTourControls = !showTourControls && !isMenuOpened
    }

    private fun interpolateView(from: TourKeyframe, to: TourKeyframe, progress: Double) {
        plain.xMin = interpolate(from.xMin, to.xMin, progress)
        plain.xMax = interpolate(from.xMax, to.xMax, progress)
        plain.yMin = interpolate(from.yMin, to.yMin, progress)
        plain.yMax = interpolate(from.yMax, to.yMax, progress)
    }

    private fun interpolate(start: Double, end: Double, progress: Double): Double {
        return start + (end - start) * progress
    }

    private fun easeInOutCubic(t: Double): Double {
        return if (t < 0.5) 4 * t * t * t else 1 - (-2 * t + 2).pow(3) / 2
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
            _shouldCloseJuliaPanel = true
            shouldCloseJuliaPanel?.invoke(true)
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
        _shouldCloseJuliaPanel = true
        shouldCloseJuliaPanel?.invoke(true)
    }

    fun saveFractalToJpg(path: String) {
        val exporter = FractalExporter(
            plain,
            currentFractalFunc,
            currentColorFunc
        )
        exporter.saveJPG(path)
    }

    fun resetCloseJuliaFlag() {
        _shouldCloseJuliaPanel = false
    }

    // Артем: Обработка клика по точке
    fun onPointClicked(x: Float, y: Float) {
        val re = Converter.xScr2Crt(x, plain)
        val im = Converter.yScr2Crt(y, plain)
        val complex = Complex(re, im)
        onJuliaPointSelected?.invoke(complex)
    }

    // --- методы переключения функций и цвета ---
    fun setFractalFunction(f: FractalFunction, type: String) {
        currentFractalFunc = f
        currentFractalType = type
        fractalPainter.fractalFunc = f
        mustRepaint = true
    }

    fun setColorFunction(c: ColorFunction, name: String) {
        currentColorType = name
        // изменил баг из-за которого цвет не менялся
        currentColorFunc = c
        fractalPainter.colorFunc = c
        mustRepaint = true
    }
    fun switchToRainbow() = setColorFunction(rainbow,"rainbow")
    fun switchToGrayscale() = setColorFunction(grayscale, "grayscale")
    //fun switchToFire() = setColorFunction(fireGradient)
    fun switchToIce() = setColorFunction(iceGradient, "ice")
    fun switchToNewtonColor() = setColorFunction(newtonColor, "newtonColor")
    fun switchToMandelbrot() = setFractalFunction(mandelbrotFunc, "mandelbrot")
    fun switchToJulia() = setFractalFunction(juliaFunc, "julia")
    fun switchToNewton() = setFractalFunction(newtonFunc, "newton")
    val currentPlain: Plain
        get() = plain.copy()
    fun setPlain(newPlain: Plain) {
        undoManager.save(plain.copy())   // сохранить в историю
        plain.xMin = newPlain.xMin
        plain.xMax = newPlain.xMax
        plain.yMin = newPlain.yMin
        plain.yMax = newPlain.yMax
        plain.width = newPlain.width
        plain.height = newPlain.height
        mustRepaint = true
    }
}

data class PlainState(
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double
)