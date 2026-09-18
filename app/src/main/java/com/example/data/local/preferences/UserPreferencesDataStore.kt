package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "expense_preferences")

data class AppUserPreferences(
    val currency: String = "₹",
    val themeMode: String = "SYSTEM",
    val isPinLockEnabled: Boolean = false,
    val pinCodeHash: String = "",
    val isBiometricEnabled: Boolean = false,
    val dailyReminderEnabled: Boolean = true,
    val budgetWarningEnabled: Boolean = true,
    val recurringAlertEnabled: Boolean = true,
    val monthlySummaryEnabled: Boolean = true,
    val userDisplayName: String = "Alex Rivera",
    val userEmail: String = "alex.rivera@example.com",
    val lastSyncTimestamp: Long = 0L
)

class UserPreferencesDataStore(private val context: Context) {

    private val KEY_CURRENCY = stringPreferencesKey("currency")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
    private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
    private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    private val KEY_DAILY_REMINDER = booleanPreferencesKey("daily_reminder")
    private val KEY_BUDGET_WARNING = booleanPreferencesKey("budget_warning")
    private val KEY_RECURRING_ALERT = booleanPreferencesKey("recurring_alert")
    private val KEY_MONTHLY_SUMMARY = booleanPreferencesKey("monthly_summary")
    private val KEY_USER_NAME = stringPreferencesKey("user_name")
    private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
    private val KEY_LAST_SYNC = longPreferencesKey("last_sync")

    val preferencesFlow: Flow<AppUserPreferences> = context.dataStore.data.map { prefs ->
        AppUserPreferences(
            currency = prefs[KEY_CURRENCY] ?: "₹",
            themeMode = prefs[KEY_THEME_MODE] ?: "SYSTEM",
            isPinLockEnabled = prefs[KEY_PIN_LOCK_ENABLED] ?: false,
            pinCodeHash = prefs[KEY_PIN_HASH] ?: "",
            isBiometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false,
            dailyReminderEnabled = prefs[KEY_DAILY_REMINDER] ?: true,
            budgetWarningEnabled = prefs[KEY_BUDGET_WARNING] ?: true,
            recurringAlertEnabled = prefs[KEY_RECURRING_ALERT] ?: true,
            monthlySummaryEnabled = prefs[KEY_MONTHLY_SUMMARY] ?: true,
            userDisplayName = prefs[KEY_USER_NAME] ?: "Alex Rivera",
            userEmail = prefs[KEY_USER_EMAIL] ?: "alex.rivera@example.com",
            lastSyncTimestamp = prefs[KEY_LAST_SYNC] ?: 0L
        )
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[KEY_CURRENCY] = currency }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setPinLock(enabled: Boolean, pinHash: String = "") {
        context.dataStore.edit {
            it[KEY_PIN_LOCK_ENABLED] = enabled
            if (pinHash.isNotEmpty()) {
                it[KEY_PIN_HASH] = pinHash
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDailyReminder(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DAILY_REMINDER] = enabled }
    }

    suspend fun setBudgetWarning(enabled: Boolean) {
        context.dataStore.edit { it[KEY_BUDGET_WARNING] = enabled }
    }

    suspend fun setRecurringAlert(enabled: Boolean) {
        context.dataStore.edit { it[KEY_RECURRING_ALERT] = enabled }
    }

    suspend fun setMonthlySummary(enabled: Boolean) {
        context.dataStore.edit { it[KEY_MONTHLY_SUMMARY] = enabled }
    }

    suspend fun setUserProfile(name: String, email: String) {
        context.dataStore.edit {
            it[KEY_USER_NAME] = name
            it[KEY_USER_EMAIL] = email
        }
    }

    suspend fun updateSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { it[KEY_LAST_SYNC] = timestamp }
    }
}
