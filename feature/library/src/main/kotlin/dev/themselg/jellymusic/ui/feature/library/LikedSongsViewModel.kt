// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.FavoritesRepository
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LikedSongsState {
    data object Loading : LikedSongsState
    data class Error(val message: String?) : LikedSongsState
    data class Success(val songs: List<Song>) : LikedSongsState
}

@HiltViewModel
class LikedSongsViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _state = MutableStateFlow<LikedSongsState>(LikedSongsState.Loading)
    val state: StateFlow<LikedSongsState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = LikedSongsState.Loading
        viewModelScope.launch {
            runCatching { favoritesRepository.getFavoriteSongs() }.fold(
                onSuccess = { _state.value = LikedSongsState.Success(it) },
                onFailure = { _state.value = LikedSongsState.Error(it.message) },
            )
        }
    }

    fun play(startIndex: Int) {
        (_state.value as? LikedSongsState.Success)?.let {
            playerController.play(it.songs, startIndex)
        }
    }

    fun shuffle() {
        (_state.value as? LikedSongsState.Success)?.let {
            if (it.songs.isEmpty()) return
            playerController.toggleShuffle()
            playerController.play(it.songs, 0)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            runCatching { favoritesRepository.setFavorite(song.id, !song.isFavorite) }
            load()
        }
    }
}
