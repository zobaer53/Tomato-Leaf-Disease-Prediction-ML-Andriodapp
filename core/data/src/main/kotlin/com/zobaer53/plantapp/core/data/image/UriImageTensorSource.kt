package com.zobaer53.plantapp.core.data.image

import android.graphics.Bitmap
import android.net.Uri
import com.zobaer53.plantapp.core.data.classifier.ModelSpec
import com.zobaer53.plantapp.core.data.di.DefaultDispatcher
import com.zobaer53.plantapp.core.data.di.IoDispatcher
import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.ImageRef
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Legacy-parity preprocessing: full decode (no EXIF rotation) -> bilinear stretch to the model size
 * (`createScaledBitmap(.., filter = true)`, no crop) -> getPixels -> raw RGB floats per [ModelSpec].
 */
internal class UriImageTensorSource @Inject constructor(
    private val decoder: BitmapDecoder,
    @IoDispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val default: CoroutineDispatcher,
) : ImageTensorSource {

    override suspend fun load(image: ImageRef, spec: ModelSpec): FloatArray {
        val source = withContext(io) { decodeOrThrow(image) }
        return withContext(default) { toTensor(source, spec) }
    }

    private fun decodeOrThrow(image: ImageRef): Bitmap {
        val bitmap = try {
            decoder.decode(Uri.parse(image.value))
        } catch (e: IOException) {
            throw unreadable(image, e)
        } catch (e: SecurityException) {
            // URI permission revoked or never granted (photo picker grants are temporary).
            throw unreadable(image, e)
        } catch (e: IllegalArgumentException) {
            // Malformed URI or an argument BitmapFactory rejects.
            throw unreadable(image, e)
        } catch (e: OutOfMemoryError) {
            // A huge (but < MAX_DECODE_SIDE) image on a small heap; the failed allocation is reclaimable.
            throw unreadable(image, e)
        }
        return bitmap ?: throw ClassificationException(
            DiagnosisError.IMAGE_UNREADABLE,
            "Could not decode an image from ${image.value}",
        )
    }

    private fun toTensor(source: Bitmap, spec: ModelSpec): FloatArray {
        val width = spec.inputWidth
        val height = spec.inputHeight
        val scaled = Bitmap.createScaledBitmap(source, width, height, /* filter = */ true)
        if (scaled !== source) source.recycle()
        // getPixels cannot read HARDWARE bitmaps; BitmapFactory never returns one, but stay defensive.
        val argb = if (scaled.config == Bitmap.Config.ARGB_8888) {
            scaled
        } else {
            (scaled.copy(Bitmap.Config.ARGB_8888, false) ?: throw ClassificationException(
                DiagnosisError.IMAGE_UNREADABLE,
                "Could not convert a ${scaled.config} bitmap to ARGB_8888",
            )).also { scaled.recycle() }
        }
        val pixels = IntArray(width * height)
        argb.getPixels(pixels, 0, width, 0, 0, width, height)
        argb.recycle()
        return PixelTensorConverter.argbToRgbFloats(pixels, spec.mean, spec.std)
    }

    private fun unreadable(image: ImageRef, cause: Throwable) =
        ClassificationException(DiagnosisError.IMAGE_UNREADABLE, "Could not read ${image.value}", cause)
}
