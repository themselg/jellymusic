// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui

import androidx.lifecycle.ViewModel
import dev.themselg.jellymusic.player.NowPlaying
import dev.themselg.jellymusic.player.PlaybackState
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Backs the persistent scaffold: exposes the mini-player state and forwards transport
 * controls. Detail/library screens use their own ViewModels but reuse this PlayerController
 * indirectly through the inline controls here.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val playerController: PlayerController,
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlaying?> = playerController.nowPlaying
    val playbackState: StateFlow<PlaybackState> = playerController.playbackState

    fun togglePlayPause() = playerController.togglePlayPause()
    fun next() = playerController.next()
}
