package dev.themselg.jellymusic.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (Navigation Compose + kotlinx.serialization).
 * Top level splits into [Login] and the signed-in graph rooted at [Home].
 */
sealed interface Route {

    @Serializable
    data object Login : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Library : Route

    @Serializable
    data object Profile : Route

    @Serializable
    data object LikedSongs : Route

    @Serializable
    data object Downloads : Route

    @Serializable
    data object Lyrics : Route

    @Serializable
    data class AlbumDetail(val albumId: String) : Route

    @Serializable
    data class ArtistDetail(val artistId: String) : Route

    @Serializable
    data class PlaylistDetail(val playlistId: String) : Route

    @Serializable
    data object NowPlaying : Route

    @Serializable
    data object Queue : Route
}
