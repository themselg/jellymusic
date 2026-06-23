// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the sleep-timer countdown. App-scoped singleton shared (same Hilt graph, same process) by
 * [MusicService][dev.themselg.jellymusic.player.service.MusicService] — which sets [onExpire] to
 * pause the active player and forwards natural track-end events — and by
 * [PlayerControllerImpl], which the UI drives.
 *
 * A sleep timer only matters while music is playing, and during playback a foreground media
 * service keeps the process (and this singleton) alive, so a plain coroutine [Job] is sufficient:
 * no AlarmManager (it would only pause something that has already stopped) and no persistence of
 * the countdown across process death (moot once playback is gone).
 */
@Singleton
class SleepTimer @Inject constructor() {

    /**
     * Coroutine scope the countdown runs on. Main thread in production (so [onExpire] can touch
     * the player directly). Overridable in tests to drive the countdown with virtual time.
     */
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Invoked on [scope] when the timer fires (duration elapsed or armed track ended). */
    var onExpire: (() -> Unit)? = null

    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private var job: Job? = null

    /** Start a countdown of [durationMs]; <= 0 cancels any running timer. */
    fun startDuration(durationMs: Long) {
        job?.cancel()
        if (durationMs <= 0L) {
            _state.value = SleepTimerState.Off
            return
        }
        job = scope.launch {
            var remaining = durationMs
            while (isActive && remaining > 0L) {
                _state.value = SleepTimerState.Running(remaining)
                delay(TICK_MS)
                remaining -= TICK_MS
            }
            if (isActive) {
                _state.value = SleepTimerState.Off
                onExpire?.invoke()
            }
        }
    }

    /** Arm the timer to fire when the current track ends naturally (not on a manual skip). */
    fun armEndOfTrack() {
        job?.cancel()
        job = null
        _state.value = SleepTimerState.EndOfTrack
    }

    /** Called by the service when a track finishes by itself; fires if armed for end-of-track. */
    fun onTrackEndedNaturally() {
        if (_state.value is SleepTimerState.EndOfTrack) {
            _state.value = SleepTimerState.Off
            onExpire?.invoke()
        }
    }

    /** Cancel any active timer. */
    fun cancel() {
        job?.cancel()
        job = null
        _state.value = SleepTimerState.Off
    }

    private companion object {
        const val TICK_MS = 1_000L
    }
}
