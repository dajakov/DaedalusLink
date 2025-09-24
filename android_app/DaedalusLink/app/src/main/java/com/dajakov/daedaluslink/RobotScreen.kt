package com.dajakov.daedaluslink

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.navigation.NavController
import co.yml.charts.axis.AxisData
import co.yml.charts.common.extensions.formatToSinglePrecision
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ControlScreen(navController: NavController) {
    BackHandler {
        webSocketManager.disconnect()
        navController.navigate("landing")
    }

    val receivedJsonData = sharedState.receivedJsonData

    @Composable
    fun ButtonElement(
        element: InterfaceData,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onPress: (String) -> Unit,
        onRelease: (String) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        val (offsetX, offsetY) = offset
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        LaunchedEffect(isPressed) {
            if (isPressed) onPress(element.pressCommand)
            else onRelease(element.pressCommand)
        }

        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = offsetX + element.position[0] * cellWidth,
                    y = offsetY + element.position[1] * cellHeight
                )
                .size(
                    width = element.size[0] * cellWidth,
                    height = element.size[1] * cellHeight
                )
        ) {
            Button(
                onClick = {},
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.surface),
                shape = RectangleShape
            ) {
                Text(element.label, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    @Composable
    fun JoystickElement(
        element: InterfaceData,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onMove: (Byte, Byte) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        val (offsetX, offsetY) = offset
        val density = LocalDensity.current

        val joystickSizeDp = element.size[0] * cellWidth
        val joystickRadiusPx = with(density) { 40.dp.toPx() }

        var offsetXInternal by remember { mutableFloatStateOf(0f) }
        var offsetYInternal by remember { mutableFloatStateOf(0f) }

        val circleColor = MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = offsetX + element.position[0] * cellWidth,
                    y = offsetY + element.position[1] * cellHeight
                )
                .size(
                    width = element.size[0] * cellWidth,
                    height = element.size[1] * cellHeight
                )
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(joystickSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                offsetXInternal = 0f
                                offsetYInternal = 0f
                                onMove(0, 0)
                            },
                            onDrag = { _, dragAmount ->
                                val maxOffsetX = size.width / 2f - joystickRadiusPx
                                val maxOffsetY = size.height / 2f - joystickRadiusPx

                                offsetXInternal = (offsetXInternal + dragAmount.x)
                                    .coerceIn(-maxOffsetX, maxOffsetX)
                                offsetYInternal = (offsetYInternal + dragAmount.y)
                                    .coerceIn(-maxOffsetY, maxOffsetY)

                                val normalizedX = ((offsetXInternal / maxOffsetX) * 127).toInt()
                                    .coerceIn(-128, 127).toByte()
                                val normalizedY = ((offsetYInternal / maxOffsetY) * 127).toInt()
                                    .coerceIn(-128, 127).toByte()

                                onMove(normalizedX, normalizedY)
                            }
                        )
                    }
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    color = circleColor,
                    radius = joystickRadiusPx,
                    center = center + Offset(offsetXInternal, offsetYInternal)
                )
            }
        }
    }

    @Composable
    fun DynamicUI(jsonString: String) {
        val config = remember { json.decodeFromString<LinkConfig>(jsonString) }

        GridLayout { gridSize, offset ->
            Box(modifier = Modifier.fillMaxSize()) {
                config.interfaceData.forEach { element ->
                    when (element.type) {
                        "button" -> ButtonElement(
                            element, gridSize, offset,
                            onPress = { cmd -> webSocketManager.sendCommand(cmd) },
                            onRelease = { cmd -> webSocketManager.sendCommand("!$cmd") }
                        )
                        "joystick" -> JoystickElement(
                            element, gridSize, offset,
                            onMove = { x, y -> webSocketManager.sendMovementCommand(x, y) }
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (receivedJsonData.isNotEmpty()) {
            DynamicUI(receivedJsonData)
        } else {
            Text("No JSON file received!")
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DebugScreen(navController: NavController, debugViewModel: DebugViewModel) {
    val debugData = debugViewModel.debugData

    BackHandler {
        navController.navigate("landing")
    }

    Scaffold (Modifier.background(MaterialTheme.colorScheme.primary)){
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Debug Charts", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (debugData.isEmpty()) {
                Text("Waiting for debug data...")
            } else {
                debugData.forEach { (key, points) ->
                    if (points.isNotEmpty()) {
                        Text(text = key.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium)
                        RpmChartScreen(rpmData = points)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RpmChartScreen(rpmData: List<Point>) {
    if (rpmData.isEmpty()) return

    val steps = 5
    val yMin = rpmData.minOf { it.y }
    val yMax = rpmData.maxOf { it.y }
    val yRange = yMax - yMin
    val safeRange = if (yRange == 0f) 1f else yRange

    val yAxisData = AxisData.Builder()
        .steps(steps)
        .backgroundColor(MaterialTheme.colorScheme.primary)
        .labelAndAxisLinePadding(20.dp)
        .labelData { i ->
            val yScale = safeRange / steps
            ((i * yScale) + yMin).formatToSinglePrecision()
        }
        .build()

    val xAxisData = AxisData.Builder()
        .axisStepSize(100.dp)
        .backgroundColor(MaterialTheme.colorScheme.primary)
        .steps(rpmData.size - 1)
        .labelData { "" }
        .labelAndAxisLinePadding(0.dp)
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = rpmData,
                    lineStyle = LineStyle(),
                    intersectionPoint = IntersectionPoint(),
                    selectionHighlightPoint = SelectionHighlightPoint(),
                    shadowUnderLine = ShadowUnderLine(),
                    selectionHighlightPopUp = SelectionHighlightPopUp()
                )
            )
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(),
        backgroundColor = MaterialTheme.colorScheme.primary
    )

    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        LineChart(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxWidth()
                .height(250.dp),
            lineChartData = lineChartData
        )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(navController: NavController) {
    BackHandler {
        navController.navigate("landing")
    }

    Scaffold (
        Modifier.background(MaterialTheme.colorScheme.primary),
        topBar = { },
    ) {
    }
}