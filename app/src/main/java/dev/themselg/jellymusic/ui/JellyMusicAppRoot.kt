// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.themselg.jellymusic.ui.components.CastVolumeOverlayHost
import dev.themselg.jellymusic.ui.feature.login.LoginScreen
import dev.themselg.jellymusic.ui.navigation.Route
import dev.themselg.jellymusic.ui.theme.AppTheme

/**
 * App entry composable. Owns the theme (from preferences + album-art seed color) and the
 * top-level Login/Signed-in split. Once signed in, [MainScaffold] hosts the inner nav graph.
 */
@Composable
fun JellyMusicAppRoot() {
    val viewModel: RootViewModel = hiltViewModel()
    val settings by viewModel.themeSettings.collectAsStateWithLifecycle()
    val seedColor by viewModel.seedColor.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()

    AppTheme(settings = settings, seedColor = seedColor) {
        val navController = rememberNavController()
        val signedIn = session != null

        // Top-level graph: Login vs the signed-in scaffold. React to session transitions so the
        // user is moved to the right place after sign-in / sign-out.
        LaunchedEffect(signedIn) {
            val target = if (signedIn) Route.Home else Route.Login
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }

        Box {
            NavHost(
                navController = navController,
                startDestination = if (signedIn) Route.Home else Route.Login,
            ) {
                composable<Route.Login> {
                    LoginScreen()
                }
                // Home anchors the signed-in experience; MainScaffold owns its own inner NavHost.
                composable<Route.Home> {
                    MainScaffold()
                }
            }
            // Cast volume HUD, drawn above everything.
            CastVolumeOverlayHost()
        }
    }
}
