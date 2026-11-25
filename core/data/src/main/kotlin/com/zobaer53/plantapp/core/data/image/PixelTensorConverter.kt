package com.zobaer53.plantapp.core.data.image

/**
 * Pure-Kotlin port of the legacy `TensorImage(DataType.FLOAT32).load(bitmap)` conversion
 * (tensorflow-lite-support 0.1.0 `ImageConversions.convertBitmapToTensorBuffer`).
 */
internal object PixelTensorConverter {

    /** ARGB_8888 ints -> NHWC RGB floats: out[3i]=(R-mean)/std, out[3i+1]=(G-mean)/std, out[3i+2]=(B-mean)/std. Alpha ignored. */
    fun argbToRgbFloats(pixels: IntArray, mean: Float = 0f, std: Float = 1f): FloatArray {
        require(std != 0f && std.isFinite()) { "std must be finite and non-zero but was $std" }
        require(mean.isFinite()) { "mean must be finite but was $mean" }
        val out = FloatArray(pixels.size * 3)
        for (i in pixels.indices) {
            val p = pixels[i]
            val o = i * 3
            out[o] = ((p shr 16 and 0xFF) - mean) / std
            out[o + 1] = ((p shr 8 and 0xFF) - mean) / std
            out[o + 2] = ((p and 0xFF) - mean) / std
        }
        return out
    }
}
