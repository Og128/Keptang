package com.keptang.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.keptang.R

@Composable
fun OnboardingScreen(
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit,
    onDone: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.onboarding_mic_rationale),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            if (!micPermissionGranted) {
                Button(onClick = onRequestMicPermission) {
                    Text(stringResource(R.string.onboarding_grant_permission))
                }
            }

            Icon(
                Icons.Filled.Widgets,
                contentDescription = null,
                modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
            )
            Text(stringResource(R.string.onboarding_add_widget_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.onboarding_add_widget_body),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Button(onClick = onDone, enabled = micPermissionGranted) {
                Text(stringResource(R.string.onboarding_done))
            }
        }
    }
}
