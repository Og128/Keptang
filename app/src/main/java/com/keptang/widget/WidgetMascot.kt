package com.keptang.widget

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.DrawableRes
import com.keptang.R
import com.keptang.data.repository.ColorTheme
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.first

/**
 * Which animal the home-screen widget wears.
 *
 * The widget has no Compose theme to read, so it resolves the same stored setting the app does
 * and falls back to the system night mode for [ColorTheme.SYSTEM]. Without this the chihuahua
 * sits on the home screen of someone who chose the cat everywhere else.
 */
object WidgetMascot {

    /**
     * @param rest what the widget shows when nothing is happening
     * @param mouthClosed / @param mouthOpen the two frames [com.keptang.capture.VoiceCaptureService]
     *   alternates while listening, so the animal looks like it is talking back
     */
    data class Frames(
        @DrawableRes val rest: Int,
        @DrawableRes val mouthClosed: Int,
        @DrawableRes val mouthOpen: Int
    )

    private val Dog = Frames(
        rest = R.drawable.widget_mic_blanc,
        mouthClosed = R.drawable.widget_mic_blanc_po,
        mouthOpen = R.drawable.widget_mic_blanc_go
    )

    private val Cat = Frames(
        rest = R.drawable.widget_closed_cat,
        mouthClosed = R.drawable.widget_littleopen_cat,
        mouthOpen = R.drawable.widget_openmouth_cat
    )

    suspend fun current(context: Context): Frames =
        if (isDogTheme(context, ServiceLocator.settingsRepository.settings.first().colorTheme)) Dog else Cat

    private fun isDogTheme(context: Context, theme: ColorTheme): Boolean = when (theme) {
        ColorTheme.DOG -> true
        ColorTheme.CAT -> false
        ColorTheme.SYSTEM ->
            context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
    }
}
