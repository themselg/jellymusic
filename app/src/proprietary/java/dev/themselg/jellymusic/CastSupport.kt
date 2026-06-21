// SPDX-FileCopyrightText: 2026 Guillermo Themsel
// SPDX-License-Identifier: GPL-3.0-or-later

package dev.themselg.jellymusic

import android.app.Activity
import android.app.Application
import android.view.KeyEvent
import com.google.android.gms.cast.framework.CastContext
import dev.themselg.jellymusic.player.cast.CastVolumeBus

/**
 * proprietary flavor: Google Cast support for the app shell.
 * - [init] warms up CastContext so the MediaRouteProvider registers for device discovery.
 * - [handleVolumeKey] routes the hardware volume keys to the active Cast session's volume.
 */
object CastSupport {

    private const val VOLUME_STEP = 0.05

    fun init(app: Application) {
        // Guarded: no-op on devices without Play Services.
        runCatching { CastContext.getSharedInstance(app) }
    }

    fun handleVolumeKey(activity: Activity, event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!isVolumeKey) return false

        val session = runCatching {
            CastContext.getSharedInstance(activity).sessionManager.currentCastSession
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
}
