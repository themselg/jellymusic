// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.domain.repository

/**
 * Reports playback to the Jellyfin server so it can update play counts, "last played",
 * resume positions and the live "now playing" session. Implemented in the data layer; the
 * player layer depends only on this interface (no compile dependency on data).
 *
 * [itemId] is the Jellyfin item id (the same string carried as the MediaItem mediaId);
 * [positionMs] is the playback position in milliseconds.
 */
interface PlaybackReporter {
    suspend fun start(itemId: String, positionMs: Long)
    suspend fun progress(itemId: String, positionMs: Long, isPaused: Boolean)
    suspend fun stopped(itemId: String, positionMs: Long)
}
