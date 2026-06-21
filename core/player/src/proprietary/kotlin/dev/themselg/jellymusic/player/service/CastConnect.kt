// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player.service

import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.Player
import com.google.android.gms.cast.framework.CastContext
import dev.themselg.jellymusic.player.cast.CastBridge

/**
 * proprietary flavor: wire Google Cast. A [CastPlayer] mirrors the queue to the Cast device;
 * when a cast session opens/closes we swap the session's active player between it and the local
 * [localPlayer] via [onPlayerSwap]. Guarded so a device without Play Services just runs local-only.
 */
internal fun MusicService.connectCast(
    localPlayer: Player,
    onPlayerSwap: (Player) -> Unit,
): CastBridge {
    val castContext = runCatching { CastContext.getSharedInstance(this) }.getOrNull()
        ?: return NoopCastBridge
    val castPlayer = CastPlayer(castContext)
    castPlayer.setSessionAvailabilityListener(object : SessionAvailabilityListener {
        override fun onCastSessionAvailable() = onPlayerSwap(castPlayer)
        override fun onCastSessionUnavailable() = onPlayerSwap(localPlayer)
    })
    // Honour a cast session that was already active when the service started.
    if (castPlayer.isCastSessionAvailable) onPlayerSwap(castPlayer)
    return object : CastBridge {
        override fun release() {
            castPlayer.setSessionAvailabilityListener(null)
            castPlayer.release()
        }
    }
}

private object NoopCastBridge : CastBridge {
    override fun release() {}
}
