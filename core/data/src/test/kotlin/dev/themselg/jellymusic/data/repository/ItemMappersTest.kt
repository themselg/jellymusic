// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.repository

import dev.themselg.jellymusic.domain.repository.SortBy
import org.jellyfin.sdk.model.api.ItemSortBy
import kotlin.test.Test
import kotlin.test.assertEquals

class ItemMappersTest {

    @Test
    fun `runTimeTicksToMs converts 100ns ticks to milliseconds`() {
        assertEquals(0L, runTimeTicksToMs(null))
        assertEquals(0L, runTimeTicksToMs(0L))
        assertEquals(1L, runTimeTicksToMs(10_000L))
        assertEquals(1_000L, runTimeTicksToMs(10_000_000L)) // 1 second
    }

    @Test
    fun `toItemSortBy maps every SortBy value`() {
        assertEquals(ItemSortBy.SORT_NAME, SortBy.NAME.toItemSortBy())
        assertEquals(ItemSortBy.DATE_CREATED, SortBy.DATE_ADDED.toItemSortBy())
        assertEquals(ItemSortBy.RANDOM, SortBy.RANDOM.toItemSortBy())
        assertEquals(ItemSortBy.PLAY_COUNT, SortBy.PLAY_COUNT.toItemSortBy())
    }

    @Test
    fun `containerToMimeType maps known containers, is case-insensitive, takes first of a list`() {
        assertEquals("audio/mpeg", containerToMimeType("mp3"))
        assertEquals("audio/flac", containerToMimeType("flac"))
        assertEquals("audio/mp4", containerToMimeType("m4a"))
        assertEquals("audio/ogg", containerToMimeType("opus"))
        assertEquals("audio/wav", containerToMimeType("wav"))
        assertEquals("audio/flac", containerToMimeType("FLAC")) // case-insensitive
        assertEquals("audio/flac", containerToMimeType("flac,mp3")) // first entry of a list
        assertEquals("audio/mpeg", containerToMimeType(null)) // fallback
        assertEquals("audio/mpeg", containerToMimeType("weirdcodec")) // fallback
    }
}
