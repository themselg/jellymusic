package dev.themselg.jellymusic.ui.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themselg.jellymusic.ui.R
import dev.themselg.jellymusic.ui.components.ErrorState
import dev.themselg.jellymusic.ui.components.LoadingState
import dev.themselg.jellymusic.ui.components.PlaylistCard
import dev.themselg.jellymusic.ui.components.PlaylistNameDialog
import dev.themselg.jellymusic.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenProfile: () -> Unit,
    onOpenLikedSongs: () -> Unit,
    onOpenDownloads: () -> Unit,
    onPlaylistClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()

    // Playlists can change elsewhere (created in AddToPlaylistSheet, deleted/renamed in a
    // playlist detail). Refresh whenever Biblioteca returns to the foreground.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadPlaylists()
    }

    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.library)) },
            actions = {
                IconButton(onClick = onOpenProfile) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        contentDescription = stringResource(R.string.profile),
                    )
                }
            },
            windowInsets = WindowInsets(0),
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 140.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "__liked__", span = { GridItemSpanFull() }) {
                ShortcutRow(
                    icon = Icons.Rounded.Favorite,
                    label = stringResource(R.string.liked_songs),
                    onClick = onOpenLikedSongs,
                )
            }
            item(key = "__downloads__", span = { GridItemSpanFull() }) {
                ShortcutRow(
                    icon = Icons.Rounded.Download,
                    label = stringResource(R.string.downloads),
                    onClick = onOpenDownloads,
                )
            }
            item(key = "__header__", span = { GridItemSpanFull() }) {
                SectionHeader(stringResource(R.string.tab_playlists))
            }

            when (val state = playlists) {
                is PlaylistsState.Loading -> item(span = { GridItemSpanFull() }) { LoadingState() }
                is PlaylistsState.Error -> item(span = { GridItemSpanFull() }) {
                    ErrorState(message = state.message, onRetry = viewModel::loadPlaylists)
                }
                is PlaylistsState.Success -> {
                    item(key = "__create__") {
                        CreatePlaylistCard(onClick = { showCreate = true })
                    }
                    items(state.playlists, key = { it.id }) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onPlaylistClick(playlist.id) })
                    }
                }
            }
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            title = stringResource(R.string.create_playlist),
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = { name ->
                showCreate = false
                viewModel.createPlaylist(name)
            },
            onDismiss = { showCreate = false },
        )
    }
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun CreatePlaylistCard(onClick: () -> Unit) {
    Card(onClick = onClick) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
            Text(
                text = stringResource(R.string.create_playlist),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

private fun LazyGridItemSpanScope.GridItemSpanFull() = GridItemSpan(maxLineSpan)
