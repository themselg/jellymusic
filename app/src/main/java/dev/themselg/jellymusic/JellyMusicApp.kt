package dev.themselg.jellymusic

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.google.android.gms.cast.framework.CastContext
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
        // Initialize Cast early so the Cast MediaRouteProvider registers and our Compose Cast
        // dialog can discover devices via MediaRouter. Guarded: no-op without Play Services.
        runCatching { CastContext.getSharedInstance(this) }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
