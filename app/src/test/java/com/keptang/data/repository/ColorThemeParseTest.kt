package com.keptang.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The stored theme name outlives the enum. Every install made before the mascot themes has one of
 * the four old names on disk, and getting this wrong silently resets people's choice.
 */
class ColorThemeParseTest {

    @Test
    fun `nothing stored follows the system`() {
        assertEquals(ColorTheme.SYSTEM, ColorTheme.parse(null))
    }

    @Test
    fun `legacy light becomes the cat`() {
        assertEquals(ColorTheme.CAT, ColorTheme.parse("LIGHT"))
    }

    @Test
    fun `legacy dark and amoled both become the chihuahua`() {
        assertEquals(ColorTheme.DOG, ColorTheme.parse("DARK"))
        assertEquals(ColorTheme.DOG, ColorTheme.parse("AMOLED"))
    }

    @Test
    fun `legacy default follows the system`() {
        assertEquals(ColorTheme.SYSTEM, ColorTheme.parse("DEFAULT"))
    }

    @Test
    fun `current names round-trip`() {
        ColorTheme.entries.forEach { theme ->
            assertEquals(theme, ColorTheme.parse(theme.name))
        }
    }

    @Test
    fun `an unknown name falls back rather than throwing`() {
        assertEquals(ColorTheme.SYSTEM, ColorTheme.parse("NEON_FUTURE"))
    }
}
