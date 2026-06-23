// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player

import dev.themselg.jellymusic.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode { OFF, ALL, ONE }

/** State of the sleep timer, mirrored to the UI. */
sealed interface SleepTimerState {
    /** No timer set. */
    data object Off : SleepTimerState
    /** Counting down; [remainingMs] is the time left before playback pauses. */
    data class Running(val remainingMs: Long) : SleepTimerState
    /** Armed to pause when the current track ends naturally. */
    data object EndOfTrack : SleepTimerState
}

/** The track currently loaded into the player, mirrored from the MediaController. */
data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationMs: Long,
    /**
     * Stable identity of this entry *within the queue* (the Media3 timeline window uid). Unique
     * even when the same track appears twice, and preserved across move/remove — used as the
     * list/reorder key. Empty for the standalone now-playing snapshot.
     */
    val queueId: String = "",
)

/** Snapshot of playback state, mirrored from the MediaController. */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
)

/**
 * UI-facing playback controller. The implementation (player layer) connects to the
 * MediaLibraryService via a Media3 MediaController and reflects its state into these flows.
 */
interface PlayerController {
    val nowPlaying: StateFlow<NowPlaying?>
    val playbackState: StateFlow<PlaybackState>
    val queue: StateFlow<List<NowPlaying>>
    val sleepTimer: StateFlow<SleepTimerState>

    /** Replace the queue with [songs] and start playing from [startIndex]. */
    fun play(songs: List<Song>, startIndex: Int = 0)
    /** Append to the queue without interrupting playback. */
    fun addToQueue(songs: List<Song>)

    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun seekToQueueItem(index: Int)
    fun toggleShuffle()
    fun cycleRepeatMode()

    /** Move a queue entry from [from] to [to] (indices into [queue]). */
    fun moveQueueItem(from: Int, to: Int)
    /** Remove the queue entry at [index]. */
    fun removeQueueItem(index: Int)
    /** Clear the whole queue and stop playback. */
    fun clearQueue()

    /** Start a sleep timer for [durationMs]; pauses playback when it elapses. <= 0 cancels. */
    fun setSleepTimer(durationMs: Long)
    /** Arm the sleep timer to pause when the current track ends. */
    fun setSleepTimerEndOfTrack()
    /** Cancel any active sleep timer. */
    fun cancelSleepTimer()
}
