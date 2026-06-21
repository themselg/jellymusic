// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themselg.jellymusic.ui.R
import dev.themselg.jellymusic.ui.components.AlbumCard
import dev.themselg.jellymusic.ui.components.CoverArt
import dev.themselg.jellymusic.ui.components.ErrorState
import dev.themselg.jellymusic.ui.components.LoadingState
import dev.themselg.jellymusic.ui.components.SectionHeader
import dev.themselg.jellymusic.ui.components.SongRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    onBack: () -> Unit,
    onAlbumClick: (String) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (state as? ArtistDetailUiState.Success)?.detail?.artist?.name.orEmpty(),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        when (val s = state) {
            is ArtistDetailUiState.Loading -> LoadingState(Modifier.fillMaxSize())
            is ArtistDetailUiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
            is ArtistDetailUiState.Success -> {
                val detail = s.detail
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                ) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CoverArt(
                                url = detail.artist.artworkUrl,
                                contentDescription = detail.artist.name,
                                cornerRadius = 1000.dp,
                                modifier = Modifier.size(180.dp),
                            )
                            Text(
                                text = detail.artist.name,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    if (detail.albums.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.albums)) }
                        item {
                            // Scrollable album shelf.
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(detail.albums, key = { it.id }) { album ->
                                    AlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(album.id) },
                                        modifier = Modifier.width(150.dp),
                                    )
                                }
                            }
                        }
                    }

                    if (detail.topSongs.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.top_songs)) }
                        itemsIndexed(detail.topSongs, key = { _, song -> song.id }) { index, song ->
                            SongRow(
                                song = song,
                                onClick = { viewModel.playTopSongs(index) },
                            )
                        }
                    }
                }
            }
        }
    }
}
