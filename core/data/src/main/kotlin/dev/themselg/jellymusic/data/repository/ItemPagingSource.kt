// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Offset-keyed [PagingSource] over a Jellyfin list endpoint. [loadPage] fetches the items at
 * [startIndex] (up to `limit`) and returns them paired with the endpoint's total record count,
 * which is used to decide when there are no more pages.
 */
internal class ItemPagingSource<T : Any>(
    private val loadPage: suspend (startIndex: Int, limit: Int) -> Pair<List<T>, Int>,
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val start = params.key ?: 0
        return try {
            val (items, total) = loadPage(start, params.loadSize)
            val nextStart = start + items.size
            LoadResult.Page(
                data = items,
                prevKey = if (start == 0) null else (start - params.loadSize).coerceAtLeast(0),
                nextKey = if (items.isEmpty() || nextStart >= total) null else nextStart,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { anchor ->
            val closest = state.closestPageToPosition(anchor)
            closest?.prevKey?.plus(state.config.pageSize)
                ?: closest?.nextKey?.minus(state.config.pageSize)
        }
}
