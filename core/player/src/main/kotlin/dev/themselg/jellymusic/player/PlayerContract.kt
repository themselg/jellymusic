package dev.themselg.jellymusic.player

import dev.themselg.jellymusic.domain.model.Song
import kotlinx.coroutines.flow.StateFlow

enum class RepeatMode { OFF, ALL, ONE }

/** The track currently loaded into the player, mirrored from the MediaController. */
data class NowPlaying(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationMs: Long,
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
}
