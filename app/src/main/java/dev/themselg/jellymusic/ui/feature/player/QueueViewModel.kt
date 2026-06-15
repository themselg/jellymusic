package dev.themselg.jellymusic.ui.feature.player

import androidx.lifecycle.ViewModel
import dev.themselg.jellymusic.player.NowPlaying
import dev.themselg.jellymusic.player.PlaybackState
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerController: PlayerController,
) : ViewModel() {

    val queue: StateFlow<List<NowPlaying>> = playerController.queue
    val nowPlaying: StateFlow<NowPlaying?> = playerController.nowPlaying
    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    fun seekToQueueItem(index: Int) = playerController.seekToQueueItem(index)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()
}
