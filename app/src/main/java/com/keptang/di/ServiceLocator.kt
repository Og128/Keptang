package com.keptang.di

import android.content.Context
import com.keptang.capture.AudioFileStore
import com.keptang.capture.CaptureProcessor
import com.keptang.data.db.KeptangDatabase
import com.keptang.data.repository.CaptureRepository
import com.keptang.data.repository.ExpenseRepository
import com.keptang.data.repository.SettingsRepository
import com.keptang.notification.NotificationHelper
import com.keptang.parser.ExpenseParser
import com.keptang.transcription.AndroidSpeechRecognitionProvider
import com.keptang.transcription.TranscriptionProvider

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

    val expenseRepository: ExpenseRepository by lazy { ExpenseRepository(database.expenseDao()) }

    val captureRepository: CaptureRepository by lazy {
        CaptureRepository(database.captureDao(), audioFileStore)
    }

    val expenseParser: ExpenseParser by lazy { ExpenseParser() }

    /** Real on-device recognizer. Swap for another [TranscriptionProvider] to change engines. */
    val transcriptionProvider: TranscriptionProvider by lazy { AndroidSpeechRecognitionProvider(context()) }

    val notificationHelper: NotificationHelper by lazy { NotificationHelper(context()) }

    val captureProcessor: CaptureProcessor by lazy {
        CaptureProcessor(captureRepository, expenseRepository, transcriptionProvider, expenseParser)
    }
}
