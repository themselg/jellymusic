// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.session

import kotlinx.coroutines.flow.StateFlow

/** Persisted identity of an authenticated Jellyfin connection. */
data class JellyfinSession(
    val serverUrl: String,
    val userId: String,
    val userName: String,
    val accessToken: String,
    val deviceId: String,
    /** Friendly server name reported by Jellyfin (e.g. "Homeserver"); may be blank. */
    val serverName: String = "",
)

/**
 * Owns the authenticated connection lifecycle. The implementation (data layer) wires the
 * Jellyfin SDK `ApiClient`, persists credentials encrypted, and exposes the current session.
 */
interface SessionManager {
    /** Null until a user signs in; restored from encrypted storage on startup. */
    val session: StateFlow<JellyfinSession?>

    /** Authenticate against [serverUrl] with username/password. Throws on failure. */
    suspend fun signIn(serverUrl: String, username: String, password: String)

    fun signOut()
}

/**
 * Builds ready-to-load URLs for the current session. Used by Coil (artwork) and the
 * player (audio stream). Returns null when there is no active session.
 */
interface JellyfinUrls {
    /** Primary image for any item, sized for the request. Null if no session. */
    fun imageUrl(itemId: String, maxWidth: Int = 512): String?

    /** Primary image for the signed-in user, sized for the request. Null if no session. */
    fun userImageUrl(maxWidth: Int = 256): String?

    /** Universal audio stream URL for a song (includes api_key + device id). Null if no session. */
    fun streamUrl(songId: String): String?
}
