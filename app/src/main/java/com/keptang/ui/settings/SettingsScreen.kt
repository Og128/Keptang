package com.keptang.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keptang.ui.theme.MascotRole
import com.keptang.ui.theme.mascotFor
import com.keptang.BuildConfig
import com.keptang.R
import com.keptang.core.Defaults
import com.keptang.data.repository.ColorTheme
import com.keptang.di.ServiceLocator
import com.keptang.ui.common.InfoCard
import com.keptang.ui.theme.MascotOutlineWidth

@Composable
fun SettingsScreen(
    onOpenInbox: () -> Unit,
    onEditCategories: () -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSeedingDemoData by viewModel.isSeedingDemoData.collectAsStateWithLifecycle()
    val attentionCount by ServiceLocator.attentionCount.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    var profileName by remember(settings.profileName) { mutableStateOf(settings.profileName) }
    val context = LocalContext.current

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Image(
            painter = painterResource(mascotFor(MascotRole.SETTINGS)),
            contentDescription = stringResource(R.string.settings_title),
            modifier = Modifier.height(56.dp),
            alignment = Alignment.CenterStart,
            contentScale = ContentScale.Fit
        )

        ProfileCard(
            name = profileName,
            onNameChange = { profileName = it; viewModel.setProfileName(it) },
            currencyCode = settings.currencyCode,
            accountCount = accounts.size,
            onOpenProfile = onOpenProfile,
            modifier = Modifier.padding(top = 16.dp)
        )

        SettingsSection(stringResource(R.string.settings_section_general), modifier = Modifier.padding(top = 20.dp)) {
            SettingsDropdown(
                label = stringResource(R.string.settings_default_currency),
                options = Defaults.CURRENCY_OPTIONS,
                selected = settings.currencyCode,
                defaultValue = Defaults.CURRENCY_CODE,
                onSelect = viewModel::setCurrency
            )
            SettingsDropdown(
                label = stringResource(R.string.settings_time_zone),
                options = Defaults.TIME_ZONE_OPTIONS,
                selected = settings.timeZoneId,
                defaultValue = Defaults.TIME_ZONE_ID,
                onSelect = viewModel::setTimeZone,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        SettingsSection(stringResource(R.string.settings_section_appearance), modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.settings_color_theme_label), style = MaterialTheme.typography.bodyMedium)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeChoice(
                    theme = ColorTheme.CAT,
                    selected = settings.colorTheme == ColorTheme.CAT,
                    mascot = R.drawable.widget_closed_cat,
                    label = stringResource(R.string.settings_color_theme_cat),
                    onClick = { viewModel.setColorTheme(ColorTheme.CAT) }
                )
                ThemeChoice(
                    theme = ColorTheme.DOG,
                    selected = settings.colorTheme == ColorTheme.DOG,
                    mascot = R.drawable.widget_mic_blanc,
                    label = stringResource(R.string.settings_color_theme_dog),
                    onClick = { viewModel.setColorTheme(ColorTheme.DOG) }
                )
                ThemeChoice(
                    theme = ColorTheme.SYSTEM,
                    selected = settings.colorTheme == ColorTheme.SYSTEM,
                    mascot = null,
                    label = stringResource(R.string.settings_color_theme_system),
                    onClick = { viewModel.setColorTheme(ColorTheme.SYSTEM) }
                )
            }

            Text(
                stringResource(R.string.settings_language_label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 20.dp)
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
        }

        SettingsSection(stringResource(R.string.settings_section_voice), modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.settings_audio_retention), style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Defaults.AUDIO_RETENTION_OPTIONS.forEach { days ->
                    FilterChip(
                        selected = settings.audioRetentionDays == days,
                        onClick = { viewModel.setAudioRetentionDays(days) },
                        label = { Text(pluralStringResource(R.plurals.settings_retention_days_option, days, days)) }
                    )
                }
            }
        }

        SettingsSection(stringResource(R.string.settings_section_shortcuts), modifier = Modifier.padding(top = 16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                BadgedBox(
                    badge = { if (attentionCount > 0) Badge { Text(attentionCount.toString()) } },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    OutlinedButton(onClick = onOpenInbox) {
                        Text(stringResource(R.string.settings_inbox_button))
                    }
                }
                OutlinedButton(onClick = onEditCategories) {
                    Text(stringResource(R.string.settings_edit_categories_button))
                }
            }
        }

        SettingsSection(stringResource(R.string.settings_section_data), modifier = Modifier.padding(top = 16.dp)) {
            OutlinedButton(onClick = { viewModel.exportExpenses(context) { intent -> context.startActivity(intent) } }) {
                Text(stringResource(R.string.settings_export_csv_button))
            }
        }

        if (BuildConfig.DEBUG) {
            SettingsSection(stringResource(R.string.settings_debug_label), modifier = Modifier.padding(top = 16.dp)) {
                OutlinedButton(
                    onClick = { viewModel.seedDemoData() },
                    enabled = !isSeedingDemoData
                ) {
                    if (isSeedingDemoData) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text(stringResource(R.string.settings_seed_demo_data_button))
                    }
                }
            }
        }
    }
}

/** A named group of related settings, in its own [InfoCard] - so the screen reads as a handful of clear groups instead of one long wall of fields. */
@Composable
private fun SettingsSection(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        InfoCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
private fun ProfileCard(
    name: String,
    onNameChange: (String) -> Unit,
    currencyCode: String,
    accountCount: Int,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoCard(modifier.fillMaxWidth(), onClick = onOpenProfile) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_profile_name_label)) },
                    placeholder = { Text(stringResource(R.string.settings_profile_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "$currencyCode · " + pluralStringResource(R.plurals.settings_profile_accounts, accountCount, accountCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                )
            }
        }
    }
}

/** A read-only field that opens a picker menu instead of a keyboard - same trigger pattern as the category picker on the manual expense screen. */
@Composable
private fun SettingsDropdown(
    label: String,
    options: List<String>,
    selected: String,
    defaultValue: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val displaySelected = if (selected == defaultValue) stringResource(R.string.settings_option_default_suffix, selected) else selected

    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displaySelected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
            options.forEach { value ->
                DropdownMenuItem(
                    text = {
                        Text(if (value == defaultValue) stringResource(R.string.settings_option_default_suffix, value) else value)
                    },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

/**
 * One theme option, showing the animal whose artwork the palette was sampled from. The mascot is
 * the point of the choice, so it is the thing you tap - a text chip would make the two schemes
 * look interchangeable when they are the whole identity.
 */
@Composable
private fun ThemeChoice(
    theme: ColorTheme,
    selected: Boolean,
    mascot: Int?,
    label: String,
    onClick: () -> Unit
) {
    val border = if (selected) MascotOutlineWidth else 1.dp
    val borderColor = if (selected) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    Column(
        modifier = Modifier
            .width(96.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(border, borderColor, MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (mascot != null) {
            Image(
                painter = painterResource(mascot),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
        } else {
            Icon(
                Icons.Filled.Contrast,
                contentDescription = null,
                modifier = Modifier.size(48.dp).padding(8.dp)
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
