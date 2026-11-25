package com.zobaer53.plantapp.core.data.image

import org.junit.Assert.assertEquals
import org.junit.Test

class SampleSizeTest {

    @Test
    fun `images up to the limit decode at full size`() {
        assertEquals(1, computeSampleSize(1, 1))
        assertEquals(1, computeSampleSize(300, 200))
        assertEquals(1, computeSampleSize(4096, 4096))
        assertEquals(1, computeSampleSize(4096, 10))
    }

    @Test
    fun `one pixel over the limit halves`() {
        assertEquals(2, computeSampleSize(4097, 100))
        assertEquals(2, computeSampleSize(100, 4097))
        assertEquals(2, computeSampleSize(8192, 6144))
    }

    @Test
    fun `result is the smallest power of two that fits`() {
        assertEquals(4, computeSampleSize(8193, 1))
        assertEquals(4, computeSampleSize(16384, 1))
        assertEquals(8, computeSampleSize(16385, 1))
    }

    @Test
    fun `custom max side is honoured`() {
        assertEquals(1, computeSampleSize(224, 224, maxSide = 224))
        assertEquals(2, computeSampleSize(225, 224, maxSide = 224))
        assertEquals(16, computeSampleSize(4000, 3000, maxSide = 256))
    }

    @Test
    fun `degenerate sizes return 1`() {
        assertEquals(1, computeSampleSize(0, 0))
        assertEquals(1, computeSampleSize(-1, -1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-positive max side is rejected`() {
        computeSampleSize(10, 10, maxSide = 0)
    }
}
