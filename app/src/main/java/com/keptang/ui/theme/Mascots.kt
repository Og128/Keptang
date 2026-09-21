package com.keptang.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.keptang.R

/**
 * The jobs a mascot is drawn doing. Each one has a chihuahua and a cat version, so a screen asks
 * for the role and gets whichever animal the active theme belongs to.
 */
enum class MascotRole {
    /** Holding money: the expense form. */
    EXPENSE,

    /** Paw to chin: something needs the user's attention. */
    CONCERNED,

    /** Holding a receipt: the ledger. */
    READING,

    /** Lying down: nothing to do, everything is fine. */
    RELAXED,

    /** Holding a wrench: settings. */
    SETTINGS
}

/**
 * The drawable for [role] in the theme currently showing.
 *
 * Screens never name an animal directly. Hard-coding `m_settings` was fine while there was one
 * mascot; with two it would put the chihuahua on the cat's cream ground, where its black coat is
 * the only thing on screen that does not belong to the palette.
 */
@Composable
@ReadOnlyComposable
@DrawableRes
fun mascotFor(role: MascotRole): Int = if (LocalIsDogTheme.current) {
    when (role) {
        MascotRole.EXPENSE -> R.drawable.m_expense
        MascotRole.CONCERNED -> R.drawable.m_concerned
        MascotRole.READING -> R.drawable.m_reading
        MascotRole.RELAXED -> R.drawable.m_relaxed
        MascotRole.SETTINGS -> R.drawable.m_settings
    }
} else {
    when (role) {
        MascotRole.EXPENSE -> R.drawable.expense_cat
        MascotRole.CONCERNED -> R.drawable.concerned_cat
        MascotRole.READING -> R.drawable.reading_cat
        MascotRole.RELAXED -> R.drawable.relaxed_cat
        MascotRole.SETTINGS -> R.drawable.settings_cat
    }
}
