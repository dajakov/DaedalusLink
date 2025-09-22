package com.dajakov.daedaluslink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times

@Composable
fun GridLayout(content: @Composable (cellSize: Pair<Dp, Dp>, offset: Pair<Dp, Dp>) -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val cornerPadding = 8.dp
    val usableWidth = screenWidth - 2 * cornerPadding
    val usableHeight = screenHeight - 2 * cornerPadding

    val virtualColumns = 8
    val virtualRows = 14
    val cellSize = minOf(usableWidth / virtualColumns, usableHeight / virtualRows)

    val gridWidth = cellSize * virtualColumns

    val horizontalPadding = (screenWidth - gridWidth) / 2
    val verticalPadding = cornerPadding

    val circleColor = MaterialTheme.colorScheme.onPrimary

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = with(density) { horizontalPadding.roundToPx() },
                        y = with(density) { verticalPadding.roundToPx() }
                    )
                }
        ) {
            for (row in 0..virtualRows) {
                for (col in 0..virtualColumns) {
                    drawCircle(
                        color = circleColor,
                        radius = 2.dp.toPx(),
                        center = Offset(
                            x = col * cellSize.toPx(),
                            y = row * cellSize.toPx()
                        )
                    )
                }
            }
        }

        content(Pair(cellSize, cellSize), Pair(horizontalPadding,
            verticalPadding))
    }
}
