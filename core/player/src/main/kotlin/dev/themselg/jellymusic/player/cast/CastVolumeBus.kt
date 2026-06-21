// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player.cast

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A single Cast volume change, with a monotonically increasing [id] so repeated changes to the
 *  same level still re-trigger the on-screen overlay. */
data class CastVolumeChange(val id: Long, val fraction: Float)

/**
 * Lightweight bus between the (non-Compose) volume-key handling in MainActivity and the Compose
 * volume overlay. Process-wide singleton; the activity reports a new Cast volume level here and
 * the UI shows a brief overlay in response.
 */
object CastVolumeBus {
    private var counter = 0L
    private val _state = MutableStateFlow(CastVolumeChange(0L, 0f))
    val state: StateFlow<CastVolumeChange> = _state.asStateFlow()

    fun report(fraction: Float) {
        counter += 1
        _state.value = CastVolumeChange(counter, fraction.coerceIn(0f, 1f))
    }
}
