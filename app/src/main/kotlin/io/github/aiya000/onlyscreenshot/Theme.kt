package io.github.aiya000.onlyscreenshot

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/** The settings screen is the only thing that is ever drawn, so one dark scheme is enough. */
val OnlyScreenshotColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    onPrimary = Color(0xFF1A1A24),
    surface = Color(0xFF16161F),
    onSurface = Color(0xFFE6E6F0),
    surfaceVariant = Color(0xFF262635),
    onSurfaceVariant = Color(0xFFBFBFD4),
    background = Color(0xFF0E0E14),
    onBackground = Color(0xFFE6E6F0),
    error = Color(0xFFE0736F),
)
