// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.di

import android.content.Context
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.themselg.jellymusic.data.download.MusicDownloadService
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Singleton

/**
 * Offline-download wiring. The [SimpleCache] is the single app-wide instance (Media3 forbids two
 * per directory); it's shared by the [DownloadManager] (writes downloads) and the playback
 * ExoPlayer's CacheDataSource (reads downloaded content) in MusicService.
 */
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    @Provides
    @Singleton
    fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider =
        StandaloneDatabaseProvider(context)

    @Provides
    @Singleton
    fun provideDownloadCache(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
    ): SimpleCache =
        SimpleCache(File(context.filesDir, "downloads"), NoOpCacheEvictor(), databaseProvider)

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        databaseProvider: DatabaseProvider,
        cache: SimpleCache,
    ): DownloadManager = DownloadManager(
        context,
        databaseProvider,
        cache,
        DefaultHttpDataSource.Factory(),
        Executors.newFixedThreadPool(3),
    ).apply { maxParallelDownloads = 3 }

    @Provides
    @Singleton
    fun provideDownloadNotificationHelper(
        @ApplicationContext context: Context,
    ): DownloadNotificationHelper =
        DownloadNotificationHelper(context, MusicDownloadService.DOWNLOAD_CHANNEL_ID)
}
