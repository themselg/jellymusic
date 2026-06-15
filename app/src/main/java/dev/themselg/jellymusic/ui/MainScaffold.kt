package dev.themselg.jellymusic.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.themselg.jellymusic.R
import dev.themselg.jellymusic.ui.components.MiniPlayer
import dev.themselg.jellymusic.ui.feature.detail.AlbumDetailScreen
import dev.themselg.jellymusic.ui.feature.detail.ArtistDetailScreen
import dev.themselg.jellymusic.ui.feature.detail.PlaylistDetailScreen
import dev.themselg.jellymusic.ui.feature.home.HomeScreen
import dev.themselg.jellymusic.ui.feature.library.DownloadsScreen
import dev.themselg.jellymusic.ui.feature.library.LibraryScreen
import dev.themselg.jellymusic.ui.feature.library.LikedSongsScreen
import dev.themselg.jellymusic.ui.feature.player.LyricsScreen
import dev.themselg.jellymusic.ui.feature.player.NowPlayingScreen
import dev.themselg.jellymusic.ui.feature.player.QueueScreen
import dev.themselg.jellymusic.ui.feature.profile.ProfileScreen
import dev.themselg.jellymusic.ui.feature.search.SearchScreen
import dev.themselg.jellymusic.ui.navigation.Route

private data class TopDestination(
    val route: Route,
    val icon: ImageVector,
    val labelRes: Int,
)

/**
 * Signed-in shell: bottom NavigationBar across Home/Search/Settings with a single inner
 * NavHost. Detail screens and NowPlaying are pushed on top of the current tab. The MiniPlayer
 * sits between content and the bar whenever something is loaded.
 */
