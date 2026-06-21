// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Static brand palette (the STATIC color mode and the fallback before any artwork loads).
// A violet seed in the spirit of the default Material 3 baseline.
private val Purple = Color(0xFF6750A4)
private val PurpleLight = Color(0xFFD0BCFF)

val StaticLightColors = lightColorScheme(
    primary = Purple,
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
)

val StaticDarkColors = darkColorScheme(
    primary = PurpleLight,
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
)

/** Default seed used to derive an album-art-style scheme before any track is playing. */
val DefaultSeed = Purple
