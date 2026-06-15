package dev.themselg.jellymusic

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.cast.framework.CastContext
import dev.themselg.jellymusic.player.cast.CastVolumeBus
import dev.themselg.jellymusic.ui.JellyMusicAppRoot
import dagger.hilt.android.AndroidEntryPoint

// AppCompatActivity (extends ComponentActivity) so AppCompatDelegate per-app locale
// switching applies on every API level. Compose, edge-to-edge and Hilt all still work.
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            JellyMusicAppRoot()
        }
    }

    /**
     * While casting, the hardware volume keys should control the Cast device's volume instead
     * of the phone's. We intercept them here and adjust the active Cast session's volume,
     * consuming the event so the phone volume doesn't move. When not casting we return false and
     * the system handles them normally.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        handleCastVolumeKey(event) || super.dispatchKeyEvent(event)

    private fun handleCastVolumeKey(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return false

        val session = runCatching {
            CastContext.getSharedInstance(this).sessionManager.currentCastSession
        }.getOrNull()
        if (session == null || !session.isConnected) return false

        if (event.action == KeyEvent.ACTION_DOWN) {
            val step = if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) VOLUME_STEP else -VOLUME_STEP
            val newVolume = (session.volume + step).coerceIn(0.0, 1.0)
            runCatching { session.volume = newVolume }
            // We consume the key (no system volume HUD), so surface our own overlay instead.
            CastVolumeBus.report(newVolume.toFloat())
        }
        // Consume both DOWN and UP so the system volume UI never appears.
        return true
    }

    private companion object {
        const val VOLUME_STEP = 0.05
    }
}
