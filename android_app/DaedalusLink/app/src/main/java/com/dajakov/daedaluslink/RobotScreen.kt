package com.dajakov.daedaluslink

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import kotlin.math.roundToInt

//import co.yml.charts.axis.AxisData
//import co.yml.charts.common.extensions.formatToSinglePrecision
//import co.yml.charts.common.model.Point
//import co.yml.charts.ui.linechart.LineChart
//import co.yml.charts.ui.linechart.model.GridLines
//import co.yml.charts.ui.linechart.model.IntersectionPoint
//import co.yml.charts.ui.linechart.model.Line
//import co.yml.charts.ui.linechart.model.LineChartData
//import co.yml.charts.ui.linechart.model.LinePlotData
//import co.yml.charts.ui.linechart.model.LineStyle
//import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
//import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
//import co.yml.charts.ui.linechart.model.ShadowUnderLine

object UIElementLogic {
    suspend fun PointerInputScope.handleMoveDrag(
        gridSize: Pair<Dp, Dp>,
        getCurrentX: () -> Int,
        getCurrentY: () -> Int,
        onEditingChange: (Boolean) -> Unit,
        onPositionChange: (Int, Int) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        var moveAccumulatorX = 0f
        var moveAccumulatorY = 0f

        detectDragGestures(
            onDragStart = { onEditingChange(true) },
            onDragEnd = { onEditingChange(false) },
            onDragCancel = { onEditingChange(false) },
            onDrag = { change, dragAmount ->
                change.consume()
                moveAccumulatorX += dragAmount.x
                moveAccumulatorY += dragAmount.y

                val cellWidthPx = cellWidth.toPx()
                val cellHeightPx = cellHeight.toPx()

                val dxCells = (moveAccumulatorX / cellWidthPx).toInt()
                val dyCells = (moveAccumulatorY / cellHeightPx).toInt()

                if (dxCells != 0 || dyCells != 0) {
                    onPositionChange(getCurrentX() + dxCells, getCurrentY() + dyCells)
                    moveAccumulatorX -= dxCells * cellWidthPx
                    moveAccumulatorY -= dyCells * cellHeightPx
                }
            }
        )
    }

