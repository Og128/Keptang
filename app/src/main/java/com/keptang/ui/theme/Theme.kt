package com.keptang.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.keptang.data.repository.ColorTheme

// "Default" - the original brand green, follows the system's light/dark setting.
private val DefaultLight = lightColorScheme(primary = Color(0xFF0F6B5C))
private val DefaultDark = darkColorScheme(primary = Color(0xFF7FD9C4))

// "Dark" - a distinct, always-dark violet/amber palette (not tied to the system setting).
private val SombreColors = darkColorScheme(
    primary = Color(0xFFA991F2),
    onPrimary = Color(0xFF34186B),
    primaryContainer = Color(0xFF4B2E8C),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFF2A65A),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6B3D00),
    onSecondaryContainer = Color(0xFFFFDDB3)
)

// "Light" - a distinct, always-light blue/coral palette (not tied to the system setting).
private val ClairColors = lightColorScheme(
    primary = Color(0xFF1E6FD9),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E7FF),
    onPrimaryContainer = Color(0xFF002D6B),
    secondary = Color(0xFFE0604B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD3),
    onSecondaryContainer = Color(0xFF5C1B0F)
)

@Composable
fun KeptangTheme(
    colorTheme: ColorTheme = ColorTheme.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (colorTheme) {
        ColorTheme.DEFAULT -> if (darkTheme) DefaultDark else DefaultLight
        ColorTheme.DARK -> SombreColors
        ColorTheme.LIGHT -> ClairColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
