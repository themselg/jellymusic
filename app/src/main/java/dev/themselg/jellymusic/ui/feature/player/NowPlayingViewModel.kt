package dev.themselg.jellymusic.ui.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.domain.repository.FavoritesRepository
import dev.themselg.jellymusic.player.NowPlaying
import dev.themselg.jellymusic.player.PlaybackState
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlaying?> = playerController.nowPlaying
    val playbackState: StateFlow<PlaybackState> = playerController.playbackState
    val queue: StateFlow<List<NowPlaying>> = playerController.queue

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    init {
        // Refresh the favorite state from the server whenever the current track changes.
        viewModelScope.launch {
            nowPlaying
                .map { it?.mediaId }
                .distinctUntilChanged()
                .collect { id ->
                    _isFavorite.value = id?.let {
                        runCatching { favoritesRepository.isFavorite(it) }.getOrDefault(false)
                    } ?: false
                }
        }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
    fun previous() = playerController.previous()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun seekToQueueItem(index: Int) = playerController.seekToQueueItem(index)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()

    fun toggleFavorite() {
        val id = nowPlaying.value?.mediaId ?: return
        val target = !_isFavorite.value
        _isFavorite.value = target // optimistic
        viewModelScope.launch {
            runCatching { favoritesRepository.setFavorite(id, target) }
                .onFailure { _isFavorite.value = !target } // revert on failure
        }
    }
}
