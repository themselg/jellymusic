// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.themselg.jellymusic.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val serverUrl: String = "https://",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean
        // Jellyfin allows password-less accounts (e.g. the public demo user), so the
        // password field is intentionally not required here.
        get() = !isLoading &&
            serverUrl.isNotBlank() && serverUrl != "https://" &&
            username.isNotBlank()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onServerUrlChange(value: String) {
        _state.value = _state.value.copy(serverUrl = value, error = null)
    }

    fun onUsernameChange(value: String) {
        _state.value = _state.value.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        _state.value = _state.value.copy(password = value, error = null)
    }

    fun signIn() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.value = current.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                sessionManager.signIn(
                    serverUrl = current.serverUrl.trim(),
                    username = current.username.trim(),
                    password = current.password,
                )
            }.fold(
                onSuccess = {
                    // Session flow turns non-null; the root navigates away. Just clear loading.
                    _state.value = _state.value.copy(isLoading = false)
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = e.message ?: "Sign-in failed",
                    )
                },
            )
        }
    }
}
