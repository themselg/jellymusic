package dev.themselg.jellymusic

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JellyMusicApp : Application(), SingletonImageLoader.Factory {

    // Provided by Hilt; configured with the OkHttp fetcher so artwork loads share
    // one client. Coil's loaded bitmaps also feed album-art color extraction.
    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()
        // Initialize Cast early (proprietary flavor) so the Cast MediaRouteProvider registers and
        // the Compose cast dialog can discover devices. No-op in the libre flavor.
        CastSupport.init(this)
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
