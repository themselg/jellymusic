// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player.service

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.themselg.jellymusic.player.R
import dev.themselg.jellymusic.player.Scrobbler
import dev.themselg.jellymusic.player.cast.CastBridge
import dev.themselg.jellymusic.domain.repository.PlaybackReporter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * The Media3 [MediaLibraryService] that owns the actual [ExoPlayer]. The UI never talks to
 * this directly — it connects via a MediaController (see PlayerControllerImpl). Declared in
 * the manifest as `.player.service.MusicService` with foregroundServiceType mediaPlayback.
 *
 * For MVP the browse tree is intentionally minimal (an empty root) so the structure is in
 * place for Android Auto / Wear browsing to be filled in later without reworking the service.
 */
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject lateinit var downloadCache: SimpleCache
    @Inject lateinit var playbackReporter: PlaybackReporter

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private var castBridge: CastBridge? = null

    // Reports playback to Jellyfin (scrobbling). Main.immediate so listener callbacks (already
    // on the main thread) dispatch reporting without an extra hop; the reporter does its own IO.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var scrobbler: Scrobbler

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Read-only CacheDataSource over the shared download cache: a song that's been
        // downloaded plays from disk (offline); otherwise it streams from Jellyfin. Write is
        // disabled here so only the DownloadManager populates the cache.
        val upstream = DefaultDataSource.Factory(this)
        val dataSourceFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            // handleAudioFocus=true so the player pauses/ducks for other apps.
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaLibrarySession = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .build()

        // Media-style foreground notification. MediaLibraryService promotes the service to
        // foreground automatically once a session exists; we only customise the channel name.
        // NOTE (media3 1.6.x): DefaultMediaNotificationProvider.Builder exposes
        // setChannelName(@StringRes) and setChannelId(String). Verify these names if the
        // build breaks — older snapshots named the resource setter slightly differently.
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.playback_channel_name)
            .build()
        setMediaNotificationProvider(notificationProvider)

        // Cast is provided per build flavor: the proprietary flavor wires a CastPlayer that
        // swaps with the local player on session changes; the libre flavor returns a no-op.
        castBridge = connectCast(player) { setCurrentPlayer(it) }

        // Scrobbling: report start/progress/stopped to Jellyfin. Attached to the local player
        // now and re-attached in setCurrentPlayer so it keeps reporting after a Cast swap.
        scrobbler = Scrobbler(playbackReporter, serviceScope)
        scrobbler.attach(player)
    }

    /**
     * Move queue + position + play state to [newPlayer] and make it the session's player.
     *
     * Every interaction with the outgoing player is guarded: this runs from the Cast session
     * callbacks, and reading from / stopping a player that is mid-disconnect can throw. Since the
     * service shares its process with the UI, an uncaught exception here would crash the whole
     * app (this is what closed the app when tapping "Stop casting").
     */
    private fun setCurrentPlayer(newPlayer: Player) {
        val oldPlayer = mediaLibrarySession.player
        if (oldPlayer === newPlayer) return

        runCatching {
            val items = (0 until oldPlayer.mediaItemCount).map(oldPlayer::getMediaItemAt)
            if (items.isNotEmpty()) {
                newPlayer.setMediaItems(
                    items,
                    oldPlayer.currentMediaItemIndex.coerceAtLeast(0),
                    oldPlayer.currentPosition.coerceAtLeast(0),
                )
                newPlayer.playWhenReady = oldPlayer.playWhenReady
                newPlayer.prepare()
            }
        }

        mediaLibrarySession.player = newPlayer

        // Keep scrobbling against the now-active player (local ↔ Cast).
        scrobbler.attach(newPlayer)

        runCatching { oldPlayer.stop() }
        runCatching { oldPlayer.clearMediaItems() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaLibrarySession

    override fun onDestroy() {
        scrobbler.release()
        serviceScope.cancel()
        castBridge?.release()
        mediaLibrarySession.release()
        player.release()
        super.onDestroy()
    }

    /**
     * Minimal library callback. Connections are accepted with the default available
     * commands; the browse tree currently exposes only an empty root so that Android Auto
     * can be implemented later by fleshing out [onGetLibraryRoot] / [onGetChildren].
     */
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            // Empty root for MVP. ROOT_ID kept so children can be added later.
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build(),
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            // No browsable children yet.
            return Futures.immediateFuture(
                LibraryResult.ofItemList(ImmutableList.of(), params),
            )
        }
    }

    private companion object {
        const val ROOT_ID = "root"
    }
}
