package dev.themselg.jellymusic.data.repository

import dev.themselg.jellymusic.data.session.JellyfinUrls
import dev.themselg.jellymusic.data.session.SessionManagerImpl
import dev.themselg.jellymusic.domain.model.SearchResults
import dev.themselg.jellymusic.domain.repository.SearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManagerImpl,
    private val urls: JellyfinUrls,
) : SearchRepository {

    override suspend fun search(query: String): SearchResults = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext SearchResults.EMPTY

        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        // Single query across the three music item types, split locally by item type.
        val result by api.itemsApi.getItems(
            userId = userId,
            searchTerm = trimmed,
            recursive = true,
            includeItemTypes = listOf(
                BaseItemKind.MUSIC_ARTIST,
                BaseItemKind.MUSIC_ALBUM,
                BaseItemKind.AUDIO,
            ),
            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
            limit = 50,
        )

        val items = result.items.orEmpty()
        SearchResults(
            artists = items.filter { it.type == BaseItemKind.MUSIC_ARTIST }.map { it.toArtist(urls) },
            albums = items.filter { it.type == BaseItemKind.MUSIC_ALBUM }.map { it.toAlbum(urls) },
            songs = items.filter { it.type == BaseItemKind.AUDIO }.map { it.toSong(urls) },
        )
    }
}
