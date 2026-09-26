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

/**
 * The app's palette, chosen in Settings. Each theme belongs to one of the two mascots and takes
 * its colours from that animal's own artwork. See [com.keptang.ui.theme.KeptangTheme].
 */
enum class ColorTheme {
    /** Follows the system setting: the cat by day, the chihuahua by night. */
    SYSTEM,

    /** The cat: cream ground, a committed apricot field, ink rules. */
    CAT,

    /** The chihuahua: near-black ground, bone ink. */
    DOG;

    companion object {
        /**
         * Settings persist the theme by name, so every install predating the mascot themes still
         * has one of the four old scheme names on disk. Mapping them keeps someone who had chosen
         * a dark scheme on a dark one, rather than silently resetting them to [SYSTEM].
         */
        fun parse(stored: String?): ColorTheme = when (stored) {
            null -> SYSTEM
            "LIGHT" -> CAT
            "DARK", "AMOLED" -> DOG
            "DEFAULT" -> SYSTEM
            else -> entries.firstOrNull { it.name == stored } ?: SYSTEM
        }
    }
}

/** One customizable block on the Dashboard. See [com.keptang.ui.dashboard.DashboardScreen]. */
enum class DashboardCard {
    SPENDING, BUDGET, RECENT
}

/**
 * The persisted form of the Dashboard's card layout: a comma-separated list of [DashboardCard]
 * names, in display order, holding only the visible ones. Kept as free functions so the encoding
 * can be tested without a DataStore - a decode that silently drops a card is invisible in the UI
 * (the card just stops appearing) and would otherwise only surface as a bug report.
 */
internal fun encodeDashboardCards(cards: List<DashboardCard>): String =
    cards.joinToString(",") { card -> card.name }

/**
 * Names this build no longer knows (a card removed since, or a downgrade) are skipped rather than
 * throwing. A stored empty string means "every card hidden", which is a legitimate choice and must
 * not be confused with "nothing stored yet" - only the latter falls back to showing everything.
 */
internal fun decodeDashboardCards(raw: String?): List<DashboardCard> =
    raw?.split(",")
        ?.mapNotNull { name -> runCatching { DashboardCard.valueOf(name) }.getOrNull() }
        ?.distinct()
        ?: DashboardCard.entries

data class AppSettings(
    val profileName: String = "",
    val currencyCode: String = Defaults.CURRENCY_CODE,
    val timeZoneId: String = Defaults.TIME_ZONE_ID,
    /** Id of the [com.keptang.data.db.AccountEntity] new expenses land on. Null until the user picks one, which the first account then stands in for. */
    val defaultAccountId: String? = null,
    val audioRetentionDays: Int = Defaults.AUDIO_RETENTION_DAYS,
    val languageCode: String = Defaults.LANGUAGE_CODE,
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val firstRunCompleted: Boolean = false,
    /** Visible dashboard cards, in display order. A card absent from this list is hidden. */
    val dashboardCardOrder: List<DashboardCard> = DashboardCard.entries
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val CURRENCY = stringPreferencesKey("currency_code")
        val TIME_ZONE = stringPreferencesKey("time_zone_id")
        val DEFAULT_ACCOUNT_ID = stringPreferencesKey("default_account_id")
        val RETENTION_DAYS = intPreferencesKey("audio_retention_days")
        val LANGUAGE = stringPreferencesKey("language_code")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val FIRST_RUN = stringPreferencesKey("first_run_completed")
        val DASHBOARD_CARDS = stringPreferencesKey("dashboard_cards")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            profileName = prefs[Keys.PROFILE_NAME] ?: "",
            currencyCode = prefs[Keys.CURRENCY] ?: Defaults.CURRENCY_CODE,
            timeZoneId = prefs[Keys.TIME_ZONE] ?: Defaults.TIME_ZONE_ID,
            defaultAccountId = prefs[Keys.DEFAULT_ACCOUNT_ID],
            audioRetentionDays = prefs[Keys.RETENTION_DAYS] ?: Defaults.AUDIO_RETENTION_DAYS,
            languageCode = prefs[Keys.LANGUAGE] ?: Defaults.LANGUAGE_CODE,
            colorTheme = ColorTheme.parse(prefs[Keys.COLOR_THEME]),
            firstRunCompleted = prefs[Keys.FIRST_RUN] == "true",
            dashboardCardOrder = decodeDashboardCards(prefs[Keys.DASHBOARD_CARDS])
        )
    }

    suspend fun setProfileName(name: String) = context.dataStore.edit { it[Keys.PROFILE_NAME] = name }

    suspend fun setCurrency(code: String) = context.dataStore.edit { it[Keys.CURRENCY] = code }

    suspend fun setTimeZone(id: String) = context.dataStore.edit { it[Keys.TIME_ZONE] = id }

    suspend fun setDefaultAccountId(accountId: String) = context.dataStore.edit { it[Keys.DEFAULT_ACCOUNT_ID] = accountId }

    suspend fun setAudioRetentionDays(days: Int) = context.dataStore.edit { it[Keys.RETENTION_DAYS] = days }

    suspend fun setLanguage(code: String) = context.dataStore.edit { it[Keys.LANGUAGE] = code }

    suspend fun setColorTheme(theme: ColorTheme) = context.dataStore.edit { it[Keys.COLOR_THEME] = theme.name }

    suspend fun setFirstRunCompleted() = context.dataStore.edit { it[Keys.FIRST_RUN] = "true" }

    suspend fun setDashboardCardOrder(cards: List<DashboardCard>) =
        context.dataStore.edit { it[Keys.DASHBOARD_CARDS] = encodeDashboardCards(cards) }
}
