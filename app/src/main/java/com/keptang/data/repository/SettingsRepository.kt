package com.keptang.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.keptang.core.Defaults
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "keptang_settings")

data class AppSettings(
    val currencyCode: String = Defaults.CURRENCY_CODE,
    val timeZoneId: String = Defaults.TIME_ZONE_ID,
    val defaultAccount: String = Defaults.DEFAULT_ACCOUNT,
    val audioRetentionDays: Int = Defaults.AUDIO_RETENTION_DAYS,
    val languageCode: String = Defaults.LANGUAGE_CODE,
    val firstRunCompleted: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val CURRENCY = stringPreferencesKey("currency_code")
        val TIME_ZONE = stringPreferencesKey("time_zone_id")
        val ACCOUNT = stringPreferencesKey("default_account")
        val RETENTION_DAYS = intPreferencesKey("audio_retention_days")
        val LANGUAGE = stringPreferencesKey("language_code")
        val FIRST_RUN = stringPreferencesKey("first_run_completed")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            currencyCode = prefs[Keys.CURRENCY] ?: Defaults.CURRENCY_CODE,
            timeZoneId = prefs[Keys.TIME_ZONE] ?: Defaults.TIME_ZONE_ID,
            defaultAccount = prefs[Keys.ACCOUNT] ?: Defaults.DEFAULT_ACCOUNT,
            audioRetentionDays = prefs[Keys.RETENTION_DAYS] ?: Defaults.AUDIO_RETENTION_DAYS,
            languageCode = prefs[Keys.LANGUAGE] ?: Defaults.LANGUAGE_CODE,
            firstRunCompleted = prefs[Keys.FIRST_RUN] == "true"
        )
    }

    suspend fun setCurrency(code: String) = context.dataStore.edit { it[Keys.CURRENCY] = code }

    suspend fun setTimeZone(id: String) = context.dataStore.edit { it[Keys.TIME_ZONE] = id }

    suspend fun setDefaultAccount(account: String) = context.dataStore.edit { it[Keys.ACCOUNT] = account }

    suspend fun setAudioRetentionDays(days: Int) = context.dataStore.edit { it[Keys.RETENTION_DAYS] = days }

    suspend fun setLanguage(code: String) = context.dataStore.edit { it[Keys.LANGUAGE] = code }

    suspend fun setFirstRunCompleted() = context.dataStore.edit { it[Keys.FIRST_RUN] = "true" }
}
