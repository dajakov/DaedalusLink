package com.dajakov.daedaluslink

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.TextFieldDefaults
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
                .background(MaterialTheme.colorScheme.onPrimary, shape = CircleShape)
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
        element: InterfaceData,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onPress: (String) -> Unit,
        onRelease: (String) -> Unit,
        onResize: (Int, Int) -> Unit,
        onPositionChange: (Int, Int) -> Unit,
        isEditMode: Boolean,
        onOpenSettings: () -> Unit
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
            // Only execute commands if NOT in edit mode
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
                .then(if (isEditing) Modifier.border(2.dp, MaterialTheme.colorScheme.onPrimary, RectangleShape) else Modifier)
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
                onClick = {
                    if (isEditMode) {
                        onOpenSettings() // Trigger settings when clicked in edit mode
                    }
                },
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxSize(),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                // Keep enabled so it's not transparent and remains clickable
                enabled = true
            ) {
                Text(element.label)
            }

            if (isEditMode) {
                ResizeHandle(gridSize, currentWidth, currentHeight, { isEditing = it }, onResize)
            }
        }
    }

    @Composable
    fun JoystickElement(
        element: InterfaceData,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onMove: (String, Byte, Byte) -> Unit,
        onResize: (Int, Int) -> Unit,
        onPositionChange: (Int, Int) -> Unit,
        isEditMode: Boolean
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

        var isEditing by remember { mutableStateOf(false) }

        val circleColor = MaterialTheme.colorScheme.onSurface

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

            else {
                ResizeHandle(
                    gridSize = gridSize,
                    currentWidth = currentWidth,
                    currentHeight = currentHeight,
                    onEditingChange = { isEditing = it },
                    onResize = onResize
                )
            }
        }
    }

    @Composable
    fun SliderElement(
        element: InterfaceData,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onValueChange: (String, Byte) -> Unit,
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
        var sliderPositionNormalized by remember { mutableFloatStateOf(0.5f) }

        val trackColor = MaterialTheme.colorScheme.surface
        val thumbColor = MaterialTheme.colorScheme.onSurface
        val labelColor = MaterialTheme.colorScheme.onSurface

        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = offset.first + currentX * cellWidth,
                    y = offset.second + currentY * cellHeight
                )
                .size(currentWidth * cellWidth, currentHeight * cellHeight)
                .background(MaterialTheme.colorScheme.primary)
                .then(
                    if (isEditing) Modifier.border(2.dp, Color.White, RectangleShape)
                    else Modifier
                )
                .pointerInput(gridSize, isEditMode) {
                    if (isEditMode) {
                        // Use refactored Move Logic
                        UIElementLogic.run {
                            handleMoveDrag(
                                gridSize = gridSize,
                                getCurrentX = { currentX },
                                getCurrentY = { currentY },
                                onEditingChange = { isEditing = it },
                                onPositionChange = onPositionChange
                            )
                        }
                    } else {
                        // Normal Slider Logic
                        detectDragGestures { change, _ ->
                            val isHorizontal = size.width >= size.height
                            val thumbRadiusVisual = if (isHorizontal) size.height * 0.4f else size.width * 0.4f
                            val newPositionNormalized = if (isHorizontal) {
                                val trackActualWidth = size.width - 2 * thumbRadiusVisual
                                if (trackActualWidth <= 0) 0.5f
                                else (change.position.x - thumbRadiusVisual).div(trackActualWidth).coerceIn(0f, 1f)
                            } else {
                                val trackActualHeight = size.height - 2 * thumbRadiusVisual
                                if (trackActualHeight <= 0) 0.5f
                                else (1 - (change.position.y - thumbRadiusVisual).div(trackActualHeight)).coerceIn(0f, 1f)
                            }
                            sliderPositionNormalized = newPositionNormalized
                            val byteValue = (sliderPositionNormalized * 255f - 128f).toInt().coerceIn(-128, 127).toByte()
                            onValueChange(element.command, byteValue)
                            change.consume()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
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
                modifier = Modifier.align(Alignment.TopCenter).padding(2.dp),
                color = labelColor,
                style = MaterialTheme.typography.bodySmall
            )

            if (isEditMode) {
                // Use refactored Resize Logic
                ResizeHandle(
                    gridSize = gridSize,
                    currentWidth = currentWidth,
                    currentHeight = currentHeight,
                    onEditingChange = { isEditing = it },
                    onResize = onResize
                )
            }
        }
    }

    @Composable
    fun SpawnCard(label: String, type: String, onSelect: (String) -> Unit) {Box(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(16.dp)) // Slightly rounder for modern look
            .background(MaterialTheme.colorScheme.surfaceVariant) // Use surfaceVariant for cards
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) { detectTapGestures { onSelect(type) } },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Contrast against surfaceVariant
        )
    }
    }

    @Composable
    fun SpawnScreenOverlay(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
        // Use Scrim-like background using surface color with high transparency
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp)) // Modern M3 container rounding
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(32.dp)
                    .pointerInput(Unit) { /* Prevent clicks from passing through */ }
            ) {
                Text(
                    text = "Select Element to Spawn",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    SpawnCard("Button", "button", onSelect)
                    SpawnCard("Joystick", "joystick", onSelect)
                    SpawnCard("Slider", "slider", onSelect)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(0.7f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RectangleShape
                ) {
                    Text("Cancel")
                }
            }
        }
    }
    @Composable
    fun ElementSettingsOverlay(
        element: InterfaceData,
        onDismiss: () -> Unit,
        onDelete: () -> Unit,
        onUpdateLabel: (String) -> Unit,
        onUpdateCommand: (String) -> Unit
    ) {// Local state for smooth typing
        var tempLabel by remember(element.command) { mutableStateOf(element.label) }
        var tempCommand by remember(element.command) { mutableStateOf(element.command) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
                    .pointerInput(Unit) { /* Stop propagation */ },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Edit Element", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)

                androidx.compose.material3.OutlinedTextField(
                    value = tempLabel,
                    onValueChange = {
                        tempLabel = it
                        onUpdateLabel(it) // Update global state
                    },
                    label = { Text("Label", color = MaterialTheme.colorScheme.onPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                androidx.compose.material3.OutlinedTextField(
                    value = tempCommand,
                    onValueChange = {
                        tempCommand = it
                        onUpdateCommand(it) // Update global state
                    },
                    label = { Text("Command", color = MaterialTheme.colorScheme.onPrimary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.onPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .weight(1f)
                            .padding(0.dp)
                            .fillMaxWidth(),
                        shape = RectangleShape
                    ) { Text("Delete", color = MaterialTheme.colorScheme.onError) }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(0.dp)
                            .fillMaxWidth(),
                        shape = RectangleShape
                    ) { Text("Done", color = MaterialTheme.colorScheme.onSecondary) }
                }
            }
        }
    }

    @Composable
    fun DynamicUI(jsonString: String, webSocketInterface: WebSocketManager) {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

        LaunchedEffect(jsonString) {
            if (sharedState.persistentElements.isEmpty() && jsonString.isNotEmpty()) {
                try {
                    val config = json.decodeFromString<LinkConfig>(jsonString)
                    val mapped = config.interfaceData.map {
                        InterfaceData(
                            type = it.type,
                            command = it.command,
                            position = it.position,
                            size = it.size,
                            label = it.label
                        )
                    }
                    sharedState.persistentElements.addAll(mapped)
                } catch (e: Exception) {
                    println("JSON Parsing error: ${e.message}")
                }
            }
        }

        val elements = sharedState.persistentElements

        var showSpawnScreen by remember { mutableStateOf(false) }
        var showElementSettings by remember { mutableStateOf(false) }
        var selectedElement by remember { mutableStateOf<InterfaceData?>(null) }
        var spawnCell by remember { mutableStateOf(Pair(0, 0)) }

        GridLayout { gridSize, offset ->
            Box(modifier = Modifier
                .fillMaxSize()
                .pointerInput(sharedState.isEditMode) {
                    if (!sharedState.isEditMode) return@pointerInput
                    detectTapGestures { pressOffset ->
                        val cellX = (pressOffset.x / gridSize.first.toPx()).toInt()
                        val cellY = (pressOffset.y / gridSize.second.toPx()).toInt()

                        // Check if an element occupies this cell
                        val clickedElement = elements.find { el ->
                            cellX >= el.position[0] && cellX < el.position[0] + el.size[0] &&
                                    cellY >= el.position[1] && cellY < el.position[1] + el.size[1]
                        }

                        if (clickedElement != null) {
                            // CLICKED ON ELEMENT -> Open Settings
                            selectedElement = clickedElement
                            showElementSettings = true
                        } else {
                            // CLICKED ON EMPTY GRID -> Open Spawn Menu
                            spawnCell = Pair(cellX, cellY)
                            showSpawnScreen = true
                        }
                    }
                }
            ) {
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
                                    elements[index] = elements[index].copy(size = listOf(newW, newH))
                                }
                            },
                            onPositionChange = { newX, newY ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] = elements[index].copy(position = listOf(newX, newY))
                                }
                            },
                            isEditMode = sharedState.isEditMode,
                            onOpenSettings = {
                                selectedElement = element
                                showElementSettings = true
                            }
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
                                        elements[index].copy(size = listOf(newW, newH))
                                }
                            },
                            onPositionChange = { newX, newY ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] = elements[index].copy(position = listOf(newX, newY))
                                }
                            },
                            isEditMode = sharedState.isEditMode
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
                                        elements[index].copy(size = listOf(newW, newH))
                                }
                            },
                            onPositionChange = { newX, newY ->
                                val index = elements.indexOfFirst { it.command == element.command }
                                if (index != -1) {
                                    elements[index] = elements[index].copy(position = listOf(newX, newY))
                                }
                            },
                            isEditMode = sharedState.isEditMode
                        )
                    }
                }

                if (showSpawnScreen) {
                    SpawnScreenOverlay(
                        onDismiss = { showSpawnScreen = false },
                        onSelect = { type ->
                            val newElement = when(type) {
                                "button" -> InterfaceData("button", "New Button", listOf(spawnCell.first, spawnCell.second), listOf(2, 2), "btn_${System.currentTimeMillis()}")
                                "joystick" -> InterfaceData("joystick", "New Joy", listOf(spawnCell.first, spawnCell.second), listOf(4, 4), "joy_${System.currentTimeMillis()}")
                                else -> InterfaceData("slider", "New Slide", listOf(spawnCell.first, spawnCell.second), listOf(1, 4), "sld_${System.currentTimeMillis()}")
                            }
                            elements.add(newElement)
                            showSpawnScreen = false
                        }
                    )
                }

                if (showElementSettings && selectedElement != null) {
                    ElementSettingsOverlay(
                        element = selectedElement!!,
                        onDismiss = { showElementSettings = false },
                        onDelete = {
                            elements.removeIf { it.command == selectedElement?.command }
                            showElementSettings = false
                        },
                        onUpdateLabel = { newLabel ->
                            val index = elements.indexOfFirst { it.command == selectedElement?.command }
                            if (index != -1) {
                                elements[index] = elements[index].copy(label = newLabel)
                                selectedElement = elements[index]
                            }
                        },
                        onUpdateCommand = { newCmd ->
                            val index = elements.indexOfFirst { it.command == selectedElement?.command }
                            if (index != -1) {
                                elements[index] = elements[index].copy(command = newCmd)
                                selectedElement = elements[index]
                            }
                        }
                    )
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

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // EDIT MODE TOGGLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Edit Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        "Enable moving and resizing elements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                }
                androidx.compose.material3.Switch(
                    checked = sharedState.isEditMode,
                    onCheckedChange = { sharedState.isEditMode = it },
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.onPrimary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}