@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val nowPlaying by mainViewModel.nowPlaying.collectAsStateWithLifecycle()
    val playbackState by mainViewModel.playbackState.collectAsStateWithLifecycle()

    val topDestinations = listOf(
        TopDestination(Route.Home, Icons.Rounded.Home, R.string.home),
        TopDestination(Route.Search, Icons.Rounded.Search, R.string.search),
        TopDestination(Route.Library, Icons.Rounded.LibraryMusic, R.string.library),
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Now Playing, Queue and Lyrics are full-screen: hide the mini-player + bottom navigation there.
    val immersive = currentDestination?.let {
        it.hierarchyHasRoute(Route.NowPlaying) ||
            it.hierarchyHasRoute(Route.Queue) ||
            it.hierarchyHasRoute(Route.Lyrics)
    } ?: false

    Scaffold(
        bottomBar = {
            if (!immersive) {
                Column {
                    nowPlaying?.let { np ->
                        MiniPlayer(
                            nowPlaying = np,
                            playbackState = playbackState,
                            onClick = { navController.navigate(Route.NowPlaying) },
                            onTogglePlayPause = mainViewModel::togglePlayPause,
                            onNext = mainViewModel::next,
                        )
                    }
                    NavigationBar {
                        topDestinations.forEach { dest ->
                            val selected = currentDestination?.hierarchyHasRoute(dest.route) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(dest.icon, contentDescription = null) },
                                label = { Text(stringResource(dest.labelRes)) },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        val dur = 320
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // Defaults for tabs + detail pushes. The full-screen player/queue (NowPlaying, Queue)
            // override these to rise from the bottom; when they do, the screen underneath must
            // stay static (None) so it doesn't slide while the player slides up over it.
            enterTransition = {
                val fromTab = initialState.destination.tabIndex()
                val toTab = targetState.destination.tabIndex()
                when {
                    initialState.destination.isImmersive() -> EnterTransition.None
                    fromTab != null && toTab != null ->
                        slideIntoContainer(tabDirection(fromTab, toTab), tween(dur)) + fadeIn(tween(dur))
                    else -> slideIntoContainer(SlideDirection.Start, tween(dur)) + fadeIn(tween(dur))
                }
            },
            exitTransition = {
                val fromTab = initialState.destination.tabIndex()
                val toTab = targetState.destination.tabIndex()
                when {
                    targetState.destination.isImmersive() -> ExitTransition.None
                    fromTab != null && toTab != null ->
                        slideOutOfContainer(tabDirection(fromTab, toTab), tween(dur)) + fadeOut(tween(dur))
                    else -> slideOutOfContainer(SlideDirection.Start, tween(dur)) + fadeOut(tween(dur))
                }
            },
            popEnterTransition = {
                if (initialState.destination.isImmersive()) EnterTransition.None
                else slideIntoContainer(SlideDirection.End, tween(dur)) + fadeIn(tween(dur))
            },
            popExitTransition = {
                if (targetState.destination.isImmersive()) ExitTransition.None
                else slideOutOfContainer(SlideDirection.End, tween(dur)) + fadeOut(tween(dur))
            },
        ) {
            composable<Route.Home> {
                HomeScreen(
                    onAlbumClick = { navController.navigate(Route.AlbumDetail(it)) },
                    onArtistClick = { navController.navigate(Route.ArtistDetail(it)) },
                    onOpenProfile = { navController.navigate(Route.Profile) },
                )
            }
            composable<Route.Search> {
                SearchScreen(
                    onAlbumClick = { navController.navigate(Route.AlbumDetail(it)) },
                    onArtistClick = { navController.navigate(Route.ArtistDetail(it)) },
                )
            }
            composable<Route.Library> {
                LibraryScreen(
                    onOpenProfile = { navController.navigate(Route.Profile) },
                    onOpenLikedSongs = { navController.navigate(Route.LikedSongs) },
                    onOpenDownloads = { navController.navigate(Route.Downloads) },
                    onPlaylistClick = { navController.navigate(Route.PlaylistDetail(it)) },
                )
            }
            composable<Route.Profile> {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.LikedSongs> {
                LikedSongsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.Downloads> {
                DownloadsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.AlbumDetail> {
                AlbumDetailScreen(
                    onBack = { navController.popBackStack() },
                    onArtistClick = { navController.navigate(Route.ArtistDetail(it)) },
                )
            }
            composable<Route.ArtistDetail> {
                ArtistDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAlbumClick = { navController.navigate(Route.AlbumDetail(it)) },
                )
            }
            composable<Route.PlaylistDetail> {
                PlaylistDetailScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            // Now Playing rises from the bottom (from where the mini-player sits) and drops back down.
            composable<Route.NowPlaying>(
                enterTransition = { slideInVertically(tween(dur)) { it } + fadeIn(tween(dur)) },
                popExitTransition = { slideOutVertically(tween(dur)) { it } + fadeOut(tween(dur)) },
                // Stay static while the queue rises over it / drops away.
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
            ) {
                NowPlayingScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQueue = { navController.navigate(Route.Queue) },
                    onOpenLyrics = { navController.navigate(Route.Lyrics) },
                )
            }
            // The queue also rises from the bottom over Now Playing.
            composable<Route.Queue>(
                enterTransition = { slideInVertically(tween(dur)) { it } + fadeIn(tween(dur)) },
                popExitTransition = { slideOutVertically(tween(dur)) { it } + fadeOut(tween(dur)) },
            ) {
                QueueScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            // Lyrics rise from the bottom over Now Playing, like the queue.
            composable<Route.Lyrics>(
                enterTransition = { slideInVertically(tween(dur)) { it } + fadeIn(tween(dur)) },
                popExitTransition = { slideOutVertically(tween(dur)) { it } + fadeOut(tween(dur)) },
            ) {
                LyricsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavDestination.hierarchyHasRoute(route: Route): Boolean =
    hierarchy.any { it.hasRoute(route::class) }

/** Bottom-nav order, used to pick the horizontal slide direction between tabs. */
private fun NavDestination.tabIndex(): Int? = when {
    hasRoute(Route.Home::class) -> 0
    hasRoute(Route.Search::class) -> 1
    hasRoute(Route.Library::class) -> 2
    else -> null
}

/** Full-screen destinations that rise over a static background. */
private fun NavDestination.isImmersive(): Boolean =
    hasRoute(Route.NowPlaying::class) ||
        hasRoute(Route.Queue::class) ||
        hasRoute(Route.Lyrics::class)

/** Slide toward the tab being opened: higher index → content enters from the right. */
private fun tabDirection(fromTab: Int, toTab: Int): SlideDirection =
    if (toTab > fromTab) SlideDirection.Start else SlideDirection.End
