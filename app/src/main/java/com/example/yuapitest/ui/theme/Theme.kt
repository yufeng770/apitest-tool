package com.example.yuapitest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = TealLight,
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF8FF8E8),
    secondary = GraphiteLight,
    secondaryContainer = Color(0xFF344B46),
    onSecondaryContainer = Color(0xFFD4E8E2),
    tertiary = AmberLight,
    background = Color(0xFF101413),
    surface = Color(0xFF101413),
    surfaceContainerLow = Color(0xFF171C1A),
    surfaceContainer = Color(0xFF1C2220)
)

private val LightColorScheme = lightColorScheme(
    primary = TealDark,
    primaryContainer = Color(0xFFB2F2E8),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = GraphiteDark,
    secondaryContainer = Color(0xFFD9E5E1),
    onSecondaryContainer = Color(0xFF13201D),
    tertiary = AmberDark,
    background = Color(0xFFF7F9F8),
    surface = Color(0xFFF7F9F8),
    surfaceContainerLow = Color(0xFFF0F4F2),
    surfaceContainer = Color(0xFFE9EFEC)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun YuapitestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
