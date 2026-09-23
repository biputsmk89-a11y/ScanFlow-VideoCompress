package com.compressflow.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "compressflow_settings")

data class AppSettings(
    val saveLocation: String = "Movies/CompressFlow",
    val filenamePattern: String = "{name}_compressed",
    val defaultQuality: String = "Balanced",
    val defaultCodec: String = "Auto (best available)",
    val keepAudio: Boolean = true,
    val darkMode: Boolean = false,
    val themeMode: String = "SYSTEM", // "SYSTEM", "LIGHT", "DARK"
    val notifications: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SAVE_LOCATION = stringPreferencesKey("save_location")
        val FILENAME_PATTERN = stringPreferencesKey("filename_pattern")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val DEFAULT_CODEC = stringPreferencesKey("default_codec")
        val KEEP_AUDIO = booleanPreferencesKey("keep_audio")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS = booleanPreferencesKey("notifications")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            saveLocation = preferences[PreferencesKeys.SAVE_LOCATION] ?: "Movies/CompressFlow",
            filenamePattern = preferences[PreferencesKeys.FILENAME_PATTERN] ?: "{name}_compressed",
            defaultQuality = preferences[PreferencesKeys.DEFAULT_QUALITY] ?: "Balanced",
            defaultCodec = preferences[PreferencesKeys.DEFAULT_CODEC] ?: "Auto (best available)",
            keepAudio = preferences[PreferencesKeys.KEEP_AUDIO] ?: true,
            darkMode = preferences[PreferencesKeys.DARK_MODE] ?: false,
            themeMode = preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM",
            notifications = preferences[PreferencesKeys.NOTIFICATIONS] ?: true
        )
    }

    suspend fun setKeepAudio(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_AUDIO] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = enabled
            preferences[PreferencesKeys.THEME_MODE] = if (enabled) "DARK" else "SYSTEM"
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
            preferences[PreferencesKeys.DARK_MODE] = (mode == "DARK")
        }
    }

    suspend fun setSaveLocation(location: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAVE_LOCATION] = location
        }
    }

    suspend fun setNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS] = enabled
        }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_QUALITY] = quality
        }
    }

    suspend fun setDefaultCodec(codec: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CODEC] = codec
        }
    }

    suspend fun setFilenamePattern(pattern: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FILENAME_PATTERN] = pattern
        }
    }
}
