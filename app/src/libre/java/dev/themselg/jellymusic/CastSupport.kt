package dev.themselg.jellymusic

import android.app.Activity
import android.app.Application
import android.view.KeyEvent

/** libre flavor: no Chromecast. Both entry points are no-ops. */
@Suppress("UNUSED_PARAMETER")
object CastSupport {
    fun init(app: Application) {}
    fun handleVolumeKey(activity: Activity, event: KeyEvent): Boolean = false
}
