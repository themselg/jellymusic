package dev.themselg.jellymusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * libre flavor: no Chromecast, so the cast button renders nothing. Same signature as the
 * proprietary implementation so shared screens (e.g. NowPlayingScreen) compile unchanged.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun CastButton(modifier: Modifier = Modifier) {
}
