package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BrutalistLightColorScheme = lightColorScheme(
    primary = BrutalistYellow,
    onPrimary = BrutalistBlack,
    secondary = BrutalistBlack,
    onSecondary = BrutalistWhite,
    tertiary = BrutalistRed,
    onTertiary = BrutalistWhite,
    background = BrutalistCanvasLight,
    onBackground = BrutalistBlack,
    surface = BrutalistSurfaceLight,
    onSurface = BrutalistBlack,
    surfaceVariant = BrutalistLightGray,
    onSurfaceVariant = BrutalistBlack,
    outline = BrutalistBlack,
    outlineVariant = BrutalistDarkGray
)

private val BrutalistDarkColorScheme = darkColorScheme(
    primary = BrutalistYellow,
    onPrimary = BrutalistBlack,
    secondary = BrutalistWhite,
    onSecondary = BrutalistBlack,
    tertiary = BrutalistRed,
    onTertiary = BrutalistWhite,
    background = BrutalistCanvasDark,
    onBackground = BrutalistWhite,
    surface = BrutalistSurfaceDark,
    onSurface = BrutalistWhite,
    surfaceVariant = BrutalistDarkGray,
    onSurfaceVariant = BrutalistWhite,
    outline = BrutalistWhite,
    outlineVariant = BrutalistMutedTextDark
)

// Stark geometric brutalist shapes with sharp edges
val BrutalistShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BrutalistDarkColorScheme else BrutalistLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BrutalistShapes,
        content = content
    )
}
