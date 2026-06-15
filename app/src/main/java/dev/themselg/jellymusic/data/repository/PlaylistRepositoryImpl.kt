package dev.themselg.jellymusic.data.repository

import dev.themselg.jellymusic.data.session.JellyfinUrls
import dev.themselg.jellymusic.data.session.SessionManagerImpl
import dev.themselg.jellymusic.domain.model.Playlist
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.libraryApi
import org.jellyfin.sdk.api.client.extensions.playlistsApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CreatePlaylistDto
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.MediaType
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.UpdatePlaylistDto
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManagerImpl,
    private val urls: JellyfinUrls,
) : PlaylistRepository {

    override suspend fun getPlaylists(): List<Playlist> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.itemsApi.getItems(
            userId = userId,
            recursive = true,
            includeItemTypes = listOf(BaseItemKind.PLAYLIST),
            sortBy = listOf(ItemSortBy.SORT_NAME),
            sortOrder = listOf(SortOrder.ASCENDING),
            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
        )
        result.items.orEmpty().map { it.toPlaylist(urls) }
    }

    override suspend fun getPlaylist(playlistId: String): Playlist = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val item by api.userLibraryApi.getItem(
            userId = userId,
            itemId = UUID.fromString(playlistId),
        )
        item.toPlaylist(urls)
    }

    override suspend fun getPlaylistTracks(playlistId: String): List<Song> = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        val result by api.playlistsApi.getPlaylistItems(
            playlistId = UUID.fromString(playlistId),
            userId = userId,
            fields = listOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO),
        )
        // Carry each track's playlist-entry id so it can be removed later.
        result.items.orEmpty().map { it.toSong(urls).copy(playlistItemId = it.playlistItemId) }
    }

    override suspend fun createPlaylist(name: String, songIds: List<String>): String =
        withContext(Dispatchers.IO) {
            val api = sessionManager.requireApi()
            val userId = sessionManager.requireUserId()
            val result by api.playlistsApi.createPlaylist(
                data = CreatePlaylistDto(
                    name = name,
                    ids = songIds.map(UUID::fromString),
                    userId = userId,
                    mediaType = MediaType.AUDIO,
                    users = emptyList(),
                    isPublic = false,
                ),
            )
            result.id?.toString() ?: error("Playlist created but no id was returned")
        }

    override suspend fun renamePlaylist(playlistId: String, name: String) = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        api.playlistsApi.updatePlaylist(
            playlistId = UUID.fromString(playlistId),
            data = UpdatePlaylistDto(name = name),
        )
        Unit
    }

    override suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        // A playlist is an item; deleting it uses the generic library delete endpoint.
        api.libraryApi.deleteItem(itemId = UUID.fromString(playlistId))
        Unit
    }

    override suspend fun addToPlaylist(playlistId: String, songIds: List<String>) = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        val userId = sessionManager.requireUserId()
        api.playlistsApi.addItemToPlaylist(
            playlistId = UUID.fromString(playlistId),
            ids = songIds.map(UUID::fromString),
            userId = userId,
        )
        Unit
    }

    override suspend fun removeFromPlaylist(playlistId: String, entryIds: List<String>) = withContext(Dispatchers.IO) {
        val api = sessionManager.requireApi()
        // NOTE: removeItemFromPlaylist takes the playlistId as a String (unlike add, which takes UUID).
        api.playlistsApi.removeItemFromPlaylist(
            playlistId = playlistId,
            entryIds = entryIds,
        )
        Unit
    }
}
