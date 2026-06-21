// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.player.service.MusicService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PlayerController] implementation backed by a Media3 [MediaController] that connects to
 * [MusicService]. All player interaction happens on the main thread (Media3 requirement),
 * so the controller scope uses [Dispatchers.Main]. Actions issued before the controller is
 * ready are queued and flushed on connection.
 */
@Singleton
class PlayerControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _queue = MutableStateFlow<List<NowPlaying>>(emptyList())
    override val queue: StateFlow<List<NowPlaying>> = _queue.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    /** Actions enqueued while the controller is still connecting. */
    private val pendingActions = ArrayDeque<(MediaController) -> Unit>()

    /** Coroutine that ticks the position while playing so the seek bar advances. */
    private var positionJob: Job? = null

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val c = try {
                    future.get()
                } catch (e: Exception) {
                    // Connection failed (e.g. service unavailable); leave controller null.
                    return@addListener
                }
                controller = c
                c.addListener(playerListener)
                // Reflect any state that already exists, then flush queued actions.
                syncFromController()
                while (pendingActions.isNotEmpty()) {
                    pendingActions.removeFirst().invoke(c)
                }
            },
            // Run on the app main thread — Media3 controllers are single-threaded.
            androidx.core.content.ContextCompat.getMainExecutor(context),
        )
    }

    /** Run [block] now if connected, otherwise queue it until the controller is ready. */
    private inline fun withController(crossinline block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pendingActions.addLast { block(it) }
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            // Recompute the full snapshot on any relevant change; cheap and avoids
            // tracking each individual callback.
            syncFromController()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startPositionTicker() else stopPositionTicker()
        }
    }

    private fun syncFromController() {
        val c = controller ?: return
        _nowPlaying.value = c.currentMediaItem?.toNowPlaying(c.duration)
        _playbackState.value = PlaybackState(
            isPlaying = c.isPlaying,
            isBuffering = c.playbackState == Player.STATE_BUFFERING,
            positionMs = c.currentPosition.coerceAtLeast(0L),
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode.toRepeatMode(),
            hasNext = c.hasNextMediaItem(),
            hasPrevious = c.hasPreviousMediaItem(),
        )
        _queue.value = buildQueue(c)
        if (c.isPlaying) startPositionTicker()
    }

    private fun buildQueue(c: MediaController): List<NowPlaying> {
        val timeline = c.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val window = Timeline.Window()
        return (0 until timeline.windowCount).map { index ->
            timeline.getWindow(index, window)
            // Per-item duration isn't reliably known for non-current windows; report 0 unless current.
            val duration = if (index == c.currentMediaItemIndex) c.duration else 0L
            // window.uid is a stable per-entry id, preserved across moves/removals.
            window.mediaItem.toNowPlaying(duration, queueId = window.uid.toString())
        }
    }

    private fun startPositionTicker() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                val c = controller ?: break
                _playbackState.value = _playbackState.value.copy(
                    positionMs = c.currentPosition.coerceAtLeast(0L),
                )
                if (!c.isPlaying) break
                delay(POSITION_TICK_MS)
            }
        }
    }

    private fun stopPositionTicker() {
        positionJob?.cancel()
        positionJob = null
    }

    // ---- PlayerController API -------------------------------------------------------------

    override fun play(songs: List<Song>, startIndex: Int) = withController { c ->
        val items = MediaItemFactory.toMediaItems(songs)
        if (items.isEmpty()) return@withController
        val index = startIndex.coerceIn(0, items.lastIndex)
        c.setMediaItems(items, index, 0L)
        c.prepare()
        c.play()
    }

    override fun addToQueue(songs: List<Song>) = withController { c ->
        c.addMediaItems(MediaItemFactory.toMediaItems(songs))
    }

    override fun togglePlayPause() = withController { c ->
        if (c.isPlaying) c.pause() else {
            // Re-prepare if playback had ended/idle so play() resumes cleanly.
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    override fun next() = withController { it.seekToNextMediaItem() }

    override fun previous() = withController { it.seekToPreviousMediaItem() }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    override fun seekToQueueItem(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) c.seekTo(index, 0L)
    }

    override fun toggleShuffle() = withController { c ->
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    override fun cycleRepeatMode() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun moveQueueItem(from: Int, to: Int) = withController { c ->
        val count = c.mediaItemCount
        if (from in 0 until count && to in 0 until count && from != to) {
            c.moveMediaItem(from, to)
        }
    }

    override fun removeQueueItem(index: Int) = withController { c ->
        if (index in 0 until c.mediaItemCount) c.removeMediaItem(index)
    }

    override fun clearQueue() = withController { c ->
        c.clearMediaItems()
    }

    /** Tear down the controller; call if/when the singleton's lifetime ends. */
    fun release() {
        stopPositionTicker()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    private companion object {
        const val POSITION_TICK_MS = 500L
    }
}

private fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
    else -> RepeatMode.OFF
}

private fun MediaItem.toNowPlaying(durationMs: Long, queueId: String = ""): NowPlaying {
    val md = mediaMetadata
    return NowPlaying(
        mediaId = mediaId,
        title = md.title?.toString().orEmpty(),
        artist = md.artist?.toString().orEmpty(),
        artworkUrl = md.artworkUri?.toString(),
        // controller.duration is C.TIME_UNSET (a large negative-ish sentinel) before ready.
        durationMs = durationMs.coerceAtLeast(0L),
        queueId = queueId,
    )
}
