// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Artist
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.LibraryRepository
import dev.themselg.jellymusic.player.PlayerController
import dev.themselg.jellymusic.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistDetail(
    val artist: Artist,
    val albums: List<Album>,
    val topSongs: List<Song>,
)

sealed interface ArtistDetailUiState {
    data object Loading : ArtistDetailUiState
    data class Error(val message: String?) : ArtistDetailUiState
    data class Success(val detail: ArtistDetail) : ArtistDetailUiState
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val artistId: String = savedStateHandle.toRoute<Route.ArtistDetail>().artistId

    private val _state = MutableStateFlow<ArtistDetailUiState>(ArtistDetailUiState.Loading)
    val state: StateFlow<ArtistDetailUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.value = ArtistDetailUiState.Loading
        viewModelScope.launch {
            runCatching {
                val artist = libraryRepository.getArtist(artistId)
                val albums = libraryRepository.getArtistAlbums(artistId)
                val top = libraryRepository.getArtistTopSongs(artistId)
                ArtistDetail(artist, albums, top)
            }.fold(
                onSuccess = { _state.value = ArtistDetailUiState.Success(it) },
                onFailure = { _state.value = ArtistDetailUiState.Error(it.message) },
            )
        }
    }

    fun playTopSongs(startIndex: Int) {
        (_state.value as? ArtistDetailUiState.Success)?.let {
            playerController.play(it.detail.topSongs, startIndex)
        }
    }
}
