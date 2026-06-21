// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchResultsTest {

    @Test
    fun `EMPTY is empty`() {
        assertTrue(SearchResults.EMPTY.isEmpty)
    }

    @Test
    fun `is not empty when any list has results`() {
        val song = Song(
            id = "1", name = "Song", albumName = null, albumId = null,
            artistName = "A", artistId = null, trackNumber = null, discNumber = null,
            durationMs = 0, artworkUrl = null, streamUrl = "", mimeType = null, isFavorite = false,
        )
        val results = SearchResults(artists = emptyList(), albums = emptyList(), songs = listOf(song))
        assertFalse(results.isEmpty)
    }
}
