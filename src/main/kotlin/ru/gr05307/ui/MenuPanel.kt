package ru.gr05307.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector

import ru.gr05307.viewmodels.MainViewModel
import ru.gr05307.serialization.FractalSerializer
import ru.gr05307.painting.*

import java.awt.FileDialog
import java.awt.Frame
import java.io.File


val NeutralDark = Color(0xFF333333)
val LightButton = Color(0xFFF0F0F0)

@Composable
fun FractalMenu(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isShowingTourControls: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val menuButtonPadding = 16.dp

    Box(modifier = modifier.fillMaxSize()) {

        Button(
            onClick = { menuOpen = !menuOpen },
            enabled = !isShowingTourControls,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(menuButtonPadding),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = NeutralDark,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.elevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = if (menuOpen) Icons.Filled.Close else Icons.Filled.Menu,
                contentDescription = if (menuOpen) "Закрыть меню" else "Открыть меню"
            )
        }

        Box(modifier = modifier.fillMaxSize()) {

            Button(
                onClick = {
                    menuOpen = !menuOpen
                    viewModel.isMenuOpened = !viewModel.isMenuOpened
                },
                enabled = !isShowingTourControls,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = NeutralDark,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (menuOpen) Icons.Filled.Close else Icons.Filled.Menu,
                    contentDescription = null
                )
            }

            Button(
                onClick = { viewModel.performUndo() },
                enabled = viewModel.canUndo(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = NeutralDark,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Назад")
            }

            AnimatedVisibility(
                visible = menuOpen,
                enter = slideInHorizontally(tween(300)) { -it },
                exit = slideOutHorizontally(tween(300)) { -it },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 70.dp, start = 10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight(),
                    color = Color.White.copy(alpha = 0.87f),
                    elevation = 12.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        Text(
                            "Настройки",
                            style = MaterialTheme.typography.h5,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentWidth(Alignment.CenterHorizontally)
                                .padding(top = 16.dp)
                        )

                        Divider(color = Color.LightGray)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Switch(
                                checked = viewModel.showJulia,
                                onCheckedChange = { value -> viewModel.setJuliaEnabled(value) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Отображать множества Жюлиа", style = MaterialTheme.typography.body2)
                        }


                        Divider(color = Color.LightGray)
                        Text("Фрактал:", style = MaterialTheme.typography.h6)
                        FractalSelectionButtons(viewModel)
                        Divider(color = Color.LightGray)
                        Text("Цветовая схема:", style = MaterialTheme.typography.h6)
                        ColorSelectionButtons(viewModel)
                        Divider(color = Color.LightGray)
                        Text("Сохранение и Загрузка:", style = MaterialTheme.typography.h6)
                        SavingLoadingButtons(viewModel)
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun FractalSelectionButtons(viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuButton(onClick = { viewModel.switchToMandelbrot() }, text = "Mandelbrot")
        MenuButton(onClick = { viewModel.switchToJulia() }, text = "Julia")
        MenuButton(onClick = { viewModel.switchToNewton() }, text = "Newton")
    }
}

@Composable
fun ColorSelectionButtons(viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuButton(onClick = { viewModel.switchToRainbow() }, text = "Rainbow")
        MenuButton(onClick = { viewModel.switchToGrayscale() }, text = "Grayscale")
        MenuButton(onClick = { viewModel.switchToIce() }, text = "Ice")
        MenuButton(onClick = { viewModel.switchToNewtonColor() }, text = "Newton Color")
    }
}

@Composable
fun SavingLoadingButtons(viewModel: MainViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MenuButton(
            onClick = {
                val dialog = FileDialog(null as Frame?, "Загрузить фрактал", FileDialog.LOAD)
                dialog.isVisible = true
                dialog.file?.let { fileName ->
                    val file = File(dialog.directory, fileName)
                    if (!file.exists()) return@let
                    val serializer = FractalSerializer()
                    val data = serializer.load(file.absolutePath)
                    viewModel.plain.xMin = data.plain.xMin
                    viewModel.plain.xMax = data.plain.xMax
                    viewModel.plain.yMin = data.plain.yMin
                    viewModel.plain.yMax = data.plain.yMax
                    when (data.fractalType) {
                        "mandelbrot" -> viewModel.switchToMandelbrot()
                        "julia" -> viewModel.switchToJulia()
                        "newton" -> viewModel.switchToNewton()
                    }
                    when (data.colorType) {
                        "rainbow" -> viewModel.switchToRainbow()
                        "grayscale" -> viewModel.switchToGrayscale()
                        "ice" -> viewModel.switchToIce()
                        "newtonColor" -> viewModel.switchToNewtonColor()
                    }
                }
            },
            text = "Загрузить JSON",
        )

        MenuButton(
            onClick = {
                val dialog = FileDialog(null as Frame?, "Сохранить фрактал", FileDialog.SAVE)
                dialog.file = "fractal.json"
                dialog.isVisible = true
                val directory = dialog.directory
                val name = dialog.file
                if (directory != null && name != null) {
                    val file = File(directory, name)
                    val serializer = FractalSerializer()
                    val frac = when (viewModel.currentFractalFuncPublic) {
                        mandelbrotFunc -> "mandelbrot"
                        juliaFunc -> "julia"
                        newtonFunc -> "newton"
                        else -> "mandelbrot"
                    }
                    val col = when (viewModel.currentColorFuncPublic) {
                        rainbow -> "rainbow"
                        grayscale -> "grayscale"
                        iceGradient -> "ice"
                        newtonColor -> "newtonColor"
                        else -> "rainbow"
                    }
                    serializer.save(
                        plain = viewModel.plain,
                        fractalType = frac,
                        colorType = col,
                        filePath = file.absolutePath
                    )
                }
            },
            text = "Сохранить JSON",
        )

        MenuButton(
            onClick = {
                val dialog = FileDialog(null as Frame?, "Сохранить изображение", FileDialog.SAVE)
                dialog.file = "fractal.jpg"
                dialog.isVisible = true
                dialog.file?.let { name ->
                    val file = File(dialog.directory, name)
                    viewModel.saveFractalToJpg(file.absolutePath)
                }
            },
            text = "Сохранить JPG",
        )
    }
}

@Composable
fun MenuButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = LightButton, // Светло-серый фон кнопки
            contentColor = Color.Black
        ),
        elevation = ButtonDefaults.elevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = NeutralDark // Иконки делаем темно-серыми
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(text, style = MaterialTheme.typography.button)
        }
    }
}

