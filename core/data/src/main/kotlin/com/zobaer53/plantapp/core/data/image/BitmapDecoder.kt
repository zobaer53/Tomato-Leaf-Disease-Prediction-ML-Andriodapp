package com.zobaer53.plantapp.core.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/** Longest side, in pixels, decoded at full resolution. Larger images are subsampled to avoid OOM. */
internal const val MAX_DECODE_SIDE: Int = 4096

/** Smallest power of two s such that max(width,height)/s <= maxSide. */
internal fun computeSampleSize(width: Int, height: Int, maxSide: Int = MAX_DECODE_SIDE): Int {
    require(maxSide > 0) { "maxSide must be positive but was $maxSide" }
    val longest = maxOf(width, height)
    var sample = 1
    // Compare with multiplication: integer division would truncate (8193 / 2 == 4096).
    while (longest.toLong() > maxSide.toLong() * sample) sample *= 2
    return sample
}

internal class BitmapDecoder @Inject constructor(@ApplicationContext private val context: Context) {

    /**
     * BitmapFactory via ContentResolver.openInputStream; ARGB_8888; NO EXIF rotation (legacy parity).
     * Null if undecodable.
     *
     * Images up to [MAX_DECODE_SIDE] px decode at full size exactly like the legacy
     * `MediaStore.Images.Media.getBitmap`; larger ones are subsampled by a power of two.
     *
     * Blocking: call from an I/O dispatcher.
     * @throws IOException if the URI cannot be opened (FileNotFoundException for unknown or missing content).
     * @throws SecurityException if the URI grant was revoked or never given.
     */
    fun decode(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight)
        }
        return open(uri).use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun open(uri: Uri) =
        context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException("No content for $uri")
}
