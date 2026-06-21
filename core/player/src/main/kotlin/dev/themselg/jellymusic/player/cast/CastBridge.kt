// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.player.cast

/**
 * Flavor-neutral handle to whatever Cast integration the current build flavor provides.
 * The `proprietary` flavor backs this with a real Google Cast [androidx.media3.cast.CastPlayer];
 * the `libre` flavor returns a no-op. Created via `MusicService.connectCast(...)`.
 */
interface CastBridge {
    fun release()
}
