package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MusicColorScheme = darkColorScheme(
    primary = MusicPrimary,
    onPrimary = Color(0xFF0F0F1A),
    primaryContainer = MusicDarkSurfaceVariant,
    onPrimaryContainer = MusicPrimary,
    secondary = MusicSecondary,
    onSecondary = Color(0xFF0F0F1A),
    tertiary = MusicTertiary,
    onTertiary = Color.White,
    background = MusicDarkBackground,
    onBackground = MusicTextPrimary,
    surface = MusicDarkSurface,
    onSurface = MusicTextPrimary,
    surfaceVariant = MusicDarkSurfaceVariant,
    onSurfaceVariant = MusicTextSecondary,
    outline = MusicBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force modern dark theme as requested
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MusicColorScheme,
        typography = Typography,
        content = content
    )
}
