package dev.themselg.jellymusic.player.service

import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.android.gms.cast.framework.CastContext
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.themselg.jellymusic.R
import dagger.hilt.android.AndroidEntryPoint

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

    private lateinit var player: ExoPlayer
    private lateinit var mediaLibrarySession: MediaLibrarySession
    private var castPlayer: CastPlayer? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        // Explicit DefaultDataSource.Factory so the ExoPlayer can resolve the http(s)
        // Jellyfin stream URLs carried on each MediaItem.
        val dataSourceFactory = DefaultDataSource.Factory(this)
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

        initCast()

        // TODO(scrobbling): attach a Player.Listener here to report playback progress
        //  (start / progress / stopped) back to Jellyfin's PlaybackInfo / Sessions API.
    }

    /**
     * Wire up Google Cast if Play Services is available. A [CastPlayer] mirrors the queue to a
     * Cast device; when a cast session opens/closes we move playback between it and the local
     * [ExoPlayer] by swapping the session's active player. Guarded so devices without Play
     * Services (or where Cast init fails) simply run local-only.
     */
    private fun initCast() {
        val castContext = runCatching { CastContext.getSharedInstance(this) }.getOrNull() ?: return
        val cp = CastPlayer(castContext)
        cp.setSessionAvailabilityListener(object : SessionAvailabilityListener {
            override fun onCastSessionAvailable() = setCurrentPlayer(cp)
            override fun onCastSessionUnavailable() = setCurrentPlayer(player)
        })
        castPlayer = cp
        // Honour a cast session that was already active when the service started.
        if (cp.isCastSessionAvailable) setCurrentPlayer(cp)
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

        runCatching { oldPlayer.stop() }
        runCatching { oldPlayer.clearMediaItems() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession =
        mediaLibrarySession

    override fun onDestroy() {
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.release()
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
