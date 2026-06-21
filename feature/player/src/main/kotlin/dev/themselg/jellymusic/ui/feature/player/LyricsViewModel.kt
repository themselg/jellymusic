// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.domain.model.Lyrics
import dev.themselg.jellymusic.domain.repository.LyricsRepository
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LyricsUiState {
    data object Loading : LyricsUiState
    data object Empty : LyricsUiState
    data class Loaded(val lyrics: Lyrics) : LyricsUiState
}

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val lyricsRepository: LyricsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LyricsUiState>(LyricsUiState.Loading)
    val state: StateFlow<LyricsUiState> = _state.asStateFlow()

    val positionMs: StateFlow<Long> = playerController.playbackState
        .map { it.positionMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val isPlaying: StateFlow<Boolean> = playerController.playbackState
        .map { it.isPlaying }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Index of the last synced line whose [startMs] has elapsed at the current position,
     * or -1 when nothing is active / the lyrics aren't synced.
     */
    val activeLineIndex: StateFlow<Int> = combine(
        state,
        playerController.playbackState.map { it.positionMs }.distinctUntilChanged(),
    ) { lyricsState, position ->
        val lyrics = (lyricsState as? LyricsUiState.Loaded)?.lyrics
        if (lyrics == null || !lyrics.synced) return@combine -1
        lyrics.lines.indexOfLast { line -> line.startMs?.let { it <= position } == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1)

    init {
        // Reload lyrics whenever the current track changes.
        viewModelScope.launch {
            playerController.nowPlaying
                .map { it?.mediaId }
                .distinctUntilChanged()
                .collect { id ->
                    if (id == null) {
                        _state.value = LyricsUiState.Empty
                        return@collect
                    }
                    _state.value = LyricsUiState.Loading
                    val lyrics = lyricsRepository.getLyrics(id)
                    _state.value = lyrics
                        ?.takeIf { it.lines.isNotEmpty() }
                        ?.let { LyricsUiState.Loaded(it) }
                        ?: LyricsUiState.Empty
                }
        }
    }

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
}
