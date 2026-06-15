package dev.themselg.jellymusic.ui.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themselg.jellymusic.R
import dev.themselg.jellymusic.domain.model.SearchResults
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.ui.components.AlbumCard
import dev.themselg.jellymusic.ui.components.ArtistCard
import dev.themselg.jellymusic.ui.components.EmptyState
import dev.themselg.jellymusic.ui.components.ErrorState
import dev.themselg.jellymusic.ui.components.LoadingState
import dev.themselg.jellymusic.ui.components.SectionHeader
import dev.themselg.jellymusic.ui.components.SongRow
import dev.themselg.jellymusic.ui.feature.playlist.AddToPlaylistSheet

@Composable
fun SearchScreen(
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Open the keyboard automatically when arriving on the Search screen.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focusRequester),
        )
        when (val s = state) {
            is SearchUiState.Idle -> EmptyState(message = stringResource(R.string.search_prompt))
            is SearchUiState.Loading -> LoadingState()
            is SearchUiState.Error -> ErrorState(message = s.message)
            is SearchUiState.Success -> {
                if (s.results.isEmpty) {
                    EmptyState(message = stringResource(R.string.search_no_results))
                } else {
                    SearchResultsList(
                        results = s.results,
                        onAlbumClick = onAlbumClick,
                        onArtistClick = onArtistClick,
                        onPlaySong = viewModel::playSongs,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    results: SearchResults,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaySong: (List<Song>, Int) -> Unit,
) {
    var addTarget by remember { mutableStateOf<Song?>(null) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (results.artists.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.tab_artists)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results.artists, key = { it.id }) { artist ->
                        ArtistCard(
                            artist = artist,
                            onClick = { onArtistClick(artist.id) },
                            modifier = Modifier.width(130.dp),
                        )
                    }
                }
            }
        }
        if (results.albums.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.tab_albums)) }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results.albums, key = { it.id }) { album ->
                        AlbumCard(
                            album = album,
                            onClick = { onAlbumClick(album.id) },
                            modifier = Modifier.width(150.dp),
                        )
                    }
                }
            }
        }
        if (results.songs.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.tab_songs)) }
            itemsIndexed(results.songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song = song,
                    onClick = { onPlaySong(results.songs, index) },
                    onAddToPlaylist = { addTarget = song },
                )
            }
        }
    }
    addTarget?.let { song ->
        AddToPlaylistSheet(songIds = listOf(song.id), onDismiss = { addTarget = null })
    }
}
