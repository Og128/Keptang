package com.keptang

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.keptang.core.Defaults
import com.keptang.di.ServiceLocator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class KeptangApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        ServiceLocator.notificationHelper.ensureChannels()

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
