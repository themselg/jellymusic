// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themselg.jellymusic.ui.R
import dev.themselg.jellymusic.player.NowPlaying
import dev.themselg.jellymusic.player.PlaybackState
import dev.themselg.jellymusic.player.RepeatMode
import dev.themselg.jellymusic.ui.components.CastButton
import dev.themselg.jellymusic.ui.components.CoverArt
import dev.themselg.jellymusic.ui.components.formatDuration
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.now_playing),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.queue_source),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    CastButton(modifier = Modifier.padding(end = 8.dp).size(40.dp))
                },
                // Outer Scaffold already pads for the status bar; don't add it twice.
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        val track = nowPlaying
        if (track == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.empty_generic))
            }
        } else {
            NowPlayingContent(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                track = track,
                playbackState = playbackState,
                isFavorite = isFavorite,
                onTogglePlayPause = viewModel::togglePlayPause,
                onNext = viewModel::next,
                onPrevious = viewModel::previous,
                onSeekTo = viewModel::seekTo,
                onToggleShuffle = viewModel::toggleShuffle,
                onCycleRepeat = viewModel::cycleRepeatMode,
                onToggleFavorite = viewModel::toggleFavorite,
                onOpenQueue = onOpenQueue,
                onOpenLyrics = onOpenLyrics,
            )
        }
    }
}

@Composable
private fun NowPlayingContent(
    track: NowPlaying,
    playbackState: PlaybackState,
    isFavorite: Boolean,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverArt(
            url = track.artworkUrl,
            contentDescription = track.title,
            cornerRadius = 16.dp,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
        )

        Spacer(Modifier.weight(1f))

        // Title + artist (left aligned, fills width).
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        WavySeekBar(
            positionMs = playbackState.positionMs,
            durationMs = track.durationMs,
            playing = playbackState.isPlaying,
            onSeekTo = onSeekTo,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )

        // Transport row.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCycleRepeat) {
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
            IconButton(onClick = onPrevious, enabled = playbackState.hasPrevious) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    modifier = Modifier.size(38.dp),
                )
            }
            PlayPauseButton(isPlaying = playbackState.isPlaying, onClick = onTogglePlayPause)
            IconButton(onClick = onNext, enabled = playbackState.hasNext) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    modifier = Modifier.size(38.dp),
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom: open the queue / lyrics as their own screens.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomActionButton(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = stringResource(R.string.queue),
                onClick = onOpenQueue,
                modifier = Modifier.weight(1f),
            )
            BottomActionButton(
                icon = Icons.AutoMirrored.Rounded.Notes,
                label = stringResource(R.string.lyrics),
                onClick = onOpenLyrics,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BottomActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null)
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Big play/pause button that morphs from a circle (paused) to a squircle (playing). */
@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val corner by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 36.dp,
        label = "playPauseCorner",
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.cd_play_pause),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(38.dp),
        )
    }
}

@Composable
private fun WavySeekBar(
    positionMs: Long,
    durationMs: Long,
    playing: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val duration = durationMs.coerceAtLeast(1L)
    val displayed = dragValue ?: (positionMs.toFloat() / duration).coerceIn(0f, 1f)

    // Squiggle only while playing; flat (waveHeight 0) when paused or scrubbing.
    val waveHeight by animateDpAsState(
        targetValue = if (playing && dragValue == null) 8.dp else 0.dp,
        label = "waveHeight",
    )

    Column(modifier = modifier) {
        WavySlider(
            value = displayed,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onSeekTo((it * duration).toLong()) }
                dragValue = null
            },
            waveLength = 26.dp,
            waveHeight = waveHeight,
            incremental = true,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDuration((displayed * duration).toLong()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDuration(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}
