package dev.themselg.jellymusic.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score

/**
 * Extracts a single Material-You-style seed color from album art.
 *
 * Uses the quantizer + scoring algorithm bundled with MaterialKolor (the same
 * `material-color-utilities` pipeline the system uses for wallpaper-based color):
 * quantize the pixels into a small palette, then score them for suitability as a
 * UI seed (vibrancy, population, hue spread). The top-ranked color is returned.
 */
object SeedColorExtractor {

    private const val SAMPLE_SIZE = 128

    /** Returns the best seed [Color] for [bitmap], or null if none could be derived. */
    fun extract(bitmap: Bitmap): Color? {
        val scaled = downscale(bitmap)
        val width = scaled.width
        val height = scaled.height
        if (width == 0 || height == 0) return null

        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        if (scaled !== bitmap) scaled.recycle()

        val quantized = QuantizerCelebi.quantize(pixels, 128)
        val ranked = Score.score(quantized)
        val argb = ranked.firstOrNull() ?: return null
        return Color(argb)
    }

    private fun downscale(bitmap: Bitmap): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= SAMPLE_SIZE) return bitmap
        val scale = SAMPLE_SIZE.toFloat() / largest
        val w = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }
}
