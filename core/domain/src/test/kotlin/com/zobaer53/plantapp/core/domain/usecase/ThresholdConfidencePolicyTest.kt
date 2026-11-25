package com.zobaer53.plantapp.core.domain.usecase

import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.model.Prediction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ThresholdConfidencePolicyTest {

    private fun top(confidence: Float) = Prediction(LeafDisease.LATE_BLIGHT, confidence)

    @Test
    fun `default threshold is 0_60`() {
        assertEquals(0.60f, ThresholdConfidencePolicy.DEFAULT_MIN_CONFIDENCE, 0f)
    }

    @Test
    fun `default policy uses the default threshold`() {
        val policy = ThresholdConfidencePolicy()

        assertTrue(policy.isConfident(top(0.60f)))
        assertTrue(policy.isConfident(top(0.6001f)))
        assertFalse(policy.isConfident(top(0.5999f)))
    }

    @Test
    fun `confident above the threshold`() {
        assertTrue(ThresholdConfidencePolicy(0.75f).isConfident(top(0.9f)))
    }

    @Test
    fun `not confident below the threshold`() {
        assertFalse(ThresholdConfidencePolicy(0.75f).isConfident(top(0.74f)))
    }

    @Test
    fun `exactly at the threshold is confident`() {
        assertTrue(ThresholdConfidencePolicy(0.75f).isConfident(top(0.75f)))
    }

    @Test
    fun `threshold 0 accepts everything, including zero confidence`() {
        val policy = ThresholdConfidencePolicy(0f)

        assertTrue(policy.isConfident(top(0f)))
        assertTrue(policy.isConfident(top(1f)))
    }

    @Test
    fun `threshold 1 accepts only full confidence`() {
        val policy = ThresholdConfidencePolicy(1f)

        assertTrue(policy.isConfident(top(1f)))
        assertFalse(policy.isConfident(top(0.9999f)))
    }

    @Test
    fun `rejects a threshold below 0`() {
        assertThrows(IllegalArgumentException::class.java) { ThresholdConfidencePolicy(-0.01f) }
    }

    @Test
    fun `rejects a threshold above 1`() {
        assertThrows(IllegalArgumentException::class.java) { ThresholdConfidencePolicy(1.01f) }
    }

    @Test
    fun `rejects a NaN threshold`() {
        assertThrows(IllegalArgumentException::class.java) { ThresholdConfidencePolicy(Float.NaN) }
    }
}
