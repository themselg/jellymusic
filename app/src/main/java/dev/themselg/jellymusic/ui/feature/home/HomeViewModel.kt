package dev.themselg.jellymusic.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Artist
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.data.session.SessionManager
import dev.themselg.jellymusic.domain.repository.LibraryRepository
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeTab { ALBUMS, ARTISTS, SONGS }

sealed interface TabContent<out T> {
    data object Loading : TabContent<Nothing>
    data class Error(val message: String?) : TabContent<Nothing>
    data class Success<T>(val data: T) : TabContent<T>
}

data class AlbumsTabData(
    val recentlyAdded: List<Album>,
    val albums: List<Album>,
)

data class HomeUiState(
    val albums: TabContent<AlbumsTabData> = TabContent.Loading,
    val artists: TabContent<List<Artist>> = TabContent.Loading,
    val songs: TabContent<List<Song>> = TabContent.Loading,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    /** Friendly Jellyfin server name shown as the screen title; blank until known. */
    val serverName: StateFlow<String> = sessionManager.session
        .map { it?.serverName?.takeIf(String::isNotBlank).orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        loadAlbums()
        loadArtists()
        loadSongs()
    }

    fun loadAlbums() {
        _state.update { it.copy(albums = TabContent.Loading) }
        viewModelScope.launch {
            runCatching {
                val recent = libraryRepository.getRecentlyAddedAlbums()
                val all = libraryRepository.getAlbums()
                AlbumsTabData(recent, all)
            }.fold(
                onSuccess = { data -> _state.update { it.copy(albums = TabContent.Success(data)) } },
                onFailure = { e -> _state.update { it.copy(albums = TabContent.Error(e.message)) } },
            )
        }
    }

    fun loadArtists() {
        _state.update { it.copy(artists = TabContent.Loading) }
        viewModelScope.launch {
            runCatching { libraryRepository.getArtists() }.fold(
                onSuccess = { d -> _state.update { it.copy(artists = TabContent.Success(d)) } },
                onFailure = { e -> _state.update { it.copy(artists = TabContent.Error(e.message)) } },
            )
        }
    }

    fun loadSongs() {
        _state.update { it.copy(songs = TabContent.Loading) }
        viewModelScope.launch {
            runCatching { libraryRepository.getSongs() }.fold(
                onSuccess = { d -> _state.update { it.copy(songs = TabContent.Success(d)) } },
                onFailure = { e -> _state.update { it.copy(songs = TabContent.Error(e.message)) } },
            )
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int) {
        playerController.play(songs, startIndex)
    }
}