    suspend fun PointerInputScope.handleResizeDrag(
        gridSize: Pair<Dp, Dp>,
        getCurrentWidth: () -> Int,    // Changed to lambda
        getCurrentHeight: () -> Int,   // Changed to lambda
        onEditingChange: (Boolean) -> Unit,
        onResize: (Int, Int) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        var resizeAccumulatorX = 0f
        var resizeAccumulatorY = 0f

        detectDragGestures(
            onDragStart = { onEditingChange(true) },
            onDragEnd = { onEditingChange(false) },
            onDragCancel = { onEditingChange(false) },
            onDrag = { change, dragAmount ->
                change.consume() // CRITICAL: Prevents parent from moving
                resizeAccumulatorX += dragAmount.x
                resizeAccumulatorY += dragAmount.y

                val cellWidthPx = cellWidth.toPx()
                val cellHeightPx = cellHeight.toPx()

                val dxCells = (resizeAccumulatorX / cellWidthPx).toInt()
                val dyCells = (resizeAccumulatorY / cellHeightPx).toInt()

                if (dxCells != 0 || dyCells != 0) {
                    onResize(
                        (getCurrentWidth() + dxCells).coerceAtLeast(1),
                        (getCurrentHeight() + dyCells).coerceAtLeast(1)
                    )
                    // Subtract only the consumed distance
                    if (dxCells != 0) resizeAccumulatorX -= dxCells * cellWidthPx
                    if (dyCells != 0) resizeAccumulatorY -= dyCells * cellHeightPx
                }
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ControlScreen(navController: NavController, webSocketMngr: WebSocketManager) { // Added webSocketMngr parameter
    BackHandler {
        webSocketMngr.disconnect() // Use passed webSocketMngr
        navController.navigate("landing")
    }

    val receivedJsonData = sharedState.receivedJsonData

    @Composable
    fun ConnectionStatusIndicator(isConnected: Boolean, modifier: Modifier = Modifier) {
        val (indicatorColor, statusText) = if (isConnected) {
            Pair(Color.Green, "Connected")
        } else {
            Pair(Color.Red, "Disconnected")
        }
        val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(indicatorColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = statusText,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }

    @Composable
    fun PacketLossIndicator(percentage: Float, modifier: Modifier = Modifier) {
        val lossText = "Loss: ${percentage.toInt()}%"
        val textColor = if (percentage > 30f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)

        Text(
            text = lossText,
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }

    data class InterfaceElementState(
        val type: String,
        val label: String,
        val command: String,
        var position: IntArray,
        var size: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InterfaceElementState

            if (type != other.type) return false
            if (label != other.label) return false
            if (command != other.command) return false
            if (!position.contentEquals(other.position)) return false
            if (!size.contentEquals(other.size)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + label.hashCode()
            result = 31 * result + command.hashCode()
            result = 31 * result + position.contentHashCode()
            result = 31 * result + size.contentHashCode()
            return result
        }
    }

    @Composable
    fun BoxScope.ResizeHandle(
        gridSize: Pair<Dp, Dp>,
        currentWidth: Int,
        currentHeight: Int,
        onEditingChange: (Boolean) -> Unit,
        onResize: (Int, Int) -> Unit
    ) {
        // Wrap current values in lambdas so handleResizeDrag always sees the latest
        val getW = rememberUpdatedState(currentWidth)
        val getH = rememberUpdatedState(currentHeight)

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                .pointerInput(gridSize) { // Key ONLY by gridSize
                    UIElementLogic.run {
                        handleResizeDrag(
                            gridSize = gridSize,
                            getCurrentWidth = { getW.value },
                            getCurrentHeight = { getH.value },
                            onEditingChange = onEditingChange,
                            onResize = onResize
                        )
                    }
                }
        )
    }

    @Composable
    fun ButtonElement(
        element: InterfaceElementState,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onPress: (String) -> Unit,
        onRelease: (String) -> Unit,
        onResize: (Int, Int) -> Unit,
        onPositionChange: (Int, Int) -> Unit,
        isEditMode: Boolean
    ) {
        val (cellWidth, cellHeight) = gridSize
        val currentWidth by rememberUpdatedState(element.size[0])
        val currentHeight by rememberUpdatedState(element.size[1])
        val currentX by rememberUpdatedState(element.position[0])
        val currentY by rememberUpdatedState(element.position[1])

        var isEditing by remember { mutableStateOf(false) }
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        LaunchedEffect(isPressed) {
            if (!isEditMode) {
                if (isPressed) onPress(element.command)
                else onRelease(element.command)
            }
        }

        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = offset.first + currentX * cellWidth + 1.dp,
                    y = offset.second + currentY * cellHeight + 1.dp
                )
                .size(currentWidth * cellWidth - 2.dp, currentHeight * cellHeight - 2.dp)
                .then(if (isEditing) Modifier.border(2.dp, Color.White, RectangleShape) else Modifier)
                // KEY FIX: Only key by gridSize and isEditMode.
                // DO NOT key by currentX/Y or the drag will reset every 1-cell move.
                .pointerInput(gridSize, isEditMode) {
                    if (!isEditMode) return@pointerInput
                    UIElementLogic.run {
                        handleMoveDrag(
                            gridSize = gridSize,
                            getCurrentX = { currentX },
                            getCurrentY = { currentY },
                            onEditingChange = { isEditing = it },
                            onPositionChange = onPositionChange
                        )
                    }
                }
        ) {
            Button(
                onClick = {},
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.surface),
                enabled = !isEditMode
            ) {
                Text(element.label, color = MaterialTheme.colorScheme.onSurface)
            }

            if (isEditMode) {
                ResizeHandle(gridSize, currentWidth, currentHeight, { isEditing = it }, onResize)
            }
        }
    }

    @Composable
    fun JoystickElement(
        element: InterfaceElementState,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onMove: (String, Byte, Byte) -> Unit,
        onResize: (Int, Int) -> Unit,
        onPositionChange: (Int, Int) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        val (offsetX, offsetY) = offset
        val density = LocalDensity.current

        val joystickSizeDp = element.size[0] * cellWidth
        val joystickRadiusPx = with(density) { 40.dp.toPx() }

        var offsetXInternal by remember { mutableFloatStateOf(0f) }
        var offsetYInternal by remember { mutableFloatStateOf(0f) }

        val currentWidth by rememberUpdatedState(element.size[0])
        val currentHeight by rememberUpdatedState(element.size[1])
        val currentX by rememberUpdatedState(element.position[0])
        val currentY by rememberUpdatedState(element.position[1])

        var resizeAccumulatorX by remember { mutableFloatStateOf(0f) }
        var resizeAccumulatorY by remember { mutableFloatStateOf(0f) }

        var moveAccumulatorX by remember { mutableFloatStateOf(0f) }
        var moveAccumulatorY by remember { mutableFloatStateOf(0f) }

        var isEditing by remember { mutableStateOf(false) }

        val circleColor = MaterialTheme.colorScheme.onSurface
        val resizeHandleSize = 18.dp

        val isEditMode: Boolean

        isEditMode = true

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
                .background(MaterialTheme.colorScheme.surface)
                .then(
                    if (isEditing) Modifier.border(2.dp, Color.White, RoundedCornerShape(20.dp))
                    else Modifier
                )
                .pointerInput(gridSize) {
                    if (!isEditMode) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            isEditing = true
                            moveAccumulatorX = 0f
                            moveAccumulatorY = 0f
                        },
                        onDragEnd = { isEditing = false },
                        onDragCancel = { isEditing = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            moveAccumulatorX += dragAmount.x
                            moveAccumulatorY += dragAmount.y

                            val cellWidthPx = cellWidth.toPx()
                            val cellHeightPx = cellHeight.toPx()

                            val dxCells = (moveAccumulatorX / cellWidthPx).toInt()
                            val dyCells = (moveAccumulatorY / cellHeightPx).toInt()

                            if (dxCells != 0 || dyCells != 0) {
                                onPositionChange(currentX + dxCells, currentY + dyCells)
                                if (dxCells != 0) moveAccumulatorX -= dxCells * cellWidthPx
                                if (dyCells != 0) moveAccumulatorY -= dyCells * cellHeightPx
                            }
                        }
                    )
                }
        ) {

            /* ──────────────── JOYSTICK ──────────────── */

            if(!isEditMode) {
                Canvas(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(joystickSizeDp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    offsetXInternal = 0f
                                    offsetYInternal = 0f
                                    onMove(element.command, 0, 0)
                                },
                                onDrag = { _, dragAmount ->
                                    val maxOffsetX = size.width / 2f - joystickRadiusPx
                                    val maxOffsetY = size.height / 2f - joystickRadiusPx

                                    offsetXInternal = (offsetXInternal + dragAmount.x)
                                        .coerceIn(-maxOffsetX, maxOffsetX)
                                    offsetYInternal = (offsetYInternal + dragAmount.y)
                                        .coerceIn(-maxOffsetY, maxOffsetY)

                                    val normalizedX = ((offsetXInternal / maxOffsetX) * 127)
                                        .toInt().coerceIn(-128, 127).toByte()
                                    val normalizedY = ((offsetYInternal / maxOffsetY) * 127)
                                        .toInt().coerceIn(-128, 127).toByte()

                                    onMove(element.command, normalizedX, normalizedY)
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
            /* ──────────────── RESIZE HANDLE ──────────────── */

            else {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(resizeHandleSize)
                        .background(MaterialTheme.colorScheme.onPrimary, shape = CircleShape)
                        .pointerInput(gridSize) {
                            detectDragGestures(
                                onDragStart = {
                                    isEditing = true
                                    resizeAccumulatorX = 0f
                                    resizeAccumulatorY = 0f
                                },
                                onDragEnd = { isEditing = false },
                                onDragCancel = { isEditing = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    resizeAccumulatorX += dragAmount.x
                                    resizeAccumulatorY += dragAmount.y

                                    val cellWidthPx = cellWidth.toPx()
                                    val cellHeightPx = cellHeight.toPx()

                                    val dxCells = (resizeAccumulatorX / cellWidthPx).toInt()
                                    val dyCells = (resizeAccumulatorY / cellHeightPx).toInt()

                                    if (dxCells != 0 || dyCells != 0) {
                                        onResize(
                                            (currentWidth + dxCells).coerceAtLeast(1),
                                            (currentHeight + dyCells).coerceAtLeast(1)
                                        )
                                        if (dxCells != 0) resizeAccumulatorX -= dxCells * cellWidthPx
                                        if (dyCells != 0) resizeAccumulatorY -= dyCells * cellHeightPx
                                    }
                                }
                            )
                        }
                )
            }
        }
    }

    @Composable
    fun SliderElement(
        element: InterfaceElementState,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onValueChange: (String, Byte) -> Unit,
        onResize: (Int, Int) -> Unit,
        onPositionChange: (Int, Int) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        val (offsetX, offsetY) = offset

        val currentWidth by rememberUpdatedState(element.size[0])
        val currentHeight by rememberUpdatedState(element.size[1])
        val currentX by rememberUpdatedState(element.position[0])
        val currentY by rememberUpdatedState(element.position[1])

        var sliderPositionNormalized by remember { mutableFloatStateOf(0f) }
        var isEditing by remember { mutableStateOf(false) }

        // Accumulators
        var moveAccumulatorX by remember { mutableFloatStateOf(0f) }
        var moveAccumulatorY by remember { mutableFloatStateOf(0f) }
        var resizeAccumulatorX by remember { mutableFloatStateOf(0f) }
        var resizeAccumulatorY by remember { mutableFloatStateOf(0f) }

        val isEditMode = true // This should be controlled by your global state

        val trackColor = MaterialTheme.colorScheme.surface
        val thumbColor = MaterialTheme.colorScheme.onSurface
        val labelColor = MaterialTheme.colorScheme.onSurface

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
                .background(MaterialTheme.colorScheme.primary)
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (isEditing) Modifier.border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .pointerInput(gridSize) {
                    if (!isEditMode) {
                        // NORMAL SLIDER LOGIC
                        detectDragGestures { change, _ ->
                            val isHorizontal = size.width >= size.height
                            val thumbRadiusVisual =
                                if (isHorizontal) size.height * 0.4f else size.width * 0.4f

                            val newPositionNormalized = if (isHorizontal) {
                                val trackActualWidth = size.width - 2 * thumbRadiusVisual
                                if (trackActualWidth <= 0) 0.5f
                                else (change.position.x - thumbRadiusVisual).div(trackActualWidth)
                                    .coerceIn(0f, 1f)
                            } else {
                                val trackActualHeight = size.height - 2 * thumbRadiusVisual
                                if (trackActualHeight <= 0) 0.5f
                                else (1 - (change.position.y - thumbRadiusVisual).div(
                                    trackActualHeight
                                )).coerceIn(0f, 1f)
                            }
                            sliderPositionNormalized = newPositionNormalized
                            val byteValue =
                                (sliderPositionNormalized * 255f - 128f).toInt().coerceIn(-128, 127)
                                    .toByte()
                            onValueChange(element.command, byteValue)
                            change.consume()
                        }
                    } else {
                        // EDIT MODE: MOVE LOGIC
                        detectDragGestures(
                            onDragStart = {
                                isEditing = true
                                moveAccumulatorX = 0f
                                moveAccumulatorY = 0f
                            },
                            onDragEnd = { isEditing = false },
                            onDragCancel = { isEditing = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                moveAccumulatorX += dragAmount.x
                                moveAccumulatorY += dragAmount.y

                                val cellWidthPx = cellWidth.toPx()
                                val cellHeightPx = cellHeight.toPx()

                                val dxCells = (moveAccumulatorX / cellWidthPx).toInt()
                                val dyCells = (moveAccumulatorY / cellHeightPx).toInt()

                                if (dxCells != 0 || dyCells != 0) {
                                    onPositionChange(currentX + dxCells, currentY + dyCells)
                                    if (dxCells != 0) moveAccumulatorX -= dxCells * cellWidthPx
                                    if (dyCells != 0) moveAccumulatorY -= dyCells * cellHeightPx
                                }
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Slider Drawing Logic (Same as before)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val isHorizontal = size.width >= size.height
                val trackThickness = if (isHorizontal) size.height * 0.25f else size.width * 0.25f
                val thumbRadius = if (isHorizontal) size.height * 0.4f else size.width * 0.4f
                val trackCornerRadiusValue = trackThickness / 2f

                if (isHorizontal) {
                    val trackActualWidth = size.width - 2 * thumbRadius
                    if (trackActualWidth > 0) {
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(thumbRadius, (size.height - trackThickness) / 2f),
                            size = androidx.compose.ui.geometry.Size(trackActualWidth, trackThickness),
                            cornerRadius = CornerRadius(trackCornerRadiusValue, trackCornerRadiusValue)
                        )
                        val thumbCenterX = thumbRadius + (sliderPositionNormalized * trackActualWidth)
                        drawCircle(color = thumbColor, radius = thumbRadius, center = Offset(thumbCenterX, size.height / 2f))
                    }
                } else {
                    val trackActualHeight = size.height - 2 * thumbRadius
                    if (trackActualHeight > 0) {
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset((size.width - trackThickness) / 2f, thumbRadius),
                            size = androidx.compose.ui.geometry.Size(trackThickness, trackActualHeight),
                            cornerRadius = CornerRadius(trackCornerRadiusValue, trackCornerRadiusValue)
                        )
                        val thumbCenterY = thumbRadius + ((1 - sliderPositionNormalized) * trackActualHeight)
                        drawCircle(color = thumbColor, radius = thumbRadius, center = Offset(size.width / 2f, thumbCenterY))
                    }
                }
            }

            Text(
                text = element.label,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(2.dp),
                color = labelColor,
                style = MaterialTheme.typography.bodySmall
            )

            // EDIT MODE: RESIZE HANDLE
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.onPrimary, shape = CircleShape)
                        .pointerInput(gridSize) {
                            detectDragGestures(
                                onDragStart = {
                                    isEditing = true
                                    resizeAccumulatorX = 0f
                                    resizeAccumulatorY = 0f
                                },
                                onDragEnd = { isEditing = false },
                                onDragCancel = { isEditing = false },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    resizeAccumulatorX += dragAmount.x
                                    resizeAccumulatorY += dragAmount.y

                                    val cellWidthPx = cellWidth.toPx()
                                    val cellHeightPx = cellHeight.toPx()

                                    val dxCells = (resizeAccumulatorX / cellWidthPx).toInt()
                                    val dyCells = (resizeAccumulatorY / cellHeightPx).toInt()

                                    if (dxCells != 0 || dyCells != 0) {
                                        onResize(
                                            (currentWidth + dxCells).coerceAtLeast(1),
                                            (currentHeight + dyCells).coerceAtLeast(1)
                                        )
                                        if (dxCells != 0) resizeAccumulatorX -= dxCells * cellWidthPx
                                        if (dyCells != 0) resizeAccumulatorY -= dyCells * cellHeightPx
                                    }
                                }
                            )
                        }
                )
            }
        }
    }

    @Composable
    fun DynamicUI(jsonString: String, webSocketInterface: WebSocketManager) {
        val config = remember { json.decodeFromString<LinkConfig>(jsonString) }

        val initialConfig = remember(jsonString) {
            json.decodeFromString<LinkConfig>(jsonString)
        }

        val elements = remember {
            mutableStateListOf<InterfaceElementState>().apply {
                initialConfig.interfaceData.forEach {
                    add(
                        InterfaceElementState(
                            type = it.type,
                            command = it.command,
                            position = it.position.toIntArray(),
                            size = it.size.toIntArray(),
                            label = it.label
                        )
                    )
                }
            }
        }

        GridLayout { gridSize, offset ->
            Box(modifier = Modifier.fillMaxSize()) {
                elements.forEach { element ->
                    when (element.type) {
                        "button" -> ButtonElement(
                            element = element,
                            gridSize = gridSize,
                            offset = offset,
                            onPress = { cmd -> webSocketInterface.sendCommand(cmd) },
                            onRelease = { cmd -> webSocketInterface.sendCommand("!$cmd") },
                            onResize = { newW, newH ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] = elements[index].copy(size = intArrayOf(newW, newH))
                                }
                            },
                            onPositionChange = { newX, newY ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] = elements[index].copy(position = intArrayOf(newX, newY))
                                }
                            },
                            isEditMode = true
                        )
                        "joystick" -> JoystickElement(
                            element = element,
                            gridSize = gridSize,
                            offset = offset,
                            onMove = { cmd, x, y ->
                                webSocketInterface.sendMovementCommand(cmd, x, y)
                            },
                            onResize = { newW, newH ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] =
                                        elements[index].copy(size = intArrayOf(newW, newH))
                                }
                            },
                            onPositionChange = { newX, newY ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] = elements[index].copy(position = intArrayOf(newX, newY))
                                }
                            }
                        )
                        "slider" -> SliderElement(
                            element, gridSize, offset,
                            onValueChange = { cmd, value ->
                                webSocketInterface.sendSliderCommand(cmd, value)
                            },
                            onResize = { newW, newH ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] =
                                        elements[index].copy(size = intArrayOf(newW, newH))
                                }
                            },
                            onPositionChange = { newX, newY ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] = elements[index].copy(position = intArrayOf(newX, newY))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ConnectionStatusIndicator(
                isConnected = sharedState.isConnected,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = sharedState.robotName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            PacketLossIndicator(
                percentage = sharedState.packetLossPercentage,
                modifier = Modifier.weight(1.5f)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (receivedJsonData.isNotEmpty()) {
                DynamicUI(receivedJsonData, webSocketMngr) // Pass webSocketMngr to DynamicUI
            } else {
                Text("No JSON file received!")
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DebugScreen(navController: NavController, debugViewModel: DebugViewModel) {
//    val debugData = debugViewModel.debugData

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

//            if (debugData.isEmpty()) {
//                Text("Waiting for debug data...")
//            } else {
//                debugData.forEach { (key, points) ->
//                    if (points.isNotEmpty()) {
//                        Text(text = key.replaceFirstChar { it.uppercase() },
//                            style = MaterialTheme.typography.titleMedium)
//                        RpmChartScreen(rpmData = points)
//                        Spacer(modifier = Modifier.height(24.dp))
//                    }
//                }
//            }
        }
    }
}

//@Composable
//fun RpmChartScreen(rpmData: List<Point>) {
//    if (rpmData.isEmpty()) return
//
//    val steps = 5
//    val yMin = rpmData.minOfOrNull { it.y } ?: 0f // Safe min
//    val yMax = rpmData.maxOfOrNull { it.y } ?: 0f // Safe max
//    val yRange = yMax - yMin
//    val safeRange = if (yRange == 0f) 1f else yRange
//
//    val yAxisData = AxisData.Builder()
//        .steps(steps)
//        .backgroundColor(MaterialTheme.colorScheme.primary)
//        .labelAndAxisLinePadding(20.dp)
//        .labelData { i ->
//            val yScale = safeRange / steps
//            ((i * yScale) + yMin).formatToSinglePrecision()
//        }
//        .build()
//
//    val xAxisData = AxisData.Builder()
//        .axisStepSize(100.dp)
//        .backgroundColor(MaterialTheme.colorScheme.primary)
//        .steps(rpmData.size.coerceAtLeast(1) -1) // Ensure steps is not negative
//        .labelData { "" }
//        .labelAndAxisLinePadding(0.dp)
//        .build()
//
//    val lineChartData = LineChartData(
//        linePlotData = LinePlotData(
//            lines = listOf(
//                Line(
//                    dataPoints = rpmData,
//                    lineStyle = LineStyle(),
//                    intersectionPoint = IntersectionPoint(),
//                    selectionHighlightPoint = SelectionHighlightPoint(),
//                    shadowUnderLine = ShadowUnderLine(),
//                    selectionHighlightPopUp = SelectionHighlightPopUp()
//                )
//            )
//        ),
//        xAxisData = xAxisData,
//        yAxisData = yAxisData,
//        gridLines = GridLines(),
//        backgroundColor = MaterialTheme.colorScheme.primary
//    )
//
//    Column(Modifier.fillMaxWidth().padding(8.dp)) {
//        LineChart(
//            modifier = Modifier
//                .background(MaterialTheme.colorScheme.primary)
//                .fillMaxWidth()
//                .height(250.dp),
//            lineChartData = lineChartData
//        )
//    }
//}

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
        // TODO: Implement Settings Screen UI
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Text("Settings Screen Placeholder", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
