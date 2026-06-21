package dev.themselg.jellymusic.ui.feature.playlist

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

@HiltViewModel
class AddToPlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { playlistRepository.getPlaylists() }
                .onSuccess { _playlists.value = it }
        }
    }

    /** Add [songIds] to an existing playlist, then invoke [onDone] on the main thread. */
    fun addTo(playlistId: String, songIds: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { playlistRepository.addToPlaylist(playlistId, songIds) }
            onDone()
        }
    }

    /** Create a playlist named [name] seeded with [songIds], then invoke [onDone]. */
    fun createWith(name: String, songIds: List<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { playlistRepository.createPlaylist(name, songIds) }
            onDone()
        }
    }
}
