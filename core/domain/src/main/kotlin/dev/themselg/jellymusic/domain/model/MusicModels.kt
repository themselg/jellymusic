package dev.themselg.jellymusic.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain models. These are decoupled from the Jellyfin SDK DTOs: the data layer maps
 * SDK `BaseItemDto`s into these. Ids are the Jellyfin item ids as plain strings.
 * `artworkUrl` is a fully-built, ready-to-load image URL (see JellyfinUrls).
 */

data class Album(
    val id: String,
    val name: String,
    val artistName: String,
    val artistId: String?,
    val year: Int?,
    val songCount: Int?,
    val artworkUrl: String?,
)

data class Artist(
    val id: String,
    val name: String,
    val albumCount: Int?,
    val artworkUrl: String?,
)

@Serializable
data class Song(
    val id: String,
    val name: String,
    val albumName: String?,
    val albumId: String?,
    val artistName: String,
    val artistId: String?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val durationMs: Long,
    val artworkUrl: String?,
    /** URL the player streams from (Jellyfin universal audio endpoint, includes token). */
    val streamUrl: String,
    /** MIME type of [streamUrl] (e.g. "audio/mpeg"); required for Cast playback. */
    val mimeType: String?,
    val isFavorite: Boolean,
    /**
     * Identifies this track's entry *within a playlist* (distinct from [id], the item id).
     * Only populated when the song was loaded as part of a playlist; needed to remove it.
     */
    val playlistItemId: String? = null,
)

data class Playlist(
    val id: String,
    val name: String,
    val songCount: Int?,
    val artworkUrl: String?,
)

/** Aggregated result of a library search. */
data class SearchResults(
    val artists: List<Artist>,
    val albums: List<Album>,
    val songs: List<Song>,
) {
    val isEmpty: Boolean get() = artists.isEmpty() && albums.isEmpty() && songs.isEmpty()

    companion object {
        val EMPTY = SearchResults(emptyList(), emptyList(), emptyList())
    }
}
