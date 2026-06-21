// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.domain.model.Playlist
import dev.themselg.jellymusic.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PlaylistsState {
    data object Loading : PlaylistsState
    data class Error(val message: String?) : PlaylistsState
    data class Success(val playlists: List<Playlist>) : PlaylistsState
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val _playlists = MutableStateFlow<PlaylistsState>(PlaylistsState.Loading)
    val playlists: StateFlow<PlaylistsState> = _playlists.asStateFlow()

    init { loadPlaylists() }

    fun loadPlaylists() {
        _playlists.value = PlaylistsState.Loading
        viewModelScope.launch {
            runCatching { playlistRepository.getPlaylists() }.fold(
                onSuccess = { _playlists.value = PlaylistsState.Success(it) },
                onFailure = { _playlists.value = PlaylistsState.Error(it.message) },
            )
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            runCatching { playlistRepository.createPlaylist(name) }
            loadPlaylists()
        }
    }
}
