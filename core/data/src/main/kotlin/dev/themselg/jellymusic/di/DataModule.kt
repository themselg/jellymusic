// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.di

import dev.themselg.jellymusic.data.repository.FavoritesRepositoryImpl
import dev.themselg.jellymusic.data.repository.LibraryRepositoryImpl
import dev.themselg.jellymusic.data.repository.PlaylistRepositoryImpl
import dev.themselg.jellymusic.data.repository.SearchRepositoryImpl
import dev.themselg.jellymusic.data.session.JellyfinUrls
import dev.themselg.jellymusic.data.session.SessionManager
import dev.themselg.jellymusic.data.session.SessionManagerImpl
import dev.themselg.jellymusic.domain.repository.FavoritesRepository
import dev.themselg.jellymusic.domain.repository.LibraryRepository
import dev.themselg.jellymusic.domain.repository.PlaylistRepository
import dev.themselg.jellymusic.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds data-layer implementations to their domain contracts.
 *
 * [SessionManagerImpl] is a `@Singleton @Inject` class that implements both [SessionManager]
 * and [JellyfinUrls]; both interfaces are bound to that same instance so URL builders and
 * the authenticated client always reflect the live session. Repositories depend on the
 * concrete [SessionManagerImpl] (for `requireApi()`) directly, so they need no extra binding
 * beyond their own constructor injection.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindSessionManager(impl: SessionManagerImpl): SessionManager

    @Binds
    @Singleton
    abstract fun bindJellyfinUrls(impl: SessionManagerImpl): JellyfinUrls

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
