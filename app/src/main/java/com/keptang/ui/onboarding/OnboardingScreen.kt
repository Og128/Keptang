package com.keptang.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.keptang.R
import com.keptang.ui.common.InfoCard

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Both animals, before the user has met either: whichever theme they end up on, the
            // welcome is the same pair, so neither reads as the default and the other as a skin.
            Image(
                painter = painterResource(R.drawable.mascots_piggybank),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )

            Text(
                stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            OnboardingStep(
                icon = Icons.Filled.Mic,
                title = stringResource(R.string.onboarding_grant_permission),
                body = stringResource(R.string.onboarding_mic_rationale),
                modifier = Modifier.padding(top = 28.dp)
            ) {
                if (!micPermissionGranted) {
                    Button(onClick = onRequestMicPermission, modifier = Modifier.padding(top = 12.dp)) {
                        Text(stringResource(R.string.onboarding_grant_permission))
                    }
                }
            }

            OnboardingStep(
                icon = Icons.Filled.Widgets,
                title = stringResource(R.string.onboarding_add_widget_title),
                body = stringResource(R.string.onboarding_add_widget_body),
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onDone,
                enabled = micPermissionGranted,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.onboarding_done), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun OnboardingStep(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {}
) {
    InfoCard(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 10.dp)
            )
            action()
        }
    }
}
