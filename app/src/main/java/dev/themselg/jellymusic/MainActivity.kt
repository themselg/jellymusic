package dev.themselg.jellymusic

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
     * While casting, the hardware volume keys should control the Cast device's volume instead of
     * the phone's. The actual handling is flavor-specific ([CastSupport]): the proprietary flavor
     * adjusts the active Cast session; the libre flavor returns false so the system handles them.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        CastSupport.handleVolumeKey(this, event) || super.dispatchKeyEvent(event)
}
