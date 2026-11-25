package com.zobaer53.plantapp.core.data.classifier

import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScoreMapperTest {

    private val mapper = ScoreMapper(ModelSpec.TomatoLeafV2)

    @Test
    fun `score i is paired with label i`() {
        val scores = FloatArray(10) { it / 100f }
        val predictions = mapper.map(scores)

        assertEquals(10, predictions.size)
        predictions.forEachIndexed { i, prediction ->
            assertEquals(ModelLabels.ORDERED[i], prediction.disease)
            assertEquals(scores[i], prediction.confidence, 0f)
        }
    }

    @Test
    fun `one-hot output maps to that disease`() {
        val scores = FloatArray(10).apply { this[6] = 1f }
        val top = mapper.map(scores).maxBy { it.confidence }
        assertEquals(LeafDisease.SPIDER_MITES, top.disease)
    }

    @Test
    fun `wrong size is INFERENCE_FAILED`() {
        listOf(FloatArray(0), FloatArray(9), FloatArray(11)).forEach { scores ->
            val e = assertThrows(ClassificationException::class.java) { mapper.map(scores) }
            assertEquals(DiagnosisError.INFERENCE_FAILED, e.error)
        }
    }

    @Test
    fun `non-finite score is INFERENCE_FAILED`() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { bad ->
            val scores = FloatArray(10) { 0.1f }.apply { this[3] = bad }
            val e = assertThrows(ClassificationException::class.java) { mapper.map(scores) }
            assertEquals(DiagnosisError.INFERENCE_FAILED, e.error)
        }
    }

    @Test
    fun `scores slightly outside 0 to 1 are coerced`() {
        val scores = FloatArray(10).apply {
            this[0] = 1.0000001f
            this[1] = -1e-7f
            this[2] = 3f
        }
        val predictions = mapper.map(scores)
        assertEquals(1f, predictions[0].confidence, 0f)
        assertEquals(0f, predictions[1].confidence, 0f)
        assertEquals(1f, predictions[2].confidence, 0f)
    }
}
