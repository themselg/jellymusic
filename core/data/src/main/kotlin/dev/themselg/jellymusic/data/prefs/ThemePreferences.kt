// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface ThemePreferences {
    val settings: Flow<ThemeSettings>
    suspend fun setColorMode(mode: ColorMode)
    suspend fun setDarkMode(mode: DarkMode)
    suspend fun setAmoledBlack(enabled: Boolean)
}

@Singleton
class DataStoreThemePreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferences {

    override val settings: Flow<ThemeSettings> = dataStore.data.map { prefs ->
        ThemeSettings(
            colorMode = prefs[KEY_COLOR_MODE]?.let { runCatching { ColorMode.valueOf(it) }.getOrNull() }
                ?: ColorMode.SYSTEM,
            darkMode = prefs[KEY_DARK_MODE]?.let { runCatching { DarkMode.valueOf(it) }.getOrNull() }
                ?: DarkMode.SYSTEM,
            amoledBlack = prefs[KEY_AMOLED] ?: false,
        )
    }

    override suspend fun setColorMode(mode: ColorMode) {
        dataStore.edit { it[KEY_COLOR_MODE] = mode.name }
    }

    override suspend fun setDarkMode(mode: DarkMode) {
        dataStore.edit { it[KEY_DARK_MODE] = mode.name }
    }

    override suspend fun setAmoledBlack(enabled: Boolean) {
        dataStore.edit { it[KEY_AMOLED] = enabled }
    }

    private companion object {
        val KEY_COLOR_MODE = stringPreferencesKey("color_mode")
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        val KEY_AMOLED = booleanPreferencesKey("amoled_black")
    }
}
