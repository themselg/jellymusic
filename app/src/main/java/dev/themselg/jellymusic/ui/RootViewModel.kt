package dev.themselg.jellymusic.ui

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import dev.themselg.jellymusic.data.prefs.ThemePreferences
import dev.themselg.jellymusic.data.prefs.ThemeSettings
import dev.themselg.jellymusic.data.session.JellyfinSession
import dev.themselg.jellymusic.data.session.SessionManager
import dev.themselg.jellymusic.ui.theme.AlbumColorThemeController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    themePreferences: ThemePreferences,
    sessionManager: SessionManager,
    albumColorThemeController: AlbumColorThemeController,
) : ViewModel() {

    val themeSettings: StateFlow<ThemeSettings> = themePreferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeSettings(),
    )

    val session: StateFlow<JellyfinSession?> = sessionManager.session

    val seedColor: StateFlow<Color?> = albumColorThemeController.seedColor
}
