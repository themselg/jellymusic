package dev.themselg.jellymusic.ui.feature.detail

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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themselg.jellymusic.ui.R
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.ui.components.ErrorState
import dev.themselg.jellymusic.ui.components.LoadingState
import dev.themselg.jellymusic.ui.components.SongRow
import dev.themselg.jellymusic.ui.components.rememberDownloadController
import dev.themselg.jellymusic.ui.feature.playlist.AddToPlaylistSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var addTarget by remember { mutableStateOf<Song?>(null) }
    val downloadController = rememberDownloadController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        (state as? DetailUiState.Success)?.detail?.title.orEmpty(),
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
            is DetailUiState.Loading -> LoadingState(Modifier.fillMaxSize())
            is DetailUiState.Error -> ErrorState(message = s.message, onRetry = viewModel::load)
            is DetailUiState.Success -> {
                val detail = s.detail
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                ) {
                    item {
                        CollectionHeader(
                            title = detail.title,
                            subtitle = detail.subtitle,
                            artworkUrl = detail.artworkUrl,
                            onPlay = { viewModel.play(0) },
                            onShuffle = viewModel::shuffle,
                            onSubtitleClick = viewModel.artistId()?.let { id -> { onArtistClick(id) } },
                            onDownload = { downloadController.download(detail.tracks) },
                        )
                    }
                    itemsIndexed(detail.tracks, key = { _, song -> song.id }) { index, song ->
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
