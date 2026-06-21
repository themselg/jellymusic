// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.data.prefs

/** How the app derives its Material 3 color scheme. */
enum class ColorMode {
    /** Material You from the device wallpaper (Android 12+). */
    SYSTEM,

    /** Seed color extracted from the currently playing album art. */
    ALBUM_ART,

    /** Fixed in-app brand palette. */
    STATIC,
}

/** Light/dark selection, independent of the color source. */
enum class DarkMode { SYSTEM, LIGHT, DARK }

data class ThemeSettings(
    val colorMode: ColorMode = ColorMode.SYSTEM,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    /** When true and the device is Android 12+, ALBUM_ART falls back to SYSTEM until a track plays. */
    val amoledBlack: Boolean = false,
)
