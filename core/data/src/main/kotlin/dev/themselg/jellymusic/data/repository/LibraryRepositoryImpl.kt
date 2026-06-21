// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.repository

import dev.themselg.jellymusic.data.session.JellyfinUrls
import dev.themselg.jellymusic.data.session.SessionManagerImpl
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Artist
import dev.themselg.jellymusic.domain.model.Song
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dev.themselg.jellymusic.domain.repository.LibraryRepository
import dev.themselg.jellymusic.domain.repository.SortBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.artistsApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManagerImpl,
    private val urls: JellyfinUrls,
) : LibraryRepository {

    private val imageFields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)

    override fun pagedAlbums(sortBy: SortBy): Flow<PagingData<Album>> = pager { start, limit ->
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
            sortBy = listOf(sortBy.toItemSortBy()),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = imageFields,
            startIndex = start,
            limit = limit,
        )
        result.items.orEmpty().map { it.toAlbum(urls) } to (result.totalRecordCount ?: 0)
    }

    override fun pagedArtists(sortBy: SortBy): Flow<PagingData<Artist>> = pager { start, limit ->
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.artistsApi.getArtists(
            userId = userId,
            sortBy = listOf(sortBy.toItemSortBy()),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = imageFields,
            startIndex = start,
            limit = limit,
        )
        result.items.orEmpty().map { it.toArtist(urls) } to (result.totalRecordCount ?: 0)
    }

    override fun pagedSongs(sortBy: SortBy): Flow<PagingData<Song>> = pager { start, limit ->
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.AUDIO),
            sortBy = listOf(sortBy.toItemSortBy()),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = imageFields,
            startIndex = start,
            limit = limit,
        )
        result.items.orEmpty().map { it.toSong(urls) } to (result.totalRecordCount ?: 0)
    }

    /** Wrap an offset-keyed loader into a paged flow. The SDK call runs on IO. */
    private fun <T : Any> pager(
        load: suspend (startIndex: Int, limit: Int) -> Pair<List<T>, Int>,
    ): Flow<PagingData<T>> = Pager(
        config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
    ) {
        ItemPagingSource { start, limit -> withContext(Dispatchers.IO) { load(start, limit) } }
    }.flow

    override suspend fun getAlbums(sortBy: SortBy): List<Album> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
            sortBy = listOf(sortBy.toItemSortBy()),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = imageFields,
        )
        result.items.orEmpty().map { it.toAlbum(urls) }
    }

    override suspend fun getArtists(sortBy: SortBy): List<Artist> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        // NOTE: artistsApi.getArtists returns BaseItemDtoQueryResult like itemsApi.
        val result by api.artistsApi.getArtists(
            userId = userId,
            sortBy = listOf(sortBy.toItemSortBy()),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = imageFields,
        )
        result.items.orEmpty().map { it.toArtist(urls) }
    }

    override suspend fun getSongs(sortBy: SortBy): List<Song> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.AUDIO),
            sortBy = listOf(sortBy.toItemSortBy()),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = imageFields,
        )
        result.items.orEmpty().map { it.toSong(urls) }
    }

    override suspend fun getRecentlyAddedAlbums(limit: Int): List<Album> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
            sortBy = listOf(ItemSortBy.DATE_CREATED),
            sortOrder = listOf(SortOrder.DESCENDING),
            fields = imageFields,
            limit = limit,
        )
        result.items.orEmpty().map { it.toAlbum(urls) }
    }

    override suspend fun getAlbum(albumId: String): Album = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        // userLibraryApi.getItem fetches a single item with full user data.
        val item by api.userLibraryApi.getItem(
            userId = userId,
            itemId = UUID.fromString(albumId),
        )
        item.toAlbum(urls)
    }

    override suspend fun getAlbumTracks(albumId: String): List<Song> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            parentId = UUID.fromString(albumId),
            includeItemTypes = listOf(BaseItemKind.AUDIO),
            sortBy = listOf(ItemSortBy.PARENT_INDEX_NUMBER, ItemSortBy.INDEX_NUMBER),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = imageFields,
        )
        result.items.orEmpty().map { it.toSong(urls) }
    }

    override suspend fun getArtist(artistId: String): Artist = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val item by api.userLibraryApi.getItem(
            userId = userId,
            itemId = UUID.fromString(artistId),
        )
        item.toArtist(urls)
    }

    override suspend fun getArtistAlbums(artistId: String): List<Album> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            albumArtistIds = listOf(UUID.fromString(artistId)),
            includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
            sortBy = listOf(ItemSortBy.PRODUCTION_YEAR, ItemSortBy.SORT_NAME),
            sortOrder = listOf(SortOrder.DESCENDING),
            fields = imageFields,
        )
        result.items.orEmpty().map { it.toAlbum(urls) }
    }

    override suspend fun getArtistTopSongs(artistId: String, limit: Int): List<Song> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            artistIds = listOf(UUID.fromString(artistId)),
            includeItemTypes = listOf(BaseItemKind.AUDIO),
            sortBy = listOf(ItemSortBy.PLAY_COUNT),
            sortOrder = listOf(SortOrder.DESCENDING),
            fields = imageFields,
            limit = limit,
        )
        result.items.orEmpty().map { it.toSong(urls) }
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
