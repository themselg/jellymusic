// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.repository

import dev.themselg.jellymusic.data.session.SessionManagerImpl
import dev.themselg.jellymusic.domain.repository.PlaybackReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.model.api.PlayMethod
import org.jellyfin.sdk.model.api.PlaybackOrder
import org.jellyfin.sdk.model.api.PlaybackProgressInfo
import org.jellyfin.sdk.model.api.PlaybackStartInfo
import org.jellyfin.sdk.model.api.PlaybackStopInfo
import org.jellyfin.sdk.model.api.RepeatMode
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reports playback to Jellyfin via the SDK `playStateApi`. All calls are best-effort and
 * guarded: a reporting failure (offline, no session) must never crash playback.
 */
@Singleton
class PlaybackReporterImpl @Inject constructor(
    private val sessionManager: SessionManagerImpl,
) : PlaybackReporter {

    override suspend fun start(itemId: String, positionMs: Long) = report {
        val api = sessionManager.requireApi()
        api.playStateApi.reportPlaybackStart(
            PlaybackStartInfo(
                itemId = UUID.fromString(itemId),
                positionTicks = positionMs.toTicks(),
                canSeek = true,
                isPaused = false,
                isMuted = false,
                playMethod = PlayMethod.DIRECT_PLAY,
                repeatMode = RepeatMode.REPEAT_NONE,
                playbackOrder = PlaybackOrder.DEFAULT,
            ),
        )
    }

    override suspend fun progress(itemId: String, positionMs: Long, isPaused: Boolean) = report {
        val api = sessionManager.requireApi()
        api.playStateApi.reportPlaybackProgress(
            PlaybackProgressInfo(
                itemId = UUID.fromString(itemId),
                positionTicks = positionMs.toTicks(),
                canSeek = true,
                isPaused = isPaused,
                isMuted = false,
                playMethod = PlayMethod.DIRECT_PLAY,
                repeatMode = RepeatMode.REPEAT_NONE,
                playbackOrder = PlaybackOrder.DEFAULT,
            ),
        )
    }

    override suspend fun stopped(itemId: String, positionMs: Long) = report {
        val api = sessionManager.requireApi()
        api.playStateApi.reportPlaybackStopped(
            PlaybackStopInfo(
                itemId = UUID.fromString(itemId),
                positionTicks = positionMs.toTicks(),
                failed = false,
            ),
        )
    }

    /** Run a reporting call on IO, swallowing any error (no session / network). */
    private suspend inline fun report(crossinline block: suspend () -> Unit) {
        withContext(Dispatchers.IO) { runCatching { block() } }
    }

    private fun Long.toTicks(): Long = coerceAtLeast(0L) * TICKS_PER_MS

    private companion object {
        const val TICKS_PER_MS = 10_000L
    }
}
