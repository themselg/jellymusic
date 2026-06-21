package dev.themselg.jellymusic.data.repository

import dev.themselg.jellymusic.data.session.JellyfinUrls
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Artist
import dev.themselg.jellymusic.domain.model.Playlist
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.SortBy
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.ItemSortBy

/**
 * Mapping helpers from Jellyfin SDK [BaseItemDto] to domain models.
 *
 * Ids are surfaced as plain strings (UUID.toString()); artwork/stream URLs are fully
 * built via [JellyfinUrls] so consumers (Coil, the player) can use them directly.
 */

/** Jellyfin stores durations as 100-nanosecond "ticks". */
private const val TICKS_PER_MS = 10_000L

internal fun runTimeTicksToMs(ticks: Long?): Long = (ticks ?: 0L) / TICKS_PER_MS

internal fun SortBy.toItemSortBy(): ItemSortBy = when (this) {
    SortBy.NAME -> ItemSortBy.SORT_NAME
    SortBy.DATE_ADDED -> ItemSortBy.DATE_CREATED
    SortBy.RANDOM -> ItemSortBy.RANDOM
    SortBy.PLAY_COUNT -> ItemSortBy.PLAY_COUNT
}

internal fun BaseItemDto.toAlbum(urls: JellyfinUrls): Album {
    val idString = id.toString()
    // Prefer the structured album-artist pair (gives us an artist id), fall back to strings.
    val primaryArtist = albumArtists?.firstOrNull()
    return Album(
        id = idString,
        name = name.orEmpty(),
        artistName = primaryArtist?.name
            ?: albumArtist
            ?: artists?.firstOrNull()
            ?: "",
        artistId = primaryArtist?.id?.toString(),
        year = productionYear,
        songCount = childCount,
        artworkUrl = urls.imageUrl(idString),
    )
}

internal fun BaseItemDto.toArtist(urls: JellyfinUrls): Artist {
    val idString = id.toString()
    return Artist(
        id = idString,
        name = name.orEmpty(),
        // NOTE: BaseItemDto.albumCount may be null unless requested via fields; childCount is
        // a reasonable fallback for artists. Verify the exact property name in the SDK.
        albumCount = albumCount ?: childCount,
        artworkUrl = urls.imageUrl(idString),
    )
}

internal fun BaseItemDto.toSong(urls: JellyfinUrls): Song {
    val idString = id.toString()
    // NOTE: BaseItemDto.artistItems is List<NameGuidPair>? (track artists with ids); falls
    // back to the plain `artists` string list / albumArtist if absent.
    val primaryArtist = artistItems?.firstOrNull()
    return Song(
        id = idString,
        name = name.orEmpty(),
        albumName = album,
        albumId = albumId?.toString(),
        artistName = primaryArtist?.name
            ?: artists?.firstOrNull()
            ?: albumArtist
            ?: "",
        artistId = primaryArtist?.id?.toString(),
        trackNumber = indexNumber,
        discNumber = parentIndexNumber,
        durationMs = runTimeTicksToMs(runTimeTicks),
        // Artwork: songs typically render their album's primary image (same item id works
        // for tracks; the server falls back to the parent album art when the track has none).
        artworkUrl = urls.imageUrl(idString),
        streamUrl = urls.streamUrl(idString).orEmpty(),
        mimeType = containerToMimeType(container),
        isFavorite = userData?.isFavorite ?: false,
    )
}

/** Map a Jellyfin audio container to a MIME type (needed for Cast). Falls back to audio/mpeg. */
internal fun containerToMimeType(container: String?): String {
    // `container` may be a comma-separated list (e.g. "mp3,flac"); use the first entry.
    val c = container?.substringBefore(',')?.trim()?.lowercase()
    return when (c) {
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "aac", "m4a", "m4b", "mp4", "alac" -> "audio/mp4"
        "ogg", "oga", "opus" -> "audio/ogg"
        "wav", "wave" -> "audio/wav"
        "webm", "webma" -> "audio/webm"
        else -> "audio/mpeg"
    }
}

internal fun BaseItemDto.toPlaylist(urls: JellyfinUrls): Playlist {
    val idString = id.toString()
    return Playlist(
        id = idString,
        name = name.orEmpty(),
        songCount = childCount,
        artworkUrl = urls.imageUrl(idString),
    )
}
