package dev.themselg.jellymusic.ui.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.domain.model.SearchResults
import dev.themselg.jellymusic.domain.model.Song
import dev.themselg.jellymusic.domain.repository.SearchRepository
import dev.themselg.jellymusic.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

sealed interface SearchUiState {
    /** No query yet — prompt the user. */
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Error(val message: String?) : SearchUiState
    data class Success(val results: SearchResults) : SearchUiState
}

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val playerController: PlayerController,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        _query
            .debounce(300)
            .distinctUntilChanged()
            .onEach { q ->
                val trimmed = q.trim()
                if (trimmed.isBlank()) {
                    _state.value = SearchUiState.Idle
                } else {
                    _state.value = SearchUiState.Loading
                    runCatching { searchRepository.search(trimmed) }.fold(
                        onSuccess = { _state.value = SearchUiState.Success(it) },
                        onFailure = { _state.value = SearchUiState.Error(it.message) },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun playSongs(songs: List<Song>, startIndex: Int) {
        playerController.play(songs, startIndex)
    }
}
