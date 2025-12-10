import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.gr05307.julia.ResizableJuliaPanel
import ru.gr05307.math.Complex
import ru.gr05307.ui.FractalMenu
import ru.gr05307.ui.PaintPanel
import ru.gr05307.ui.SelectionPanel
import ru.gr05307.ui.TourControlPanel
import ru.gr05307.viewmodels.AppViewModel
import ru.gr05307.viewmodels.JuliaViewModel
import ru.gr05307.viewmodels.MainViewModel
val NeutralDark = Color(0xFF333333)



@Composable
@Preview
fun App() {
    val viewModel = remember { AppViewModel() }

    MaterialTheme {
        FractalApp(viewModel)
    }
}

@Composable
fun FractalApp(viewModel: AppViewModel) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.isCtrlPressed &&
                    event.key == Key.Z
                ) {
                    viewModel.mainViewModel.performUndo()
                    true // событие обработано
                } else {
                    false
                }
            }
    ) {

        Row(modifier = Modifier.fillMaxSize()) {
            MainFractalView(
                viewModel = viewModel.mainViewModel,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(7f)
            )

            JuliaSidePanel(
                viewModel = viewModel.juliaViewModel,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(3f)
            )
        }
    }
}

@Composable
fun MainFractalView(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
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
            onClick = { pos -> viewModel.onPointClicked(pos.x, pos.y) },
            onDragStart = viewModel::onStartSelecting,
            onDragEnd = viewModel::onStopSelecting,
            onDrag = viewModel::onSelecting,
            onPan = viewModel::onPanning,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            FractalMenu(
                viewModel = viewModel,
                modifier = Modifier.align(Alignment.TopStart),
                isShowingTourControls = viewModel.showTourControls
            )
        }

        // Tour control panel
        if (viewModel.showTourControls) {
            TourControlPanel(
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 100.dp, end = 16.dp) // сдвигаем вниз на 100.dp
                    .width(300.dp)
            )
        }


        // Floating action button для показа/скрытия тур-контролей
        FloatingActionButton(
            onClick = { viewModel.toggleTourControls() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            backgroundColor = NeutralDark // Используем цвет гамбургера
        ) {
            Icon(
                if (viewModel.showTourControls) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = "Tour controls",
                tint = Color.White // Белая иконка
            )
        }
    }
}


@Composable
fun JuliaSidePanel(
    viewModel: JuliaViewModel,
    modifier: Modifier = Modifier
) {
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

val NaturalDark = Color(0xFF333333)

@Composable
fun PanelHeader(
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NaturalDark) // <- Natural Black
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

fun main(): Unit = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Фрактал - 2025 (гр. 05-307)"
    ) {
        App()
    }
}
