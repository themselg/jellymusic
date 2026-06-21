// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import dev.themselg.jellymusic.domain.model.Song

/**
 * Maps domain [Song]s to Media3 [MediaItem]s. The player layer never touches the
 * Jellyfin SDK: everything it needs (stream URL, artwork URL) is already carried on [Song].
 */
object MediaItemFactory {

    fun toMediaItem(song: Song): MediaItem =
        MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(song.streamUrl)
            // Required by CastPlayer to build the remote media info; harmless for local ExoPlayer.
            .setMimeType(song.mimeType ?: "audio/mpeg")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.name)
                    .setArtist(song.artistName)
                    .setAlbumTitle(song.albumName)
                    .setArtworkUri(song.artworkUrl?.toUri())
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

    fun toMediaItems(songs: List<Song>): List<MediaItem> = songs.map(::toMediaItem)
}
