// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.domain.repository

import dev.themselg.jellymusic.domain.model.Lyrics

interface LyricsRepository {
    /** Fetch lyrics for [songId], or null when the song has none / on failure. */
    suspend fun getLyrics(songId: String): Lyrics?
}
