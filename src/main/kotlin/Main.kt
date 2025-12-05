import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.gr05307.ui.PaintPanel
// Добавка Артёма
import ru.gr05307.ui.SelectionPanel
import ru.gr05307.viewmodels.AppViewModel
import ru.gr05307.julia.ResizableJuliaPanel
import ru.gr05307.math.Complex
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import ru.gr05307.viewmodels.MainViewModel
import ru.gr05307.viewmodels.JuliaViewModel
// Конец добавки

@Composable
@Preview
fun App() {
    val viewModel = remember { AppViewModel() }

    MaterialTheme {
        // Артем: Переделал тему. Отдельно рендер фрактала и отдельно рендер окна Юли
        // Каждому выделяем свой размер
        FractalApp(viewModel)
    }
}

@Composable
fun FractalApp(viewModel: AppViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Артем: Фрактал сам занимает 70%
        MainFractalView(
            viewModel = viewModel.mainViewModel,
            modifier = Modifier
                .fillMaxHeight()
                .weight(7f)
        )

        // Артем: Моя панель Юли, занимает 30%
        JuliaSidePanel(
            viewModel = viewModel.juliaViewModel,
            modifier = Modifier
                .fillMaxHeight()
                .weight(3f)
        )
    }
}

@Composable
fun MainFractalView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    // Артем: Здесь полный перенос старого содержания темы после апдейта Эмиля
    Box(modifier = modifier) {
        PaintPanel(
            modifier = Modifier.fillMaxSize(),
            onImageUpdate = { image -> viewModel.onImageUpdate(image) },
            onPaint = { scope -> viewModel.paint(scope) }
        )
        SelectionPanel(
            viewModel.selectionOffset,
            viewModel.selectionSize,
            Modifier.fillMaxSize(),
            // Артем: Добавлен детект клика
            onClick = { pos -> viewModel.onPointClicked(pos.x, pos.y) },
            onDragStart = viewModel::onStartSelecting,
            onDragEnd = viewModel::onStopSelecting,
            onDrag = viewModel::onSelecting,
            onPan = viewModel::onPanning,
        )

        Button(
            onClick = { viewModel.performUndo() },
            enabled = viewModel.canUndo(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Text("Назад")
        }
    }
}

// Артем: Моя часть окна Юли
@Composable
fun JuliaSidePanel(
    viewModel: JuliaViewModel,
    modifier: Modifier = Modifier
) {
    // Получаем значения из ViewModel - теперь они корректные State объекты
    val currentJuliaPoint = viewModel.currentJuliaPoint
    val showJuliaPanel = viewModel.showJuliaPanel

    AnimatedVisibility(
        visible = showJuliaPanel && currentJuliaPoint != null,
        enter = slideInHorizontally(animationSpec = tween(300)) { it },
        exit = slideOutHorizontally(animationSpec = tween(300)) { it },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color.White)
                .border(1.dp, Color.Gray)
        ) {
            PanelHeader(
                onClose = { viewModel.closeJuliaPanel() }
            )

            if (currentJuliaPoint != null) {
                PointInfoCard(currentJuliaPoint)
            }

            if (currentJuliaPoint != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    ResizableJuliaPanel(
                        c = currentJuliaPoint,
                        onClose = { viewModel.closeJuliaPanel() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// Артем: Ниже Viewшная часть отрисовки блоков информации, окна и точки
@Composable
fun PanelHeader(
    onClose: () -> Unit
) {
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
            onClick = onClose
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Закрыть",
                tint = Color.White
            )
        }
    }
}

@Composable
fun PointInfoCard(c: Complex) {
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
                text = "c = ${"%.6f".format(c.re)} + ${"%.6f".format(c.im)}i",
                style = MaterialTheme.typography.body1
            )
        }
    }
}
// Артем: конец View

fun main(): Unit = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Фрактал - 2025 (гр. 05-307)"
    ) {
        App()
    }
}