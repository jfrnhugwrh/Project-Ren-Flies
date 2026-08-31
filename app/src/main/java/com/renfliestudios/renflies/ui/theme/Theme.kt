package com.renfliestudios.renflies.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Placeholder palette - clear contrast, consistent night-sky style.
val NightBlue = Color(0xFF0D1B2A)
val PanelBlue = Color(0xFF1B2A41)
val AccentYellow = Color(0xFFFFC93C)
val AccentCyan = Color(0xFF4FC3F7)
val AccentGreen = Color(0xFF66BB6A)
val AccentRed = Color(0xFFFF5252)
val AccentPurple = Color(0xFFBA68C8)
val TextPrimary = Color(0xFFECF0F1)

private val DarkColors = darkColorScheme(
    primary = AccentYellow,
    onPrimary = Color.Black,
    secondary = AccentCyan,
    background = NightBlue,
    onBackground = TextPrimary,
    surface = PanelBlue,
    onSurface = TextPrimary,
    error = AccentRed
)

@Composable
fun RenFliesTheme(content: @Composable () -> Unit) {
    // The game has a fixed dark night-sky look regardless of system setting.
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
