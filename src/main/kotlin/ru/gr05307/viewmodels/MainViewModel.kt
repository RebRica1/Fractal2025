package ru.gr05307.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

class MainViewModel {
    var fractalImage: ImageBitmap = ImageBitmap(0, 0)
    var selectionOffset by mutableStateOf(Offset(0f, 0f))
    var selectionSize by mutableStateOf(Size(0f, 0f))
    private val plain = Plain(-2.0, 1.0, -1.0, 1.0)
    private val fractalPainter = FractalPainter(plain)
    private var mustRepaint by mutableStateOf(true)

    /** Обновление размеров окна с сохранением пропорций */
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

    /** Рисование фрактала */
    // video export
   //var isExportingVideo by mutableStateOf(false)
    //var exportingProgress by mutableStateOf(0f)

    // animation variables
    val tourKeyframes = mutableStateListOf<TourKeyframe>()
    var isTourPlaying by mutableStateOf(false)
    var currentTourFrame by mutableStateOf(0)
    var totalTourFrames by mutableStateOf(0)
    var tourProgress by mutableStateOf(0f)
    var showTourControls by mutableStateOf(false)
    private var tourJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    data class TourKeyframe(
        val id: String = UUID.randomUUID().toString(),
        val name: String = "Frame ${Date().time}",
        val xMin: Double,
        val xMax: Double,
        val yMin: Double,
        val yMax: Double,
        val timestamp: Long = System.currentTimeMillis()
    ) {
    override fun toString(): String {
            return "$name: X[$xMin, $xMax], Y[$yMin, $yMax]"
        }
    }

    init {
        addCurrentViewAsKeyframe("Initial view")
    }

    //private val tourAnimation = TourAnimation()
    //val animationState = tourAnimation.animationState

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

    /** Обновление ImageBitmap после рисования */
    fun onImageUpdate(image: ImageBitmap) {
        fractalImage = image
    }

    /** Начало выделения области */
    fun onStartSelecting(offset: Offset) {
        if (!isTourPlaying) {
            selectionOffset = offset
            selectionSize = Size(0f, 0f)
        }
    }

    /** Завершение выделения и масштабирование */
    fun onStopSelecting() {
        if (selectionSize.width == 0f || selectionSize.height == 0f) return

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
        }
    }

    /** Обновление выделяемой области */
    fun onSelecting(offset: Offset) {
        if (!isTourPlaying)
            selectionSize = Size(selectionSize.width + offset.x, selectionSize.height + offset.y)
    }

    // Tour functions
    fun addCurrentViewAsKeyframe(name: String = "Frame ${tourKeyframes.size + 1}") {
        tourKeyframes.add(
            TourKeyframe(
                name = name,
                xMin = plain.xMin,
                xMax = plain.xMax,
                yMin = plain.yMin,
                yMax = plain.yMax
            )
        )
    }

    fun removeKeyframe(id: String) {
        tourKeyframes.removeAll { it.id == id }
    }

    fun goToKeyframe(keyframe: TourKeyframe) {
        plain.xMin = keyframe.xMin
        plain.xMax = keyframe.xMax
        plain.yMin = keyframe.yMin
        plain.yMax = keyframe.yMax
        mustRepaint = true
    }

    fun updateKeyframe(keyframeId: String, newName: String? = null) {
        val index = tourKeyframes.indexOfFirst { it.id == keyframeId }
        if (index != -1) {
            val old = tourKeyframes[index]
            tourKeyframes[index] = old.copy(
                name = newName ?: old.name
            )
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
                        plain.xMin = interpolate(from.xMin, to.xMin, easedProgress)
                        plain.xMax = interpolate(from.xMax, to.xMax, easedProgress)
                        plain.yMin = interpolate(from.yMin, to.yMin, easedProgress)
                        plain.yMax = interpolate(from.yMax, to.yMax, easedProgress)

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

    fun pauseTour() {
        isTourPlaying = false
    }

    fun resumeTour() {
        if (!isTourPlaying && tourKeyframes.size >= 2) {
            isTourPlaying = true
            startTour()
        }
    }

    fun stopTour() {
        isTourPlaying = false
        tourJob?.cancel()
    }

    fun toggleTourControls() {
        showTourControls = !showTourControls
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

    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        return coroutineScope.launch(block = block)
    }
}