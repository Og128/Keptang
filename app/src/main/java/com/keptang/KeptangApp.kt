package com.keptang

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.keptang.core.Defaults
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class KeptangApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        // Notification channel names/descriptions are built outside the Compose tree (see
        // NotificationHelper.localizedContext()), so they need their own re-localization on
        // every language change - re-creating a channel with the same ID just updates its
        // label/description, it doesn't duplicate the channel.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            ServiceLocator.currentSettings
                .map { it.languageCode }
                .distinctUntilChanged()
                .collect { ServiceLocator.notificationHelper.ensureChannels() }
        }

        // Best-effort retention sweep on process start. A prototype-scale app does not need a
        // WorkManager-scheduled job for this; see README "Known issues" for the production gap.
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            val retentionDays = try {
                ServiceLocator.settingsRepository.settings.first().audioRetentionDays
            } catch (t: Throwable) {
                Defaults.AUDIO_RETENTION_DAYS
            }
            ServiceLocator.captureRepository.purgeExpiredAudio(retentionDays)
        }
    }
}
