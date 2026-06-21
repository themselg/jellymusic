// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.download

import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.themselg.jellymusic.domain.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

enum class DownloadStatus { NONE, DOWNLOADING, DOWNLOADED, FAILED }

data class DownloadedItem(
    val song: Song,
    val status: DownloadStatus,
    val progress: Float, // 0f..1f
)

/**
 * Thin wrapper over the Media3 [DownloadManager]. Each download carries the [Song]'s metadata
 * (serialized into `DownloadRequest.data`) so the Downloads list and offline playback work
 * without the server. State is exposed as a [StateFlow] refreshed from the download index.
 */
@Singleton
class DownloadController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadManager: DownloadManager,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _items = MutableStateFlow<List<DownloadedItem>>(emptyList())
    val items: StateFlow<List<DownloadedItem>> = _items.asStateFlow()

    init {
        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onInitialized(downloadManager: DownloadManager) = refresh()
            override fun onDownloadChanged(
                downloadManager: DownloadManager,
                download: Download,
                finalException: Exception?,
            ) = refresh()
            override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) = refresh()
            override fun onIdle(downloadManager: DownloadManager) = refresh()
        })
        refresh()
    }

    fun download(song: Song) {
        val request = DownloadRequest.Builder(song.id, song.streamUrl.toUri())
            .setMimeType(song.mimeType)
            .setData(json.encodeToString(Song.serializer(), song).toByteArray())
            .build()
        DownloadService.sendAddDownload(context, MusicDownloadService::class.java, request, false)
    }

    fun download(songs: List<Song>) = songs.forEach(::download)

    fun remove(songId: String) =
        DownloadService.sendRemoveDownload(context, MusicDownloadService::class.java, songId, false)

    private fun refresh() {
        scope.launch {
            val result = runCatching {
                val out = mutableListOf<DownloadedItem>()
                downloadManager.downloadIndex.getDownloads().use { cursor ->
                    while (cursor.moveToNext()) {
                        val d = cursor.download
                        val song = runCatching {
                            json.decodeFromString(Song.serializer(), String(d.request.data))
                        }.getOrNull() ?: continue
                        out += DownloadedItem(
                            song = song,
                            status = d.state.toStatus(),
                            progress = (d.percentDownloaded.takeIf { it >= 0f } ?: 0f) / 100f,
                        )
                    }
                }
                out
            }.getOrDefault(emptyList())
            _items.value = result
        }
    }

    private fun Int.toStatus(): DownloadStatus = when (this) {
        Download.STATE_COMPLETED -> DownloadStatus.DOWNLOADED
        Download.STATE_FAILED -> DownloadStatus.FAILED
        Download.STATE_QUEUED,
        Download.STATE_DOWNLOADING,
        Download.STATE_RESTARTING,
        Download.STATE_STOPPED,
        -> DownloadStatus.DOWNLOADING
        else -> DownloadStatus.NONE
    }
}

/** Lets the system-instantiated [MusicDownloadService] and composables reach singletons. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadEntryPoint {
    fun downloadController(): DownloadController
    fun downloadManager(): DownloadManager
    fun downloadNotificationHelper(): androidx.media3.exoplayer.offline.DownloadNotificationHelper
}
