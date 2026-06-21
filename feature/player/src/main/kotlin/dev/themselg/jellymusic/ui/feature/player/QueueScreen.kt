package dev.themselg.jellymusic.ui.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themselg.jellymusic.ui.R
import dev.themselg.jellymusic.player.NowPlaying
import dev.themselg.jellymusic.player.RepeatMode
import dev.themselg.jellymusic.ui.components.CoverArt
import dev.themselg.jellymusic.ui.components.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onBack: () -> Unit,
    viewModel: QueueViewModel = hiltViewModel(),
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val totalMs = queue.sumOf { it.durationMs.coerceAtLeast(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.queue_source)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                windowInsets = WindowInsets(0),
                actions = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 16.dp),
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.song_count, queue.size, queue.size),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (totalMs > 0) {
                            Text(
                                text = formatLongDuration(totalMs),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = viewModel::toggleShuffle) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = stringResource(R.string.cd_shuffle),
                            tint = if (playbackState.shuffle) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = viewModel::cycleRepeatMode) {
                        Icon(
                            imageVector = if (playbackState.repeatMode == RepeatMode.ONE) {
                                Icons.Rounded.RepeatOne
                            } else {
                                Icons.Rounded.Repeat
                            },
                            contentDescription = stringResource(R.string.cd_repeat),
                            tint = if (playbackState.repeatMode == RepeatMode.OFF) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = innerPadding,
        ) {
            itemsIndexed(
                items = queue,
                key = { index, item -> "${index}_${item.mediaId}" },
            ) { index, item ->
                QueueItemRow(
                    item = item,
                    isCurrent = item.mediaId == nowPlaying?.mediaId,
                    onClick = { viewModel.seekToQueueItem(index) },
                )
            }
        }
    }
}

@Composable
private fun QueueItemRow(
    item: NowPlaying,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(12.dp))
        .let {
            if (isCurrent) it.background(MaterialTheme.colorScheme.surfaceContainerHighest) else it
        }
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 8.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            CoverArt(
                url = item.artworkUrl,
                contentDescription = item.title,
                cornerRadius = 8.dp,
                modifier = Modifier.size(48.dp),
            )
            if (isCurrent) {
                Icon(
                    Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (isCurrent) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (item.durationMs > 0) {
            Text(
                text = formatDuration(item.durationMs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** h:mm:ss for long totals, falling back to m:ss. */
private fun formatLongDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
