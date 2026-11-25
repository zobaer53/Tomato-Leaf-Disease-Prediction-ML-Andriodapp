package com.zobaer53.plantapp.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosisTest {

    private val lateBlight = Prediction(LeafDisease.LATE_BLIGHT, 0.7f)
    private val healthy = Prediction(LeafDisease.HEALTHY, 0.2f)
    private val leafMold = Prediction(LeafDisease.LEAF_MOLD, 0.1f)

    @Test
    fun `accepts a ranking whose first entry is top`() {
        val diagnosis = Diagnosis(top = lateBlight, ranked = listOf(lateBlight, healthy, leafMold), isConfident = true)

        assertEquals(lateBlight, diagnosis.top)
        assertEquals(3, diagnosis.ranked.size)
        assertTrue(diagnosis.isConfident)
    }

    @Test
    fun `accepts a single-entry ranking`() {
        val diagnosis = Diagnosis(top = healthy, ranked = listOf(healthy), isConfident = false)

        assertEquals(listOf(healthy), diagnosis.ranked)
        assertFalse(diagnosis.isConfident)
    }

    @Test
    fun `top may be an equal but distinct instance of the first entry`() {
        val copy = lateBlight.copy()

        Diagnosis(top = copy, ranked = listOf(lateBlight, healthy), isConfident = true)
    }

    @Test
    fun `rejects an empty ranking`() {
        assertThrows(IllegalArgumentException::class.java) {
            Diagnosis(top = lateBlight, ranked = emptyList(), isConfident = true)
        }
    }

    @Test
    fun `rejects a top that is not the first ranked entry`() {
        assertThrows(IllegalArgumentException::class.java) {
            Diagnosis(top = healthy, ranked = listOf(lateBlight, healthy), isConfident = true)
        }
    }

    @Test
    fun `rejects a top that is missing from the ranking`() {
        assertThrows(IllegalArgumentException::class.java) {
            Diagnosis(top = leafMold, ranked = listOf(lateBlight, healthy), isConfident = true)
        }
    }

    @Test
    fun `rejects a top with the right disease but a different confidence`() {
        assertThrows(IllegalArgumentException::class.java) {
            Diagnosis(top = Prediction(LeafDisease.LATE_BLIGHT, 0.9f), ranked = listOf(lateBlight), isConfident = true)
        }
    }

    @Test
    fun `isHealthy is true only when top is HEALTHY`() {
        val healthyTop = Prediction(LeafDisease.HEALTHY, 0.8f)

        assertTrue(Diagnosis(healthyTop, listOf(healthyTop, leafMold), isConfident = true).isHealthy)
        assertFalse(Diagnosis(lateBlight, listOf(lateBlight, healthy), isConfident = true).isHealthy)
    }

    @Test
    fun `isHealthy ignores HEALTHY lower in the ranking`() {
        val diagnosis = Diagnosis(lateBlight, listOf(lateBlight, healthy), isConfident = false)

        assertFalse(diagnosis.isHealthy)
    }

    @Test
    fun `LeafDisease isHealthy holds for HEALTHY alone`() {
        assertEquals(listOf(LeafDisease.HEALTHY), LeafDisease.entries.filter { it.isHealthy })
    }
}
