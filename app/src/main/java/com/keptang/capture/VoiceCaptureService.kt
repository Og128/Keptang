package com.keptang.capture

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.keptang.core.Defaults
import com.keptang.di.ServiceLocator
import com.keptang.notification.NotificationIds
import com.keptang.transcription.TranscriptionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/**
 * Foreground microphone service. Started directly from the home-screen widget - never from
 * [com.keptang.ui.MainActivity] - and always terminates itself once a capture has either been
 * cancelled or fully processed (saved audio -> transcription attempt -> parsing -> DB write ->
 * result notification). See [CaptureProcessor] for the pipeline after recording stops.
 */
class VoiceCaptureService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var activeRecorder: AudioRecorderController? = null
    private var isRecording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> activeRecorder?.requestManualStop()
            ACTION_CANCEL -> activeRecorder?.requestCancel()
            else -> if (isRecording) activeRecorder?.requestManualStop() else beginRecording()
        }
        return START_NOT_STICKY
    }

    private fun beginRecording() {
        isRecording = true
        val captureId = UUID.randomUUID().toString()

        val hasMicPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

        // Context.startForegroundService() (used by the widget's PendingIntent) obligates us to
        // call startForeground() promptly regardless of outcome, so this still happens even when
        // we're about to immediately bail out for a missing permission.
        val notification = if (hasMicPermission) {
            ServiceLocator.notificationHelper.buildRecordingNotification(captureId)
        } else {
            ServiceLocator.notificationHelper.buildPermissionRequiredNotification()
        }
        ServiceCompat.startForeground(
            this,
            NotificationIds.RECORDING,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        if (!hasMicPermission) {
            isRecording = false
            finishService()
            return
        }

        vibrateFeedback()

        val audioFile = ServiceLocator.audioFileStore.newFileFor(captureId)
        val recorder = AudioRecorderController()
        activeRecorder = recorder

        serviceScope.launch {
            // Persist the CAPTURED row target before any recognition is attempted, so the
            // recording can never be lost even if the process dies mid-transcription.
            ServiceLocator.captureRepository.createRecording(captureId, audioFile.absolutePath, Defaults.TIME_ZONE_ID)

            val transcriptionProvider = ServiceLocator.transcriptionProvider
            val providerAvailable = runCatching { transcriptionProvider.isAvailable() }.getOrDefault(false)
            val recognitionDeferred = if (providerAvailable) {
                async { transcriptionProvider.listen(onEndOfSpeech = { recorder.requestSpeechEndedStop() }) }
            } else {
                null
            }

            val outcome = recorder.record(
                outputFile = audioFile,
                maxDurationMillis = MAX_RECORDING_DURATION_MILLIS,
                silenceDetector = SilenceDetector()
            )

            if (outcome.stopReason == StopReason.CANCELLED) {
                recognitionDeferred?.let { transcriptionProvider.cancel() }
                ServiceLocator.captureRepository.markCancelled(captureId)
                finishService()
                return@launch
            }

            recognitionDeferred?.let { transcriptionProvider.stopListening() }
            ServiceLocator.captureRepository.markCaptured(captureId, outcome.durationMillis)

            val transcriptionResult = if (recognitionDeferred != null) {
                withTimeoutOrNull(RECOGNITION_GRACE_PERIOD_MILLIS) { recognitionDeferred.await() }
                    ?: TranscriptionResult.Failed("Recognition timed out")
            } else {
                TranscriptionResult.Unavailable("On-device speech recognition is not available on this device")
            }

            val processOutcome = ServiceLocator.captureProcessor.processFreshCapture(captureId, transcriptionResult)
            ServiceLocator.notificationHelper.notifyResult(captureId, processOutcome)

            finishService()
        }
    }

    private fun finishService() {
        isRecording = false
        activeRecorder = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun vibrateFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_TOGGLE = "com.keptang.action.WIDGET_TOGGLE_RECORDING"
        const val ACTION_STOP = "com.keptang.action.STOP_RECORDING"
        const val ACTION_CANCEL = "com.keptang.action.CANCEL_RECORDING"

        const val MAX_RECORDING_DURATION_MILLIS = 60_000L
        private const val RECOGNITION_GRACE_PERIOD_MILLIS = 8_000L
    }
}
