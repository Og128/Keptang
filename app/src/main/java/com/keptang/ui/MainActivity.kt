package com.keptang.ui

import android.Manifest
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
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startCaptureId = intent.getStringExtra(EXTRA_OPEN_CAPTURE_ID)

        setContent {
            KeptangTheme {
                var micGranted by remember { mutableStateOf(hasMicPermission()) }
                var firstRunCompleted by remember { mutableStateOf<Boolean?>(null) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results -> micGranted = results[Manifest.permission.RECORD_AUDIO] == true }

                LaunchedEffect(Unit) {
                    firstRunCompleted = ServiceLocator.settingsRepository.settings.first().firstRunCompleted
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
                    true -> KeptangNavHost(startCaptureId = startCaptureId)
                }
            }
        }
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
