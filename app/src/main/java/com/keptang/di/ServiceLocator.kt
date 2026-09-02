package com.keptang.di

import android.content.Context
import com.keptang.capture.AudioFileStore
import com.keptang.capture.CaptureProcessor
import com.keptang.data.db.KeptangDatabase
import com.keptang.data.repository.AppSettings
import com.keptang.data.repository.BudgetRepository
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.notification.NotificationHelper
import com.keptang.parser.ExpenseParser
import com.keptang.transcription.AndroidSpeechRecognitionProvider
import com.keptang.transcription.TranscriptionProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Minimal hand-rolled dependency container. Kept intentionally simple (no DI framework) since
 * the app has a small, fixed dependency graph; every consumer asks for what it needs by
 * function call instead of field injection, which keeps the parser and repositories trivially
 * testable in isolation.
 */
object ServiceLocator {

    @Volatile private var appContext: Context? = null

    private fun context(): Context =
        appContext ?: error("ServiceLocator.init(context) must be called from Application.onCreate")

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val database: KeptangDatabase by lazy { KeptangDatabase.getInstance(context()) }

    val audioFileStore: AudioFileStore by lazy { AudioFileStore(context()) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(context()) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Synchronously-readable snapshot of [settingsRepository]'s settings, for call sites that
     * can't suspend (e.g. [NotificationHelper], or picking the parser's language mid-parse).
     * Starts collecting the moment anything first touches it and never stops for the life of
     * the process.
     */
    val currentSettings: StateFlow<AppSettings> by lazy {
        settingsRepository.settings.stateIn(appScope, SharingStarted.Eagerly, AppSettings())
    }

    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(database.expenseDao()) }

    val budgetRepository: BudgetRepository by lazy { BudgetRepository(database.budgetDao()) }

    val captureRepository: CaptureRepository by lazy {
        CaptureRepository(database.captureDao(), audioFileStore)
    }

    val expenseParser: ExpenseParser by lazy { ExpenseParser() }

    /** Real on-device recognizer. Swap for another [TranscriptionProvider] to change engines. */
    val transcriptionProvider: TranscriptionProvider by lazy {
        AndroidSpeechRecognitionProvider(context(), settingsRepository)
    }

    val notificationHelper: NotificationHelper by lazy { NotificationHelper(context()) }

    val captureProcessor: CaptureProcessor by lazy {
        CaptureProcessor(
            captureRepository,
            expenseRepository,
            transcriptionProvider,
            expenseParser,
            languageCodeProvider = { currentSettings.value.languageCode }
        )
    }
}
