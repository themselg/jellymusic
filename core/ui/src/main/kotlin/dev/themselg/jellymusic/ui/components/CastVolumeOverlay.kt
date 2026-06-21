// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.themselg.jellymusic.player.cast.CastVolumeBus
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * App-wide overlay that briefly shows the Cast volume level when it's changed with the hardware
 * volume keys (those events are consumed, so the system HUD never appears). Observes
 * [CastVolumeBus]; place once near the top of the composition, above all content.
 */
@Composable
fun CastVolumeOverlayHost(modifier: Modifier = Modifier) {
    val change by CastVolumeBus.state.collectAsStateWithLifecycle()
    var visible by remember { mutableStateOf(false) }

    // Show on each new change id, then auto-hide.
    LaunchedEffect(change.id) {
        if (change.id == 0L) return@LaunchedEffect
        visible = true
        delay(1600)
        visible = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            VolumeCard(fraction = change.fraction)
        }
    }
}

@Composable
private fun VolumeCard(fraction: Float) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .width(280.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Rounded.CastConnected, contentDescription = null)
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${(fraction * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
