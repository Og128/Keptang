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

// "AMOLED" - same violet/amber accents as "Dark", but background/surface pushed to pure black
// to actually save power on OLED screens (a dark-gray Material surface doesn't).
private val AmoledColors = SombreColors.copy(
    background = Color(0xFF000000),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF121212),
    surfaceContainer = Color(0xFF0A0A0A),
    surfaceContainerHigh = Color(0xFF121212),
    surfaceContainerHighest = Color(0xFF1A1A1A),
    surfaceContainerLow = Color(0xFF050505),
    surfaceContainerLowest = Color(0xFF000000)
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
        ColorTheme.AMOLED -> AmoledColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
