// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.domain.repository.PlaylistRepository
import dev.themselg.jellymusic.player.NowPlaying
import dev.themselg.jellymusic.player.PlaybackState
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QueueViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val queue: StateFlow<List<NowPlaying>> = playerController.queue
    val nowPlaying: StateFlow<NowPlaying?> = playerController.nowPlaying
    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    fun seekToQueueItem(index: Int) = playerController.seekToQueueItem(index)
    fun toggleShuffle() = playerController.toggleShuffle()
    fun cycleRepeatMode() = playerController.cycleRepeatMode()

    fun moveQueueItem(from: Int, to: Int) = playerController.moveQueueItem(from, to)
    fun removeQueueItem(index: Int) = playerController.removeQueueItem(index)
    fun clearQueue() = playerController.clearQueue()

    /** Save the current queue (its track ids) as a new server playlist. */
    fun saveQueueAsPlaylist(name: String) {
        val songIds = queue.value.map { it.mediaId }
        val trimmed = name.trim()
        if (trimmed.isEmpty() || songIds.isEmpty()) return
        viewModelScope.launch {
            runCatching { playlistRepository.createPlaylist(trimmed, songIds) }
        }
    }
}
