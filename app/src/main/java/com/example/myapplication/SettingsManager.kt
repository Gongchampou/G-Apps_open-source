package com.gongchampou.gapps

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {
    private val dataStore = context.dataStore

    val darkModeFlow: Flow<Boolean?> = dataStore.data.map { it[DARK_MODE] }
    val notificationsFlow: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS] ?: true }
    val soundEffectsFlow: Flow<Boolean> = dataStore.data.map { it[SOUND_EFFECTS] ?: true }
    val vibrationFlow: Flow<Boolean> = dataStore.data.map { it[VIBRATION] ?: true }
    val showCircularProgressFlow: Flow<Boolean> = dataStore.data.map { it[SHOW_CIRCULAR_PROGRESS] ?: true }
    val currencyFlow: Flow<String> = dataStore.data.map { it[CURRENCY] ?: "$" }
    val timerToneFlow: Flow<String> = dataStore.data.map { it[TIMER_TONE] ?: "Default" }
    val ebookFontSizeFlow: Flow<Float> = dataStore.data.map { it[EBOOK_FONT_SIZE] ?: 18f }
    val moneyLimitFlow: Flow<Float> = dataStore.data.map { it[MONEY_LIMIT] ?: 0f }
    val moneySpentFlow: Flow<Float> = dataStore.data.map { it[MONEY_SPENT] ?: 0f }
    val keepScreenAwakeFlow: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_AWAKE] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setNotifications(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS] = enabled }
    }

    suspend fun setSoundEffects(enabled: Boolean) {
        dataStore.edit { it[SOUND_EFFECTS] = enabled }
    }

    suspend fun setVibration(enabled: Boolean) {
        dataStore.edit { it[VIBRATION] = enabled }
    }

    suspend fun setShowCircularProgress(enabled: Boolean) {
        dataStore.edit { it[SHOW_CIRCULAR_PROGRESS] = enabled }
    }

    suspend fun setCurrency(currency: String) {
        dataStore.edit { it[CURRENCY] = currency }
    }

    suspend fun setTimerTone(tone: String) {
        dataStore.edit { it[TIMER_TONE] = tone }
    }

    suspend fun setEbookFontSize(size: Float) {
        dataStore.edit { it[EBOOK_FONT_SIZE] = size }
    }

    suspend fun setMoneyLimit(limit: Float) {
        dataStore.edit { it[MONEY_LIMIT] = limit }
    }

    suspend fun setMoneySpent(spent: Float) {
        dataStore.edit { it[MONEY_SPENT] = spent }
    }

    suspend fun setKeepScreenAwake(enabled: Boolean) {
        dataStore.edit { it[KEEP_SCREEN_AWAKE] = enabled }
    }

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val SOUND_EFFECTS = booleanPreferencesKey("sound_effects")
        private val VIBRATION = booleanPreferencesKey("vibration")
        private val SHOW_CIRCULAR_PROGRESS = booleanPreferencesKey("show_circular_progress")
        private val CURRENCY = stringPreferencesKey("currency")
        private val TIMER_TONE = stringPreferencesKey("timer_tone")
        private val EBOOK_FONT_SIZE = floatPreferencesKey("ebook_font_size")
        private val MONEY_LIMIT = floatPreferencesKey("money_limit")
        private val MONEY_SPENT = floatPreferencesKey("money_spent")
        private val KEEP_SCREEN_AWAKE = booleanPreferencesKey("keep_screen_awake")
    }
}
