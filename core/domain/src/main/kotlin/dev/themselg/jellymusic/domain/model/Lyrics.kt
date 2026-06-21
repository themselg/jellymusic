// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.domain.model

/**
 * A song's lyrics. [synced] is true when at least one line carries a timestamp,
 * in which case the UI can highlight/scroll the active line as playback progresses.
 */
data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
)

/** A single lyric line. [startMs] is the line's start time in milliseconds, or null when unsynced. */
data class LyricLine(
    val text: String,
    val startMs: Long?,
)
