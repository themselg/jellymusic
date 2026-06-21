// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.themselg.jellymusic.ui.R
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Artist
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.SortBy
import dev.themselg.jellymusic.ui.components.AlbumCard
import dev.themselg.jellymusic.ui.components.ArtistCard
import dev.themselg.jellymusic.ui.components.EmptyState
import dev.themselg.jellymusic.ui.components.ErrorState
import dev.themselg.jellymusic.ui.components.LoadingState
import dev.themselg.jellymusic.ui.components.SectionHeader
import dev.themselg.jellymusic.ui.components.SongRow
import dev.themselg.jellymusic.ui.feature.playlist.AddToPlaylistSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onOpenProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val serverName by viewModel.serverName.collectAsStateWithLifecycle()
    val tabs = HomeTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    val title = serverName.ifBlank { stringResource(R.string.app_name) }
    val userImageUrl by viewModel.userImageUrl.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // windowInsets = 0: the enclosing Scaffold already pads for the status bar, so the
        // app bar must not add it again (that caused the large empty gap at the top).
        val albumsSort by viewModel.albumsSort.collectAsStateWithLifecycle()
        val artistsSort by viewModel.artistsSort.collectAsStateWithLifecycle()
        val songsSort by viewModel.songsSort.collectAsStateWithLifecycle()
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold) },
            actions = {
                // Sort applies to whichever tab is showing.
                val (current, onSelect) = when (tabs[pagerState.currentPage]) {
                    HomeTab.ALBUMS -> albumsSort to viewModel::setAlbumsSort
                    HomeTab.ARTISTS -> artistsSort to viewModel::setArtistsSort
                    HomeTab.SONGS -> songsSort to viewModel::setSongsSort
                }
                SortMenuButton(current = current, onSelect = onSelect)
                ProfileAvatarButton(imageUrl = userImageUrl, onClick = onOpenProfile)
            },
            windowInsets = WindowInsets(0),
        )
        // Only three tabs now (Playlists/Favorites moved to Library), so a full-width TabRow
        // spreads them evenly instead of bunching them on the left like a ScrollableTabRow.
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(stringResource(tab.titleRes())) },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (tabs[page]) {
                HomeTab.ALBUMS -> AlbumsTab(
                    albums = viewModel.albums.collectAsLazyPagingItems(),
                    recentlyAdded = viewModel.recentlyAdded.collectAsStateWithLifecycle().value,
                    onAlbumClick = onAlbumClick,
                )
                HomeTab.ARTISTS -> ArtistsTab(
                    artists = viewModel.artists.collectAsLazyPagingItems(),
                    onArtistClick = onArtistClick,
                )
                HomeTab.SONGS -> SongsTab(
                    songs = viewModel.songs.collectAsLazyPagingItems(),
                    onPlay = viewModel::playSongs,
                )
            }
        }
    }
}

private fun HomeTab.titleRes(): Int = when (this) {
    HomeTab.ALBUMS -> R.string.tab_albums
    HomeTab.ARTISTS -> R.string.tab_artists
    HomeTab.SONGS -> R.string.tab_songs
}

/** Circular profile button: the signed-in user's Jellyfin avatar, falling back to an icon. */
@Composable
private fun ProfileAvatarButton(imageUrl: String?, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.AccountCircle,
                contentDescription = stringResource(R.string.profile),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

/** Sort affordance: a menu of the [SortBy] options, with a check on the active one. */
@Composable
private fun SortMenuButton(current: SortBy, onSelect: (SortBy) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = listOf(
        SortBy.NAME to R.string.sort_name,
        SortBy.DATE_ADDED to R.string.sort_date_added,
        SortBy.PLAY_COUNT to R.string.sort_play_count,
        SortBy.RANDOM to R.string.sort_random,
    )
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = stringResource(R.string.sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (sortBy, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        open = false
                        onSelect(sortBy)
                    },
                    leadingIcon = if (sortBy == current) {
                        { Icon(Icons.Rounded.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: LazyPagingItems<Album>,
    recentlyAdded: List<Album>,
    onAlbumClick: (String) -> Unit,
) {
    PagedContent(albums, emptyRes = R.string.empty_albums) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (recentlyAdded.isNotEmpty()) {
                item(span = { GridItemSpanFull() }) {
                    Column {
                        SectionHeader(stringResource(R.string.recently_added))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(recentlyAdded, key = { it.id }) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album.id) },
                                    modifier = Modifier.width(150.dp),
                                )
                            }
                        }
                    }
                }
                item(span = { GridItemSpanFull() }) {
                    SectionHeader(stringResource(R.string.tab_albums))
                }
            }
            items(count = albums.itemCount, key = albums.itemKey { it.id }) { index ->
                albums[index]?.let { album ->
                    AlbumCard(album = album, onClick = { onAlbumClick(album.id) })
                }
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: LazyPagingItems<Artist>,
    onArtistClick: (String) -> Unit,
) {
    PagedContent(artists, emptyRes = R.string.empty_artists) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 130.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(count = artists.itemCount, key = artists.itemKey { it.id }) { index ->
                artists[index]?.let { artist ->
                    ArtistCard(artist = artist, onClick = { onArtistClick(artist.id) })
                }
            }
        }
    }
}

@Composable
private fun SongsTab(
    songs: LazyPagingItems<Song>,
    onPlay: (List<Song>, Int) -> Unit,
) {
    var addTarget by remember { mutableStateOf<Song?>(null) }
    PagedContent(songs, emptyRes = R.string.empty_songs) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(count = songs.itemCount, key = songs.itemKey { it.id }) { index ->
                songs[index]?.let { song ->
                    SongRow(
                        song = song,
                        // Play the currently-loaded page snapshot starting at this row.
                        onClick = { onPlay(songs.itemSnapshotList.items, index) },
                        onAddToPlaylist = { addTarget = song },
                    )
                }
            }
        }
    }
    addTarget?.let { song ->
        AddToPlaylistSheet(songIds = listOf(song.id), onDismiss = { addTarget = null })
    }
}

/** Loading/Error/Empty wrapper around a paged list, driven by the refresh load state. */
@Composable
private fun <T : Any> PagedContent(
    items: LazyPagingItems<T>,
    emptyRes: Int,
    content: @Composable () -> Unit,
) {
    val refresh = items.loadState.refresh
    when {
        refresh is LoadState.Loading && items.itemCount == 0 -> LoadingState()
        refresh is LoadState.Error && items.itemCount == 0 ->
            ErrorState(message = refresh.error.message, onRetry = { items.retry() })
        items.itemCount == 0 -> EmptyState(message = stringResource(emptyRes))
        else -> content()
    }
}

// NOTE: GridItemSpan helper — full-row span inside LazyVerticalGrid.
private fun androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.GridItemSpanFull() =
    androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan)
