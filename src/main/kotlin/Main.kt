import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.gr05307.ui.PaintPanel
import ru.gr05307.ui.SelectionPanel
import ru.gr05307.viewmodels.MainViewModel

@Composable
@Preview
fun App(viewModel: MainViewModel= MainViewModel()) {
    MaterialTheme {
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
                viewModel::onStartSelecting,
                viewModel::onStopSelecting,
                viewModel::onSelecting,
            )
        }
    }
}
// что то
fun main(): Unit = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Фрактал - 2025 (гр. 05-307)"
    ) {
        App()
    }
}