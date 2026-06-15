package dev.themselg.jellymusic.ui.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.FavoritesRepository
import dev.themselg.jellymusic.domain.repository.LibraryRepository
import dev.themselg.jellymusic.player.PlayerController
import dev.themselg.jellymusic.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.navigation.toRoute
import javax.inject.Inject

data class CollectionDetail(
    val title: String,
    val subtitle: String?,
    val artworkUrl: String?,
    val tracks: List<Song>,
)

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Error(val message: String?) : DetailUiState
    data class Success(val detail: CollectionDetail) : DetailUiState
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: String = savedStateHandle.toRoute<Route.AlbumDetail>().albumId

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    private var artistId: String? = null

    init { load() }

    fun load() {
        _state.value = DetailUiState.Loading
        viewModelScope.launch {
            runCatching {
                val album: Album = libraryRepository.getAlbum(albumId)
                val tracks = libraryRepository.getAlbumTracks(albumId)
                artistId = album.artistId
                CollectionDetail(
                    title = album.name,
                    subtitle = album.artistName,
                    artworkUrl = album.artworkUrl,
                    tracks = tracks,
                )
            }.fold(
                onSuccess = { _state.value = DetailUiState.Success(it) },
                onFailure = { _state.value = DetailUiState.Error(it.message) },
            )
        }
    }

    fun artistId(): String? = artistId

    fun play(startIndex: Int) {
        (_state.value as? DetailUiState.Success)?.let {
            playerController.play(it.detail.tracks, startIndex)
        }
    }

    fun shuffle() {
        (_state.value as? DetailUiState.Success)?.let {
            if (it.detail.tracks.isEmpty()) return
            // Enable shuffle then start playback so the player shuffles the new queue.
            playerController.toggleShuffle()
            playerController.play(it.detail.tracks, 0)
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
