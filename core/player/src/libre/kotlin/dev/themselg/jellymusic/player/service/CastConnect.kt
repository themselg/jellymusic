package dev.themselg.jellymusic.player.service

import androidx.media3.common.Player
import dev.themselg.jellymusic.player.cast.CastBridge

/** libre flavor: no Cast. Returns a no-op bridge; playback stays local. */
@Suppress("UNUSED_PARAMETER")
internal fun MusicService.connectCast(
    localPlayer: Player,
    onPlayerSwap: (Player) -> Unit,
): CastBridge = object : CastBridge {
    override fun release() {}
}
