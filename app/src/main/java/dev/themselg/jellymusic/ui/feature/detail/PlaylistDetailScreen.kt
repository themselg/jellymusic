package dev.themselg.jellymusic.ui.feature.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.themselg.jellymusic.ui.components.ErrorState
import dev.themselg.jellymusic.ui.components.LoadingState
import dev.themselg.jellymusic.ui.components.PlaylistNameDialog
import dev.themselg.jellymusic.ui.components.SongRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val title = (state as? DetailUiState.Success)?.detail?.title.orEmpty()

    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_rename)) },
                                onClick = { menuOpen = false; showRename = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                onClick = { menuOpen = false; showDelete = true },
                            )
                        }
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
                val count = detail.tracks.size
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = innerPadding,
                ) {
                    item {
                        CollectionHeader(
                            title = detail.title,
                            subtitle = pluralStringResource(R.plurals.song_count, count, count),
                            artworkUrl = detail.artworkUrl,
                            onPlay = { viewModel.play(0) },
                            onShuffle = viewModel::shuffle,
                        )
                    }
                    itemsIndexed(
                        detail.tracks,
                        key = { _, song -> song.playlistItemId ?: song.id },
                    ) { index, song ->
                        SongRow(
                            song = song,
                            onClick = { viewModel.play(index) },
                            onToggleFavorite = { _ -> viewModel.toggleFavorite(song) },
                            onRemoveFromPlaylist = { viewModel.removeTrack(song) },
                        )
                    }
                }
            }
        }
    }

    if (showRename) {
        PlaylistNameDialog(
            title = stringResource(R.string.action_rename),
            confirmLabel = stringResource(R.string.action_rename),
            initialName = title,
            onConfirm = { name ->
                showRename = false
                viewModel.rename(name)
            },
            onDismiss = { showRename = false },
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_playlist_title)) },
            text = { Text(stringResource(R.string.delete_playlist_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.delete(onBack)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
