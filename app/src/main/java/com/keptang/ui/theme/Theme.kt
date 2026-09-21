package com.keptang.ui.theme

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.keptang.data.repository.ColorTheme

/**
 * Every colour below was sampled off the mascot artwork in `drawable-nodpi/`, not picked beside
 * it, so the animals never look pasted onto a palette that was decided without them.
 *
 * Two of them are shared, because both animals are drawn in the same hand:
 *  - [Ink] is the outline on the cat and on the chihuahua alike;
 *  - [MascotPink] is the inside of the ears on both, which is why it carries the accent role in
 *    both themes rather than belonging to either one.
 *
 * What separates them is what each animal is mostly made of. The chihuahua measures 31% pure
 * black against 34% white, so its theme is a real inversion rather than a grey "dark mode". The
 * cat measures 27% apricot against 27% cream, so apricot is a committed field carrying whole
 * regions - not an accent sprinkled over a cream ground, which is where this palette would
 * otherwise drift.
 */
private val Ink = Color(0xFF131313)
private val MascotPink = Color(0xFFF5A89A)

// Cat
private val Cream = Color(0xFFFBF3E6)
private val Apricot = Color(0xFFE89B4B)
private val ApricotDeep = Color(0xFFD9822F)
private val ApricotPale = Color(0xFFF6DCB4)
private val PinkPale = Color(0xFFFBD5CC)

// Chihuahua
private val NearBlack = Color(0xFF0D0D0D)
private val Bone = Color(0xFFF4F2ED)
private val BoneDim = Color(0xFFCFCCC6)
private val Coal = Color(0xFF1C1C1C)
private val Saddle = Color(0xFFC79A63)
private val SaddleDeep = Color(0xFF4A3418)
private val PinkDeep = Color(0xFF5E3A33)

private val CatColors = lightColorScheme(
    primary = MascotPink,
    onPrimary = Ink,
    primaryContainer = PinkPale,
    onPrimaryContainer = Ink,
    secondary = Apricot,
    onSecondary = Ink,
    secondaryContainer = ApricotPale,
    onSecondaryContainer = Ink,
    tertiary = ApricotDeep,
    onTertiary = Cream,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    // The committed field: whole regions are painted apricot, and ink stays legible on it
    // (7.1:1 measured), so it can carry text rather than only decoration.
    surfaceVariant = Apricot,
    onSurfaceVariant = Ink,
    surfaceContainerLowest = Color(0xFFFFFCF5),
    surfaceContainerLow = Color(0xFFFDF8EE),
    surfaceContainer = Color(0xFFF7ECD9),
    surfaceContainerHigh = Color(0xFFF2E3CA),
    surfaceContainerHighest = Color(0xFFEDDABA),
    // Outline is the mascots' own line: one ink colour, drawn at 2dp, never a hairline grey.
    outline = Ink,
    outlineVariant = Ink,
    error = Color(0xFFA32E22),
    onError = Cream,
    errorContainer = Color(0xFFF7C9C2),
    onErrorContainer = Ink
)

private val DogColors = darkColorScheme(
    primary = MascotPink,
    onPrimary = Ink,
    primaryContainer = PinkDeep,
    onPrimaryContainer = PinkPale,
    secondary = Saddle,
    onSecondary = Ink,
    secondaryContainer = SaddleDeep,
    onSecondaryContainer = Color(0xFFF0D6B4),
    tertiary = BoneDim,
    onTertiary = Ink,
    background = NearBlack,
    onBackground = Bone,
    surface = NearBlack,
    onSurface = Bone,
    surfaceVariant = Coal,
    onSurfaceVariant = BoneDim,
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF141414),
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF242424),
    // Inverted: in the chihuahua's world the ink is bone, so the same 2dp line reads white.
    outline = Bone,
    outlineVariant = Bone,
    error = Color(0xFFF2857A),
    onError = Ink,
    errorContainer = Color(0xFF5E241D),
    onErrorContainer = Color(0xFFF7C9C2)
)

/**
 * One radius everywhere, matching the rounded silhouette the mascots are cut out on. Varying the
 * corner by component size is the habit that makes a UI read as assembled from defaults.
 */
private val MascotShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(14.dp)
)

/** The line weight every outlined container in the app is drawn at. */
val MascotOutlineWidth = 2.dp

/**
 * True when the chihuahua's scheme is showing. Provided by [KeptangTheme] so a screen can ask
 * which animal it is sitting in without plumbing the settings flow down to every composable.
 */
val LocalIsDogTheme = staticCompositionLocalOf { false }

/**
 * @param applyToWindow tints the system bars and the window background to match [colorTheme].
 *   [com.keptang.ui.quickadd.QuickAddActivity] passes `false`: it is a translucent popup drawn
 *   over the home screen, so it owns neither the bars nor the background behind it.
 */
@Composable
fun KeptangTheme(
    colorTheme: ColorTheme = ColorTheme.SYSTEM,
    darkTheme: Boolean = isSystemInDarkTheme(),
    applyToWindow: Boolean = true,
    content: @Composable () -> Unit
) {
    val dog = when (colorTheme) {
        ColorTheme.DOG -> true
        ColorTheme.CAT -> false
        ColorTheme.SYSTEM -> darkTheme
    }
    val colorScheme = if (dog) DogColors else CatColors

    if (applyToWindow) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            val window = (view.context as Activity).window
            SideEffect {
                // Edge-to-edge draws under the system bars, so their icons have to be tinted for
                // whichever scheme the user picked - the system only knows about its own
                // day/night setting, which "Cat" and "Dog" deliberately ignore.
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dog
                    isAppearanceLightNavigationBars = !dog
                }
                // The window paints before Compose does, so without this the outgoing theme's
                // colour flashes through on a theme switch or a configuration change.
                window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))
            }
        }
    }

    CompositionLocalProvider(LocalIsDogTheme provides dog) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = MascotShapes,
            typography = KeptangTypography,
            content = content
        )
    }
}
