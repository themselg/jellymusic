package dev.themselg.jellymusic.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import androidx.mediarouter.media.MediaRouter.RouteInfo
import com.google.android.gms.cast.CastMediaControlIntent
import dev.themselg.jellymusic.ui.R
import kotlin.math.roundToInt

/**
 * Compose Cast button + chooser/controller dialog, built directly on the MediaRouter API so it
 * follows the app's Material 3 style and dynamic (album-art) color — unlike the stock mediarouter
 * dialogs. Hidden when no Cast devices are around and nothing is casting.
 */
@Composable
fun CastButton(modifier: Modifier = Modifier) {
    val state = rememberCastState() ?: return
    var showDialog by remember { mutableStateOf(false) }

    // Nothing to show if there are no devices and we're not already casting.
    if (state.routes.isEmpty() && !state.isCasting) return

    IconButton(onClick = { showDialog = true }, modifier = modifier) {
        Icon(
            imageVector = if (state.isCasting) Icons.Rounded.CastConnected else Icons.Rounded.Cast,
            contentDescription = stringResource(R.string.cast),
            tint = if (state.isCasting) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }

    if (showDialog) {
        CastDialog(state = state, onDismiss = { showDialog = false })
    }
}

@Composable
private fun CastDialog(state: CastState, onDismiss: () -> Unit) {
    val casting = state.isCasting
    val selected = state.selectedRoute

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (casting) selected?.name ?: stringResource(R.string.cast)
                else stringResource(R.string.cast_to),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            if (casting && selected != null) {
                VolumeControl(route = selected, onSetVolume = state::setVolume)
            } else {
                Column {
                    state.routes.forEach { route ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    state.select(route)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Icon(Icons.Rounded.Cast, contentDescription = null)
                            Text(route.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (casting) {
                TextButton(onClick = { state.stop(); onDismiss() }) {
                    Text(stringResource(R.string.stop_casting))
                }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
        },
        dismissButton = if (casting) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
        } else {
            null
        },
    )
}

@Composable
private fun VolumeControl(route: RouteInfo, onSetVolume: (Int) -> Unit) {
    if (route.volumeHandling != RouteInfo.PLAYBACK_VOLUME_VARIABLE || route.volumeMax <= 0) return
    var volume by remember(route) { mutableStateOf(route.volume.toFloat()) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
        Slider(
            value = volume,
            onValueChange = {
                volume = it
                onSetVolume(it.roundToInt())
            },
            valueRange = 0f..route.volumeMax.toFloat(),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Holds Cast/MediaRouter state for Compose; refreshed from the router callback. */
private class CastState(
    val router: MediaRouter,
    val selector: MediaRouteSelector,
) {
    var routes by mutableStateOf<List<RouteInfo>>(emptyList())
        private set
    var selectedRoute by mutableStateOf<RouteInfo?>(null)
        private set

    /** True when the selected route is a Cast device (not the phone's default route). */
    val isCasting: Boolean
        get() = selectedRoute?.let { !it.isDefault && it.matchesSelector(selector) } == true

    fun refresh() {
        routes = router.routes.filter { it.isEnabled && !it.isDefault && it.matchesSelector(selector) }
        selectedRoute = router.selectedRoute
    }

    fun select(route: RouteInfo) = router.selectRoute(route)
    fun stop() = router.unselect(MediaRouter.UNSELECT_REASON_STOPPED)
    fun setVolume(volume: Int) = selectedRoute?.requestSetVolume(volume)
}

@Composable
private fun rememberCastState(): CastState? {
    val context = LocalContext.current
    val state = remember {
        runCatching {
            val router = MediaRouter.getInstance(context)
            val selector = MediaRouteSelector.Builder()
                .addControlCategory(
                    CastMediaControlIntent.categoryForCast(
                        CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID,
                    ),
                )
                .build()
            CastState(router, selector)
        }.getOrNull()
    }

    if (state != null) {
        DisposableEffect(state) {
            val callback = object : MediaRouter.Callback() {
                override fun onRouteAdded(router: MediaRouter, route: RouteInfo) = state.refresh()
                override fun onRouteRemoved(router: MediaRouter, route: RouteInfo) = state.refresh()
                override fun onRouteChanged(router: MediaRouter, route: RouteInfo) = state.refresh()
                override fun onRouteSelected(router: MediaRouter, route: RouteInfo, reason: Int) = state.refresh()
                override fun onRouteUnselected(router: MediaRouter, route: RouteInfo, reason: Int) = state.refresh()
                override fun onRouteVolumeChanged(router: MediaRouter, route: RouteInfo) = state.refresh()
            }
            // REQUEST_DISCOVERY actively scans for Cast devices while this UI is shown.
            state.router.addCallback(
                state.selector,
                callback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY,
            )
            state.refresh()
            onDispose { state.router.removeCallback(callback) }
        }
    }
    return state
}
