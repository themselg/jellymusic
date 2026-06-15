package dev.themselg.jellymusic.ui.feature.library

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themselg.jellymusic.R
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.ui.components.ErrorState
import dev.themselg.jellymusic.ui.components.LoadingState
import dev.themselg.jellymusic.ui.components.SongRow
import dev.themselg.jellymusic.ui.feature.detail.CollectionHeader
import dev.themselg.jellymusic.ui.feature.playlist.AddToPlaylistSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedSongsScreen(
    onBack: () -> Unit,
    viewModel: LikedSongsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var addTarget by remember { mutableStateOf<Song?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.liked_songs)) },
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
            is LikedSongsState.Loading -> LoadingState(Modifier.fillMaxSize())
            is LikedSongsState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
            is LikedSongsState.Success -> {
                val songs = s.songs
                val count = songs.size
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                ) {
                    item {
                        CollectionHeader(
                            title = stringResource(R.string.liked_songs),
                            subtitle = pluralStringResource(R.plurals.song_count, count, count),
                            artworkUrl = null,
                            onPlay = { viewModel.play(0) },
                            onShuffle = viewModel::shuffle,
                        )
                    }
                    itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                        SongRow(
                            song = song,
                            onClick = { viewModel.play(index) },
                            onToggleFavorite = { _ -> viewModel.toggleFavorite(song) },
                            onAddToPlaylist = { addTarget = song },
                        )
                    }
                }
            }
        }
    }

    addTarget?.let { song ->
        AddToPlaylistSheet(songIds = listOf(song.id), onDismiss = { addTarget = null })
    }
}
