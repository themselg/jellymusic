// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {

    private fun timerOn(scheduler: kotlinx.coroutines.test.TestCoroutineScheduler): SleepTimer =
        SleepTimer().apply { scope = CoroutineScope(StandardTestDispatcher(scheduler)) }

    @Test
    fun `duration timer counts down then fires onExpire and resets to Off`() = runTest {
        val timer = timerOn(testScheduler)
        var expired = false
        timer.onExpire = { expired = true }

        timer.startDuration(3_000L)
        runCurrent()
        assertEquals(SleepTimerState.Running(3_000L), timer.state.value)
        assertFalse(expired)

        advanceTimeBy(1_000L); runCurrent()
        assertEquals(SleepTimerState.Running(2_000L), timer.state.value)

        advanceUntilIdle()
        assertTrue(expired, "onExpire should fire once the countdown elapses")
        assertEquals(SleepTimerState.Off, timer.state.value)
    }

    @Test
    fun `non-positive duration cancels and does not fire`() = runTest {
        val timer = timerOn(testScheduler)
        var expired = false
        timer.onExpire = { expired = true }

        timer.startDuration(0L)
        advanceUntilIdle()

        assertFalse(expired)
        assertEquals(SleepTimerState.Off, timer.state.value)
    }

    @Test
    fun `cancel stops a running timer before it fires`() = runTest {
        val timer = timerOn(testScheduler)
        var expired = false
        timer.onExpire = { expired = true }

        timer.startDuration(10_000L)
        advanceTimeBy(3_000L); runCurrent()
        timer.cancel()
        advanceUntilIdle()

        assertFalse(expired)
        assertEquals(SleepTimerState.Off, timer.state.value)
    }

    @Test
    fun `end-of-track fires only on a natural track end`() = runTest {
        val timer = timerOn(testScheduler)
        var expired = false
        timer.onExpire = { expired = true }

        timer.armEndOfTrack()
        assertIs<SleepTimerState.EndOfTrack>(timer.state.value)

        timer.onTrackEndedNaturally()
        assertTrue(expired)
        assertEquals(SleepTimerState.Off, timer.state.value)
    }

    @Test
    fun `track end is ignored when no end-of-track timer is armed`() = runTest {
        val timer = timerOn(testScheduler)
        var expired = false
        timer.onExpire = { expired = true }

        timer.onTrackEndedNaturally()

        assertFalse(expired)
        assertEquals(SleepTimerState.Off, timer.state.value)
    }
}
