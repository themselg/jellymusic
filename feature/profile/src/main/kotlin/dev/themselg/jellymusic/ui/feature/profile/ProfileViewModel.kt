// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.data.prefs.ColorMode
import dev.themselg.jellymusic.data.prefs.DarkMode
import dev.themselg.jellymusic.data.prefs.ThemePreferences
import dev.themselg.jellymusic.data.prefs.ThemeSettings
import dev.themselg.jellymusic.data.session.JellyfinUrls
import dev.themselg.jellymusic.data.session.SessionManager
import dev.themselg.jellymusic.domain.repository.FavoritesRepository
import dev.themselg.jellymusic.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val themePreferences: ThemePreferences,
    private val sessionManager: SessionManager,
    private val jellyfinUrls: JellyfinUrls,
    private val playlistRepository: PlaylistRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val settings: StateFlow<ThemeSettings> = themePreferences.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeSettings(),
    )

    val userName: StateFlow<String> = sessionManager.session
        .map { it?.userName.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val serverName: StateFlow<String> = sessionManager.session
        .map { it?.serverName?.takeIf(String::isNotBlank).orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val userImageUrl: StateFlow<String?> = sessionManager.session
        .map { jellyfinUrls.userImageUrl() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _playlistCount = MutableStateFlow(0)
    val playlistCount: StateFlow<Int> = _playlistCount.asStateFlow()

    private val _likedCount = MutableStateFlow(0)
    val likedCount: StateFlow<Int> = _likedCount.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { playlistRepository.getPlaylists().size }
                .onSuccess { _playlistCount.value = it }
        }
        viewModelScope.launch {
            runCatching { favoritesRepository.getFavoriteSongs().size }
                .onSuccess { _likedCount.value = it }
        }
    }

    fun setColorMode(mode: ColorMode) {
        viewModelScope.launch { themePreferences.setColorMode(mode) }
    }

    fun setDarkMode(mode: DarkMode) {
        viewModelScope.launch { themePreferences.setDarkMode(mode) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setAmoledBlack(enabled) }
    }

    fun signOut() {
        sessionManager.signOut()
    }
}
