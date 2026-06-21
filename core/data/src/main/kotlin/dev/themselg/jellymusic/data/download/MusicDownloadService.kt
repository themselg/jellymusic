// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.download

import android.app.Notification
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import dagger.hilt.android.EntryPointAccessors
import dev.themselg.jellymusic.data.R

/**
 * Foreground service that runs Media3 downloads. Instantiated by the system, so it pulls its
 * dependencies (the shared [DownloadManager] + notification helper) from Hilt via an EntryPoint.
 */
class MusicDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DOWNLOAD_CHANNEL_ID,
    R.string.downloads,
    /* channelDescriptionResourceId = */ 0,
) {
    private fun entryPoint(): DownloadEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, DownloadEntryPoint::class.java)

    override fun getDownloadManager(): DownloadManager = entryPoint().downloadManager()

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int,
    ): Notification = entryPoint().downloadNotificationHelper().buildProgressNotification(
        this,
        android.R.drawable.stat_sys_download,
        /* contentIntent = */ null,
        /* message = */ null,
        downloads,
        notMetRequirements,
    )

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 2
        const val DOWNLOAD_CHANNEL_ID = "downloads"
    }
}
