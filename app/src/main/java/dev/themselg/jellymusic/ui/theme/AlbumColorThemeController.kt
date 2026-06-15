package dev.themselg.jellymusic.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.SuccessResult
import coil3.toBitmap
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches the currently playing track and publishes a seed [Color] derived from its
 * album art. `AppTheme` consumes [seedColor] when the ALBUM_ART color mode is active.
 *
 * The artwork bitmap is loaded through Coil (so it benefits from the shared cache),
 * then handed to [SeedColorExtractor]. `allowHardware(false)` is required because
 * hardware bitmaps cannot be read pixel-by-pixel.
 */
@Singleton
class AlbumColorThemeController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    playerController: PlayerController,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _seedColor = MutableStateFlow<Color?>(null)
    val seedColor: StateFlow<Color?> = _seedColor.asStateFlow()

    init {
        scope.launch {
            playerController.nowPlaying
                .map { it?.artworkUrl }
                .distinctUntilChanged()
                .collect { url -> _seedColor.value = url?.let { seedFor(it) } }
        }
    }

    private suspend fun seedFor(url: String): Color? = runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = imageLoader.execute(request)
        val bitmap = (result as? SuccessResult)?.image?.toBitmap() ?: return null
        SeedColorExtractor.extract(bitmap)
    }.getOrNull()
}
