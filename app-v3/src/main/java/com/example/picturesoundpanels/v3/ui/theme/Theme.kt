package com.example.picturesoundpanels.v3.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AsdPrimary,
    secondary = AsdSecondary,
    tertiary = AsdTertiary,
    background = AsdBackground,
    surface = AsdSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = AsdOnSurface,
    onSurface = AsdOnSurface,
    primaryContainer = AsdAccent,
    onPrimaryContainer = AsdOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF80CBC4), // Soothing Mint Teal for dark mode
    secondary = Color(0xFF80A1D4), // Soft Slate Blue
    tertiary = Color(0xFFBCAAA4), // Soft Taupe
    background = Color(0xFF121212), // High-contrast deep charcoal background
    surface = Color(0xFF1E1E1E), // Soft dark grey surface for cards
    onPrimary = Color(0xFF00332D),
    onSecondary = Color(0xFF00223D),
    onTertiary = Color(0xFF2E1C16),
    onBackground = Color(0xFFECEFF1), // Off-white/light-grey for readability
    onSurface = Color(0xFFECEFF1),
    primaryContainer = Color(0xFF004D40),
    onPrimaryContainer = Color(0xFFE0F2F1)
)

@Composable
fun AsdAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
