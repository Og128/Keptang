package com.keptang.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.keptang.data.repository.AppSettings
import com.keptang.di.ServiceLocator
import com.keptang.ui.navigation.KeptangNavHost
import com.keptang.ui.onboarding.OnboardingScreen
import com.keptang.ui.theme.KeptangTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Entry point for the app's own UI (onboarding, inbox, expenses, review, settings). Never
 * started from [com.keptang.widget.VoiceCaptureWidgetProvider] - the widget talks directly to
 * [com.keptang.capture.VoiceCaptureService] instead.
 */
class MainActivity : ComponentActivity() {

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Held as state rather than read once from [getIntent], because this activity is `singleTask`:
     * when the app is already open, tapping a result notification delivers the capture id through
     * [onNewIntent] and never re-runs [onCreate]. Reading it only there meant the tap simply
     * raised whatever screen happened to be showing.
     */
    private var startCaptureId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startCaptureId = intent.getStringExtra(EXTRA_OPEN_CAPTURE_ID)

        setContent {
            val settings by ServiceLocator.settingsRepository.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            KeptangTheme(colorTheme = settings.colorTheme) {
                var micGranted by remember { mutableStateOf(hasMicPermission()) }
                var firstRunCompleted by remember { mutableStateOf<Boolean?>(null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results -> micGranted = results[Manifest.permission.RECORD_AUDIO] == true }

                LaunchedEffect(Unit) {
                    val currentSettings = ServiceLocator.settingsRepository.settings.first()
                    firstRunCompleted = currentSettings.firstRunCompleted
                    ServiceLocator.recurringExpenseGenerator.generateDueExpenses(
                        timeZoneId = currentSettings.timeZoneId,
                        currencyCode = currentSettings.currencyCode
                    )
                }

                when (firstRunCompleted) {
                    null -> Unit // wait for the persisted flag to load
                    false -> OnboardingScreen(
                        micPermissionGranted = micGranted,
                        onRequestMicPermission = { permissionLauncher.launch(requiredPermissions()) },
                        onDone = {
                            firstRunCompleted = true
                            lifecycleScope.launch { ServiceLocator.settingsRepository.setFirstRunCompleted() }
                        }
                    )
                    // Cleared once consumed, so tapping the same notification again still navigates.
                    true -> KeptangNavHost(
                        startCaptureId = startCaptureId,
                        onStartCaptureConsumed = { startCaptureId = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_CAPTURE_ID)?.let { startCaptureId = it }
    }

    private fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        return permissions.toTypedArray()
    }

    companion object {
        const val EXTRA_OPEN_CAPTURE_ID = "extra_open_capture_id"
    }
}
