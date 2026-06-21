// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.domain.repository

import androidx.paging.PagingData
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Artist
import dev.themselg.jellymusic.domain.model.Playlist
import dev.themselg.jellymusic.domain.model.SearchResults
import dev.themselg.jellymusic.domain.model.Song
import kotlinx.coroutines.flow.Flow

/**
 * Repository contracts consumed by ViewModels. Implementations live in the data layer
 * (data/repository) and are bound via Hilt. Suspending functions throw on failure;
 * ViewModels wrap calls in runCatching and surface a UiState.
 */

enum class SortBy { NAME, DATE_ADDED, RANDOM, PLAY_COUNT }

interface LibraryRepository {
    suspend fun getAlbums(sortBy: SortBy = SortBy.NAME): List<Album>
    suspend fun getArtists(sortBy: SortBy = SortBy.NAME): List<Artist>
    suspend fun getSongs(sortBy: SortBy = SortBy.NAME): List<Song>
    suspend fun getRecentlyAddedAlbums(limit: Int = 20): List<Album>

    // Paged variants for the large library lists (loaded lazily page by page).
    fun pagedAlbums(sortBy: SortBy = SortBy.NAME): Flow<PagingData<Album>>
    fun pagedArtists(sortBy: SortBy = SortBy.NAME): Flow<PagingData<Artist>>
    fun pagedSongs(sortBy: SortBy = SortBy.NAME): Flow<PagingData<Song>>

    suspend fun getAlbum(albumId: String): Album
    suspend fun getAlbumTracks(albumId: String): List<Song>

    suspend fun getArtist(artistId: String): Artist
    suspend fun getArtistAlbums(artistId: String): List<Album>
    suspend fun getArtistTopSongs(artistId: String, limit: Int = 10): List<Song>
}

interface PlaylistRepository {
    suspend fun getPlaylists(): List<Playlist>
    suspend fun getPlaylist(playlistId: String): Playlist
    suspend fun getPlaylistTracks(playlistId: String): List<Song>

    /** Create a playlist with [name], optionally seeded with [songIds]. Returns its new id. */
    suspend fun createPlaylist(name: String, songIds: List<String> = emptyList()): String
    suspend fun renamePlaylist(playlistId: String, name: String)
    suspend fun deletePlaylist(playlistId: String)
    suspend fun addToPlaylist(playlistId: String, songIds: List<String>)
    /** Remove entries by their playlist-entry id (Song.playlistItemId), not the item id. */
    suspend fun removeFromPlaylist(playlistId: String, entryIds: List<String>)
}

interface FavoritesRepository {
    suspend fun getFavoriteSongs(): List<Song>
    suspend fun getFavoriteAlbums(): List<Album>
    /** Whether [itemId] is currently a favorite for the signed-in user. */
    suspend fun isFavorite(itemId: String): Boolean
    /** Mark/unmark any item (song, album, artist…) as a favorite on the server. */
    suspend fun setFavorite(itemId: String, favorite: Boolean)
}

interface SearchRepository {
    suspend fun search(query: String): SearchResults
}
