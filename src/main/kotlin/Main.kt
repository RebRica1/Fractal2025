import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.gr05307.ui.PaintPanel
import ru.gr05307.ui.SelectionPanel
import ru.gr05307.viewmodels.MainViewModel

// Добавления от Артёма
import androidx.compose.runtime.*
import ru.gr05307.julia.JuliaPanel
import ru.gr05307.math.Complex
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
// Конец

// Полностью добавленный код от Артема
class JuliaViewModelWrapper(
    private val baseViewModel: MainViewModel,
    private val onJuliaPointSelected: (Complex) -> Unit
) {
    // Делегируем все методы базовому ViewModel
    val fractalImage get() = baseViewModel.fractalImage
    val selectionOffset get() = baseViewModel.selectionOffset
    val selectionSize get() = baseViewModel.selectionSize

    fun paint(scope: androidx.compose.ui.graphics.drawscope.DrawScope) = baseViewModel.paint(scope)
    fun onImageUpdate(image: androidx.compose.ui.graphics.ImageBitmap) = baseViewModel.onImageUpdate(image)
    fun onStartSelecting(offset: androidx.compose.ui.geometry.Offset) = baseViewModel.onStartSelecting(offset)
    fun onSelecting(offset: androidx.compose.ui.geometry.Offset) = baseViewModel.onSelecting(offset)
    fun onStopSelecting() = baseViewModel.onStopSelecting()
    fun canUndo() = baseViewModel.canUndo()
    fun performUndo() = baseViewModel.performUndo()
    fun onPanning(offset: androidx.compose.ui.geometry.Offset) = baseViewModel.onPanning(offset)
    fun saveFractalToJpg(path: String) = baseViewModel.saveFractalToJpg(path)

    // Переопределяем обработку кликов
    fun onPointClicked(x: Float, y: Float) {
        val re = ru.gr05307.painting.convertation.Converter.xScr2Crt(x, baseViewModel.plain)
        val im = ru.gr05307.painting.convertation.Converter.yScr2Crt(y, baseViewModel.plain)
        onJuliaPointSelected(Complex(re, im))
    }
}
// Конец блока

// Весь App переехал в main()

fun main(): Unit = application {

    Window(
        onCloseRequest = ::exitApplication,
        title = "Фрактал - 2025 (гр. 05-307)"
    ) {
        var currentJuliaPoint by remember { mutableStateOf<Complex?>(null) }
        var showJuliaPanel by remember { mutableStateOf(false) }

        val baseViewModel = remember { MainViewModel() }

        val wrappedViewModel = remember {
            JuliaViewModelWrapper(baseViewModel) { complex ->
                currentJuliaPoint = complex
                showJuliaPanel = true // Автоматически показываем панель при выборе точки
            }
        }

        // Переезд Appa:
        MaterialTheme {
            Row(modifier = Modifier.fillMaxSize()) {
                // Основная область с фракталом
                Box(modifier = Modifier.weight(1f)) {
                    PaintPanel(
                        modifier = Modifier.fillMaxSize(),
                        onImageUpdate = { image -> wrappedViewModel.onImageUpdate(image) },
                        onPaint = { scope -> wrappedViewModel.paint(scope) }
                    )
                    SelectionPanel(
                        wrappedViewModel.selectionOffset,
                        wrappedViewModel.selectionSize,
                        Modifier.fillMaxSize(),
                        onClick = { pos -> wrappedViewModel.onPointClicked(pos.x, pos.y) },
                        onDragStart = wrappedViewModel::onStartSelecting,
                        onDragEnd = wrappedViewModel::onStopSelecting,
                        onDrag = wrappedViewModel::onSelecting,
                        onPan = wrappedViewModel::onPanning,
                    )

                    // Кнопка Назад в правом верхнем углу
                    Button(
                        onClick = { wrappedViewModel.performUndo() },
                        enabled = wrappedViewModel.canUndo(),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Text("Назад")
                    }
                }

                // Анимированная боковая панель Жюлиа
                AnimatedVisibility(
                    visible = showJuliaPanel && currentJuliaPoint != null,
                    enter = slideInHorizontally(animationSpec = tween(300)) { it },
                    exit = slideOutHorizontally(animationSpec = tween(300)) { it },
                    modifier = Modifier.width(350.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(350.dp)
                            .background(Color.White)
                            .border(1.dp, Color.Gray)
                    ) {
                        // Заголовок панели
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.primary)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Множество Жюлиа",
                                color = Color.White,
                                style = MaterialTheme.typography.h6
                            )
                            IconButton(
                                onClick = {
                                    showJuliaPanel = false
                                    currentJuliaPoint = null
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Закрыть",
                                    tint = Color.White
                                )
                            }
                        }

                        // Информация о точке
                        if (currentJuliaPoint != null) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                elevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Выбранная точка:",
                                        style = MaterialTheme.typography.subtitle1
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "c = ${"%.6f".format(currentJuliaPoint!!.re)} + ${"%.6f".format(currentJuliaPoint!!.im)}i",
                                        style = MaterialTheme.typography.body1
                                    )
                                }
                            }
                        }

                        // Панель с изображением Жюлиа
                        if (currentJuliaPoint != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                JuliaPanel(
                                    c = currentJuliaPoint,
                                    onClose = {
                                        showJuliaPanel = false
                                        currentJuliaPoint = null
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


