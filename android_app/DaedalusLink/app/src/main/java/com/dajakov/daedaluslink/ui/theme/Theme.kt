package com.dajakov.daedaluslink.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = Black,
    onSecondary = White,
    surfaceVariant = Black,
    onSurfaceVariant = LightGray,
    background = White,
    onBackground = Black,
    surface = LightGray,
    onSurface = Black,
    error = Color(0xFFB00020),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = White,
    onSecondary = Black,
    surfaceVariant = White,
    onSurfaceVariant = DarkGray,
    background = Color(0xFF121212),
    onBackground = White,
    surface = Color(0xFF121212),
    onSurface = White,
    error = Color(0xFFCF6679),
    onError = Color.Black
)

@Composable
fun DaedalusLinkTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (useDarkTheme) {
        DarkColors
    } else {
        LightColors
    }

    // Update the MaterialTheme call to use the modified colors object
    // and provide the custom colors through a CompositionLocal if needed,
    // or rely on the extension properties as shown.
    // For simplicity with extensions, no changes needed here if extensions use isSystemInDarkTheme().

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

