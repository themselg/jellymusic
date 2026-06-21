// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.library

import androidx.lifecycle.ViewModel
import dev.themselg.jellymusic.data.download.DownloadController
import dev.themselg.jellymusic.data.download.DownloadedItem
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadController: DownloadController,
    private val playerController: PlayerController,
) : ViewModel() {

    val items: StateFlow<List<DownloadedItem>> = downloadController.items

    fun play(songs: List<Song>, index: Int) = playerController.play(songs, index)
}
