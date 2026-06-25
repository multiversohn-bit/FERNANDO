package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = YellowPrimary,
    primaryContainer = YellowPrimaryVariant,
    onPrimary = Color.Black,
    secondary = YellowSecondary,
    background = BlackBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFFFFC107).copy(alpha = 0.6f),
    outlineVariant = Color(0xFF444444)
)

private val LightColorScheme = lightColorScheme(
    primary = YellowPrimaryVariant,
    primaryContainer = YellowPrimary,
    onPrimary = Color.Black,
    secondary = YellowSecondary,
    background = Color(0xFF121212), // Let's keep it beautifully dark and vibrant for both themes to ensure yellow/black vibe!
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFE0E0E0),
    outline = Color(0xFFFFC107).copy(alpha = 0.6f),
    outlineVariant = Color(0xFF444444)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default for the premium yellow & black look
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce brand identity
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
