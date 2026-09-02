package com.keptang.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.R

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)

        Text(
            stringResource(R.string.settings_language_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            val isEnglish = settings.languageCode == "en"
            if (isEnglish) {
                Button(onClick = {}, enabled = false, modifier = Modifier.padding(end = 8.dp)) {
                    Text(stringResource(R.string.settings_language_english))
                }
            } else {
                OutlinedButton(onClick = { viewModel.setLanguage("en") }, modifier = Modifier.padding(end = 8.dp)) {
                    Text(stringResource(R.string.settings_language_english))
                }
            }
            if (!isEnglish) {
                Button(onClick = {}, enabled = false) {
                    Text(stringResource(R.string.settings_language_french))
                }
            } else {
                OutlinedButton(onClick = { viewModel.setLanguage("fr") }) {
                    Text(stringResource(R.string.settings_language_french))
                }
            }
        }

        OutlinedTextField(
            value = settings.currencyCode,
            onValueChange = viewModel::setCurrency,
            label = { Text(stringResource(R.string.settings_default_currency)) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = settings.timeZoneId,
            onValueChange = viewModel::setTimeZone,
            label = { Text(stringResource(R.string.settings_time_zone)) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = settings.defaultAccount,
            onValueChange = viewModel::setDefaultAccount,
            label = { Text(stringResource(R.string.settings_default_account)) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = settings.audioRetentionDays.toString(),
            onValueChange = { text -> text.toIntOrNull()?.let(viewModel::setAudioRetentionDays) },
            label = { Text(stringResource(R.string.settings_audio_retention)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
    }
}
