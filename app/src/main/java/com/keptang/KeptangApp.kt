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

        // Notification channels are created eagerly so the first recording never races their
        // registration. Re-creating a channel with the same ID just updates its label and
        // description rather than duplicating it, so re-running this is harmless.
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
