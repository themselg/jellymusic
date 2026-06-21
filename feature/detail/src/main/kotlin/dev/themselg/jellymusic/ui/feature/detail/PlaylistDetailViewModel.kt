// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.FavoritesRepository
import dev.themselg.jellymusic.domain.repository.PlaylistRepository
import dev.themselg.jellymusic.player.PlayerController
import dev.themselg.jellymusic.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val playlistId: String = savedStateHandle.toRoute<Route.PlaylistDetail>().playlistId

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = DetailUiState.Loading
        viewModelScope.launch {
            runCatching {
                val playlist = playlistRepository.getPlaylist(playlistId)
                val tracks = playlistRepository.getPlaylistTracks(playlistId)
                CollectionDetail(
                    title = playlist.name,
                    subtitle = null, // screen derives a "N songs" subtitle from the track count
                    artworkUrl = playlist.artworkUrl,
                    tracks = tracks,
                )
            }.fold(
                onSuccess = { _state.value = DetailUiState.Success(it) },
                onFailure = { _state.value = DetailUiState.Error(it.message) },
            )
        }
    }

    fun play(startIndex: Int) {
        (_state.value as? DetailUiState.Success)?.let {
            playerController.play(it.detail.tracks, startIndex)
        }
    }

    fun shuffle() {
        (_state.value as? DetailUiState.Success)?.let {
            if (it.detail.tracks.isEmpty()) return
            playerController.toggleShuffle()
            playerController.play(it.detail.tracks, 0)
        }
    }

    fun rename(name: String) {
        viewModelScope.launch {
            runCatching { playlistRepository.renamePlaylist(playlistId, name) }
            load()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { playlistRepository.deletePlaylist(playlistId) }
                .onSuccess { onDeleted() }
        }
    }

    fun removeTrack(song: Song) {
        val entryId = song.playlistItemId ?: return
        viewModelScope.launch {
            runCatching { playlistRepository.removeFromPlaylist(playlistId, listOf(entryId)) }
            load()
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            runCatching { favoritesRepository.setFavorite(song.id, !song.isFavorite) }
                .onSuccess {
                    _state.update { st ->
                        if (st is DetailUiState.Success) {
                            val updated = st.detail.tracks.map {
                                if (it.id == song.id) it.copy(isFavorite = !song.isFavorite) else it
                            }
                            st.copy(detail = st.detail.copy(tracks = updated))
                        } else st
                    }
                }
        }
    }
}
