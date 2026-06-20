package com.morningenglish.app.data.repo

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "morning_english_settings")

/**
 * Persists user preferences in DataStore.
 *
 * Settings:
 *  - play_hour / play_minute: 24h clock, e.g. (6, 30)
 *  - difficulty_filter: 1/2/3, default 1 (初级)
 *  - enabled: master switch for daily alarm
 *  - weekend_play: whether Saturday/Sunday also play
 *  - lock_screen_play: whether playback can show on lock screen
 *  - permissions_granted: set true after permission screen completes
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val PLAY_HOUR = intPreferencesKey("play_hour")
        val PLAY_MINUTE = intPreferencesKey("play_minute")
        val DIFFICULTY = intPreferencesKey("difficulty_filter")
        val ENABLED = booleanPreferencesKey("enabled")
        val WEEKEND_PLAY = booleanPreferencesKey("weekend_play")
        val LOCK_SCREEN_PLAY = booleanPreferencesKey("lock_screen_play")
        val PERMISSIONS_GRANTED = booleanPreferencesKey("permissions_granted")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            playHour = prefs[Keys.PLAY_HOUR] ?: 6,
            playMinute = prefs[Keys.PLAY_MINUTE] ?: 30,
            difficulty = prefs[Keys.DIFFICULTY] ?: 1,
            enabled = prefs[Keys.ENABLED] ?: true,
            weekendPlay = prefs[Keys.WEEKEND_PLAY] ?: true,
            lockScreenPlay = prefs[Keys.LOCK_SCREEN_PLAY] ?: true,
            permissionsGranted = prefs[Keys.PERMISSIONS_GRANTED] ?: false
        )
    }

    suspend fun updatePlayTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PLAY_HOUR] = hour
            prefs[Keys.PLAY_MINUTE] = minute
        }
    }

    suspend fun updateDifficulty(difficulty: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.DIFFICULTY] = difficulty }
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ENABLED] = enabled }
    }

    suspend fun setWeekendPlay(weekendPlay: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.WEEKEND_PLAY] = weekendPlay }
    }

    suspend fun setLockScreenPlay(lockScreenPlay: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.LOCK_SCREEN_PLAY] = lockScreenPlay }
    }

    suspend fun setPermissionsGranted(granted: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.PERMISSIONS_GRANTED] = granted }
    }
}

data class UserSettings(
    val playHour: Int,
    val playMinute: Int,
    val difficulty: Int,
    val enabled: Boolean,
    val weekendPlay: Boolean,
    val lockScreenPlay: Boolean,
    val permissionsGranted: Boolean
)