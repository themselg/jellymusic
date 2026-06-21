// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import dev.themselg.jellymusic.domain.repository.PlaybackReporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Bridges Media3 playback events to the Jellyfin [PlaybackReporter] (scrobbling): start on a
 * new track, periodic progress + on play/pause, stopped on track change / end / release.
 *
 * [attach] follows the active session player so it keeps reporting across the local ↔ Cast
 * swap; [release] reports a final stop. All reporter calls are best-effort (the impl guards
 * failures), so this never affects playback.
 */
internal class Scrobbler(
    private val reporter: PlaybackReporter,
    private val scope: CoroutineScope,
) {
    private var player: Player? = null
    private var reportingId: String? = null
    private var lastPositionMs: Long = 0L
    private var progressJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            switchTo(mediaItem?.mediaId)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            reportProgress()
            if (isPlaying) startTicker() else stopTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) stopCurrent()
        }
    }

    /** Attach to [newPlayer], moving the listener off any previous player. */
    fun attach(newPlayer: Player) {
        if (player === newPlayer) return
        player?.removeListener(listener)
        stopTicker()
        player = newPlayer
        newPlayer.addListener(listener)
        switchTo(newPlayer.currentMediaItem?.mediaId)
        if (newPlayer.isPlaying) startTicker()
    }

    fun release() {
        stopCurrent()
        player?.removeListener(listener)
        stopTicker()
        player = null
    }

    /** Report stop for the outgoing item and start for the incoming one. No-op if unchanged. */
    private fun switchTo(newId: String?) {
        if (newId == reportingId) return
        reportingId?.let { prev -> scope.launch { reporter.stopped(prev, lastPositionMs) } }
        reportingId = newId
        lastPositionMs = 0L
        newId?.let { id -> scope.launch { reporter.start(id, position()) } }
    }

    private fun reportProgress() {
        val id = reportingId ?: return
        val pos = position()
        lastPositionMs = pos
        val paused = player?.isPlaying == false
        scope.launch { reporter.progress(id, pos, paused) }
    }

    private fun stopCurrent() {
        val id = reportingId ?: return
        reportingId = null
        val pos = position().takeIf { it > 0 } ?: lastPositionMs
        scope.launch { reporter.stopped(id, pos) }
    }

    private fun position(): Long = player?.currentPosition?.coerceAtLeast(0L) ?: 0L

    private fun startTicker() {
        stopTicker()
        progressJob = scope.launch {
            while (isActive) {
                delay(PROGRESS_INTERVAL_MS)
                reportProgress()
            }
        }
    }

    private fun stopTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 10_000L
    }
}
