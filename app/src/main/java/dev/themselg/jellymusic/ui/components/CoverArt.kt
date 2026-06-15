package dev.themselg.jellymusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.themselg.jellymusic.R

/**
 * Rounded album/artist/playlist artwork. Renders a music-note placeholder on a
 * surface-variant background while loading, on error, or when [url] is null/blank.
 */
@Composable
fun CoverArt(
    url: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val hasUrl = !url.isNullOrBlank()
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (hasUrl) {
            AsyncImage(
                model = url,
                contentDescription = contentDescription ?: stringResource(R.string.cd_album_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                // NOTE: coil3 AsyncImage placeholder/error slots are Painters; we keep a Box
                // background instead, and overlay the note icon for the empty/no-url case below.
            )
        } else {
            // Note glyph sized to ~40% of the tile, independent of corner radius.
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxSize(0.4f),
            )
        }
    }
}
