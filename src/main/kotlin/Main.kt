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
import ru.gr05307.painting.rainbow
import ru.gr05307.painting.grayscale


@Composable
@Preview
fun App(viewModel: MainViewModel = MainViewModel()) {

    MaterialTheme {
        Column {
            Button(onClick = { viewModel.switchToRainbow() }) { Text("Rainbow") }
            Button(onClick = { viewModel.switchToGrayscale() }) { Text("Grayscale") }
            //Button(onClick = { viewModel.switchToFire() }) { Text("Fire") }
            Button(onClick = { viewModel.switchToIce() }) { Text("Ice") }
            Button(onClick = { viewModel.switchToNewtonColor()}) { Text("NewtonColor") }
            Button(onClick = { viewModel.switchToMandelbrot() }) { Text("Mandelbrot") }
            Button(onClick = { viewModel.switchToJulia() }) { Text("Julia") }
            Button(onClick = { viewModel.switchToNewton() }) { Text("Newton") }
            Box {
                PaintPanel(
                    Modifier.fillMaxSize(),
                    onImageUpdate = {
                        viewModel.onImageUpdate(it)
                    }
                ) {
                    viewModel.paint(it)
                }
                SelectionPanel(
                    viewModel.selectionOffset,
                    viewModel.selectionSize,
                    Modifier.fillMaxSize(),
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
