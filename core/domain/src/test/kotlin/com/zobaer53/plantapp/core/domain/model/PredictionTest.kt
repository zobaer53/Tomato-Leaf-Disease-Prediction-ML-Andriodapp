package com.zobaer53.plantapp.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PredictionTest {

    private fun p(confidence: Float) = Prediction(LeafDisease.EARLY_BLIGHT, confidence)

    @Test
    fun `accepts the closed range 0 to 1`() {
        assertEquals(0f, p(0f).confidence, 0f)
        assertEquals(0.5f, p(0.5f).confidence, 0f)
        assertEquals(1f, p(1f).confidence, 0f)
    }

    @Test
    fun `rejects confidence below 0`() {
        assertThrows(IllegalArgumentException::class.java) { p(-0.0001f) }
    }

    @Test
    fun `rejects confidence above 1`() {
        assertThrows(IllegalArgumentException::class.java) { p(1.0001f) }
    }

    @Test
    fun `rejects NaN`() {
        assertThrows(IllegalArgumentException::class.java) { p(Float.NaN) }
    }

    @Test
    fun `rejects infinities`() {
        assertThrows(IllegalArgumentException::class.java) { p(Float.POSITIVE_INFINITY) }
        assertThrows(IllegalArgumentException::class.java) { p(Float.NEGATIVE_INFINITY) }
    }

    @Test
    fun `error message names the bad value`() {
        val e = assertThrows(IllegalArgumentException::class.java) { p(1.5f) }

        assertEquals("confidence must be in [0,1] but was 1.5", e.message)
    }

    @Test
    fun `confidencePercent covers the endpoints`() {
        assertEquals(0, p(0f).confidencePercent)
        assertEquals(100, p(1f).confidencePercent)
    }

    @Test
    fun `confidencePercent rounds to the nearest integer`() {
        assertEquals(87, p(0.874f).confidencePercent)
        assertEquals(88, p(0.876f).confidencePercent)
        assertEquals(99, p(0.994f).confidencePercent)
        assertEquals(100, p(0.996f).confidencePercent)
        assertEquals(0, p(0.004f).confidencePercent)
        assertEquals(1, p(0.006f).confidencePercent)
    }

    @Test
    fun `confidencePercent rounds an exact half up`() {
        // 0.125f and 0.375f are exact in binary, so x100 is exactly 12.5 and 37.5.
        assertEquals(13, p(0.125f).confidencePercent)
        assertEquals(38, p(0.375f).confidencePercent)
    }
}