@Composable
fun TourControlPanel(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 8.dp,
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.4f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок Animation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Face,
                    contentDescription = "Tour",
                    tint = NeutralDark // черная иконка
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Animation",
                    style = MaterialTheme.typography.h6,
                    color = NeutralDark // черный текст
                )
            }

            Divider(modifier = Modifier.padding(8.dp))

            // Секция ключевых кадров
            Text("Keyframes:", style = MaterialTheme.typography.subtitle1)
            if (viewModel.tourKeyframes.isEmpty()) {
                Text(
                    "No keyframes",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .height(150.dp)
                        .padding(vertical = 4.dp)
                ) {
                    items(viewModel.tourKeyframes.size) { index ->
                        val keyframe = viewModel.tourKeyframes[index]
                        keyframeItem(
                            keyframe = keyframe,
                            onGoto = { viewModel.goToKeyframe(keyframe) },
                            onDelete = { viewModel.removeKeyframe(keyframe.id) },
                            isPlaying = viewModel.isTourPlaying
                        )
                    }
                }
            }

            // Кнопка добавления ключевого кадра
            Button(
                onClick = { viewModel.addCurrentViewAsKeyframe() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isTourPlaying,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = LightButton, // светлый фон
                    contentColor = NeutralDark // черный текст и иконка
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = NeutralDark // черная иконка
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Add current view",
                    color = NeutralDark // черный текст
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Управление туром
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Button(
                    onClick = { viewModel.startTour() },
                    enabled = viewModel.tourKeyframes.size > 1 && !viewModel.isTourPlaying,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Start")
                }

                Button(
                    onClick = { viewModel.stopTour() },
                    enabled = viewModel.isTourPlaying,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
                ) {
                    StopIcon(modifier = Modifier, 18.dp)
                }
            }

            // Индикатор прогресса
            if (viewModel.isTourPlaying) {
                LinearProgressIndicator(
                    progress = viewModel.tourProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colors.primary
                )
                Text(
                    "Frame: ${viewModel.currentTourFrame}/${viewModel.totalTourFrames}",
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}
