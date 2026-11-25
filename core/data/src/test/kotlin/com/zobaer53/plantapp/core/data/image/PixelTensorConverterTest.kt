package com.zobaer53.plantapp.core.data.image

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PixelTensorConverterTest {

    private fun argb(a: Int, r: Int, g: Int, b: Int): Int = (a shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun `channels are emitted in R, G, B order`() {
        val out = PixelTensorConverter.argbToRgbFloats(intArrayOf(argb(255, 10, 20, 30)))
        assertArrayEquals(floatArrayOf(10f, 20f, 30f), out, 0f)
    }

    @Test
    fun `alpha is ignored`() {
        val opaque = PixelTensorConverter.argbToRgbFloats(intArrayOf(argb(255, 1, 2, 3)))
        val transparent = PixelTensorConverter.argbToRgbFloats(intArrayOf(argb(0, 1, 2, 3)))
        val half = PixelTensorConverter.argbToRgbFloats(intArrayOf(argb(128, 1, 2, 3)))
        assertArrayEquals(opaque, transparent, 0f)
        assertArrayEquals(opaque, half, 0f)
    }

    @Test
    fun `default mean and std give raw 0 to 255 values (legacy parity)`() {
        val out = PixelTensorConverter.argbToRgbFloats(
            intArrayOf(argb(255, 0, 0, 0), argb(255, 255, 255, 255), argb(255, 255, 0, 128)),
        )
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 255f, 255f, 255f, 255f, 0f, 128f), out, 0f)
    }

    @Test
    fun `output length is three times the pixel count`() {
        assertEquals(0, PixelTensorConverter.argbToRgbFloats(IntArray(0)).size)
        assertEquals(224 * 224 * 3, PixelTensorConverter.argbToRgbFloats(IntArray(224 * 224)).size)
    }

    @Test
    fun `pixels keep their row-major order`() {
        // A 2x2 image, row-major: (0,0) (1,0) / (0,1) (1,1); the red channel encodes the index.
        val pixels = IntArray(4) { argb(255, it, 100 + it, 200 + it) }
        val out = PixelTensorConverter.argbToRgbFloats(pixels)
        for (i in 0 until 4) {
            assertEquals(i.toFloat(), out[3 * i], 0f)
            assertEquals((100 + i).toFloat(), out[3 * i + 1], 0f)
            assertEquals((200 + i).toFloat(), out[3 * i + 2], 0f)
        }
    }

    @Test
    fun `mean and std are applied per channel as (v - mean) div std`() {
        val out = PixelTensorConverter.argbToRgbFloats(
            intArrayOf(argb(255, 0, 255, 127)),
            mean = 127.5f,
            std = 127.5f,
        )
        assertArrayEquals(floatArrayOf(-1f, 1f, (127f - 127.5f) / 127.5f), out, 1e-6f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero std is rejected`() {
        PixelTensorConverter.argbToRgbFloats(IntArray(1), std = 0f)
    }
}
