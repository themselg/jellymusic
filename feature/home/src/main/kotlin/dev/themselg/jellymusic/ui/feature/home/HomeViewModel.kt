// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dev.themselg.jellymusic.domain.model.Album
import dev.themselg.jellymusic.domain.model.Artist
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.data.session.JellyfinUrls
import dev.themselg.jellymusic.data.session.SessionManager
import dev.themselg.jellymusic.domain.repository.LibraryRepository
import dev.themselg.jellymusic.domain.repository.SortBy
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeTab { ALBUMS, ARTISTS, SONGS }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController,
    sessionManager: SessionManager,
    urls: JellyfinUrls,
) : ViewModel() {

    /** Friendly Jellyfin server name shown as the screen title; blank until known. */
    val serverName: StateFlow<String> = sessionManager.session
        .map { it?.serverName?.takeIf(String::isNotBlank).orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** Avatar shown on the profile button; null until signed in. */
    val userImageUrl: StateFlow<String?> = sessionManager.session
        .map { if (it != null) urls.userImageUrl() else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Per-tab sort order. The paged flows below react to changes via flatMapLatest.
    private val _albumsSort = MutableStateFlow(SortBy.NAME)
    val albumsSort: StateFlow<SortBy> = _albumsSort.asStateFlow()
    private val _artistsSort = MutableStateFlow(SortBy.NAME)
    val artistsSort: StateFlow<SortBy> = _artistsSort.asStateFlow()
    private val _songsSort = MutableStateFlow(SortBy.NAME)
    val songsSort: StateFlow<SortBy> = _songsSort.asStateFlow()

    fun setAlbumsSort(sortBy: SortBy) { _albumsSort.value = sortBy }
    fun setArtistsSort(sortBy: SortBy) { _artistsSort.value = sortBy }
    fun setSongsSort(sortBy: SortBy) { _songsSort.value = sortBy }

    // Paged library lists. cachedIn(viewModelScope) survives config changes & re-subscriptions.
    val albums: Flow<PagingData<Album>> =
        _albumsSort.flatMapLatest { libraryRepository.pagedAlbums(it) }.cachedIn(viewModelScope)
    val artists: Flow<PagingData<Artist>> =
        _artistsSort.flatMapLatest { libraryRepository.pagedArtists(it) }.cachedIn(viewModelScope)
    val songs: Flow<PagingData<Song>> =
        _songsSort.flatMapLatest { libraryRepository.pagedSongs(it) }.cachedIn(viewModelScope)

    /** Small, non-paged "Recently added" row shown above the albums grid. */
    private val _recentlyAdded = MutableStateFlow<List<Album>>(emptyList())
    val recentlyAdded: StateFlow<List<Album>> = _recentlyAdded.asStateFlow()

    init {
        loadRecentlyAdded()
    }

    fun loadRecentlyAdded() {
        viewModelScope.launch {
            runCatching { libraryRepository.getRecentlyAddedAlbums() }
                .onSuccess { _recentlyAdded.value = it }
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int) {
        playerController.play(songs, startIndex)
    }
}
