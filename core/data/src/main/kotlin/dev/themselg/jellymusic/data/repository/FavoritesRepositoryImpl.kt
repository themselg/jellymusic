// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.repository

import dev.themselg.jellymusic.data.session.JellyfinUrls
import dev.themselg.jellymusic.data.session.SessionManagerImpl
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemFilter
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManagerImpl,
    private val urls: JellyfinUrls,
) : FavoritesRepository {

    override suspend fun getFavoriteSongs(): List<Song> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            filters = listOf(ItemFilter.IS_FAVORITE),
            includeItemTypes = listOf(BaseItemKind.AUDIO),
            sortBy = listOf(ItemSortBy.SORT_NAME),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
        )
        result.items.orEmpty().map { it.toSong(urls) }
    }

    override suspend fun getFavoriteAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            filters = listOf(ItemFilter.IS_FAVORITE),
            includeItemTypes = listOf(BaseItemKind.MUSIC_ALBUM),
            sortBy = listOf(ItemSortBy.SORT_NAME),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
        )
        result.items.orEmpty().map { it.toAlbum(urls) }
    }

    override suspend fun isFavorite(itemId: String): Boolean = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val item by api.userLibraryApi.getItem(userId = userId, itemId = UUID.fromString(itemId))
        item.userData?.isFavorite ?: false
    }

    override suspend fun setFavorite(itemId: String, favorite: Boolean) = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val uuid = UUID.fromString(itemId)
        if (favorite) {
            api.userLibraryApi.markFavoriteItem(userId = userId, itemId = uuid)
        } else {
            api.userLibraryApi.unmarkFavoriteItem(userId = userId, itemId = uuid)
        }
        Unit
    }
}
