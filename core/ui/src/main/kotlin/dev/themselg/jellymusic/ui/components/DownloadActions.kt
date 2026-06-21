// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import dev.themselg.jellymusic.data.download.DownloadController
import dev.themselg.jellymusic.data.download.DownloadEntryPoint

/**
 * Reaches the singleton [DownloadController] from any composable (it can't be `hiltViewModel`-
 * injected into a leaf component like SongRow). Lets every song row offer download actions
 * without threading the controller through every screen's ViewModel.
 */
@Composable
fun rememberDownloadController(): DownloadController {
    val appContext = LocalContext.current.applicationContext
    return remember {
        EntryPointAccessors.fromApplication(appContext, DownloadEntryPoint::class.java)
            .downloadController()
    }
}
