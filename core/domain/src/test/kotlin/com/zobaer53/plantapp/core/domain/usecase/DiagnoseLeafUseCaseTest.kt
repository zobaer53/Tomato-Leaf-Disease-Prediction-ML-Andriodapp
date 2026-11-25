package com.zobaer53.plantapp.core.domain.usecase

import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.DiagnosisResult
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.model.LeafDisease.BACTERIAL_SPOT
import com.zobaer53.plantapp.core.domain.model.LeafDisease.EARLY_BLIGHT
import com.zobaer53.plantapp.core.domain.model.LeafDisease.HEALTHY
import com.zobaer53.plantapp.core.domain.model.LeafDisease.LATE_BLIGHT
import com.zobaer53.plantapp.core.domain.model.LeafDisease.LEAF_MOLD
import com.zobaer53.plantapp.core.domain.model.Prediction
import com.zobaer53.plantapp.core.domain.testing.FakeLeafDiseaseClassifier
import com.zobaer53.plantapp.core.domain.testing.PredictionFixtures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnoseLeafUseCaseTest {

    private val image = ImageRef("content://media/external/images/media/42")
    private val classifier = FakeLeafDiseaseClassifier()
    private val useCase = DiagnoseLeafUseCase(classifier, ThresholdConfidencePolicy())

    private fun p(disease: LeafDisease, confidence: Float) = Prediction(disease, confidence)

    private suspend fun DiagnoseLeafUseCase.success(): DiagnosisResult.Success {
        val result = this(image)
        assertTrue("expected Success but was $result", result is DiagnosisResult.Success)
        return result as DiagnosisResult.Success
    }

    // --- Ranking -----------------------------------------------------------------------------

    @Test
    fun `ranks predictions by confidence descending`() = runTest {
        classifier.behavior = {
            listOf(p(HEALTHY, 0.05f), p(LATE_BLIGHT, 0.7f), p(EARLY_BLIGHT, 0.15f), p(LEAF_MOLD, 0.1f))
        }

        val ranked = useCase.success().diagnosis.ranked

        assertEquals(listOf(LATE_BLIGHT, EARLY_BLIGHT, LEAF_MOLD, HEALTHY), ranked.map { it.disease })
        assertEquals(ranked.sortedByDescending { it.confidence }, ranked)
    }

    @Test
    fun `top is the highest prediction and the first ranked entry`() = runTest {
        classifier.behavior = { PredictionFixtures.predictions(top = BACTERIAL_SPOT, topConfidence = 0.8f).reversed() }

        val diagnosis = useCase.success().diagnosis

        assertEquals(p(BACTERIAL_SPOT, 0.8f), diagnosis.top)
        assertSame(diagnosis.ranked.first(), diagnosis.top)
    }

    @Test
    fun `keeps every prediction the classifier returned`() = runTest {
        val predictions = PredictionFixtures.predictions()
        classifier.behavior = { predictions }

        val ranked = useCase.success().diagnosis.ranked

        assertEquals(predictions.size, ranked.size)
        assertEquals(predictions.toSet(), ranked.toSet())
    }

    @Test
    fun `ties keep the classifier's order`() = runTest {
        classifier.behavior = {
            listOf(p(LEAF_MOLD, 0.3f), p(LATE_BLIGHT, 0.4f), p(HEALTHY, 0.3f), p(EARLY_BLIGHT, 0f))
        }

        val ranked = useCase.success().diagnosis.ranked

        assertEquals(listOf(LATE_BLIGHT, LEAF_MOLD, HEALTHY, EARLY_BLIGHT), ranked.map { it.disease })
    }

    @Test
    fun `a full tie keeps the classifier's order and picks its first entry as top`() = runTest {
        val tied = listOf(p(EARLY_BLIGHT, 0.25f), p(HEALTHY, 0.25f), p(LEAF_MOLD, 0.25f), p(LATE_BLIGHT, 0.25f))
        classifier.behavior = { tied }

        val diagnosis = useCase.success().diagnosis

        assertEquals(tied, diagnosis.ranked)
        assertEquals(EARLY_BLIGHT, diagnosis.top.disease)
    }

    @Test
    fun `a single prediction is a valid diagnosis`() = runTest {
        classifier.behavior = { listOf(p(HEALTHY, 1f)) }

        val diagnosis = useCase.success().diagnosis

        assertEquals(p(HEALTHY, 1f), diagnosis.top)
        assertEquals(listOf(p(HEALTHY, 1f)), diagnosis.ranked)
        assertTrue(diagnosis.isHealthy)
    }

    @Test
    fun `passes the image to the classifier exactly once`() = runTest {
        useCase(image)

        assertEquals(listOf(image), classifier.requests)
    }

    // --- Confidence policy -------------------------------------------------------------------

    @Test
    fun `top above the default threshold is confident`() = runTest {
        classifier.behavior = { PredictionFixtures.predictions(topConfidence = 0.61f) }

        assertTrue(useCase.success().diagnosis.isConfident)
    }

    @Test
    fun `top below the default threshold is not confident`() = runTest {
        classifier.behavior = { PredictionFixtures.predictions(topConfidence = 0.59f) }

        assertFalse(useCase.success().diagnosis.isConfident)
    }

    @Test
    fun `top exactly at the threshold is confident`() = runTest {
        classifier.behavior = {
            PredictionFixtures.predictions(topConfidence = ThresholdConfidencePolicy.DEFAULT_MIN_CONFIDENCE)
        }

        assertTrue(useCase.success().diagnosis.isConfident)
    }

    @Test
    fun `asks the policy about the top prediction only, once`() = runTest {
        classifier.behavior = { listOf(p(HEALTHY, 0.2f), p(LATE_BLIGHT, 0.5f), p(LEAF_MOLD, 0.3f)) }
        val asked = mutableListOf<Prediction>()
        val recordingUseCase = DiagnoseLeafUseCase(classifier) { top -> asked += top; true }

        recordingUseCase.success()

        assertEquals(listOf(p(LATE_BLIGHT, 0.5f)), asked)
    }

    @Test
    fun `isConfident comes from the injected policy, not from the confidence value`() = runTest {
        classifier.behavior = { PredictionFixtures.predictions(topConfidence = 0.99f) }

        val neverConfident = DiagnoseLeafUseCase(classifier) { false }
        val alwaysConfident = DiagnoseLeafUseCase(classifier) { true }

        assertFalse(neverConfident.success().diagnosis.isConfident)
        classifier.behavior = { PredictionFixtures.predictions(topConfidence = 0.11f) }
        assertTrue(alwaysConfident.success().diagnosis.isConfident)
    }

    // --- Failures ----------------------------------------------------------------------------

    @Test
    fun `ClassificationException maps to Failure with the same error`() = runTest {
        for (error in DiagnosisError.entries) {
            classifier.behavior = { throw ClassificationException(error, cause = RuntimeException("boom")) }

            assertEquals(DiagnosisResult.Failure(error), useCase(image))
        }
    }

    @Test
    fun `empty classifier output is an inference failure`() = runTest {
        classifier.behavior = { emptyList() }

        assertEquals(DiagnosisResult.Failure(DiagnosisError.INFERENCE_FAILED), useCase(image))
    }

    @Test
    fun `a thrown CancellationException propagates instead of becoming a Failure`() = runTest {
        val cancellation = CancellationException("cancelled by caller")
        classifier.behavior = { throw cancellation }

        val thrown = runCatching { useCase(image) }.exceptionOrNull()

        assertSame(cancellation, thrown)
    }

    @Test
    fun `cancelling the caller cancels a classification in flight`() = runTest {
        classifier.behavior = { awaitCancellation() }
        var result: DiagnosisResult? = null

        // UNDISPATCHED runs the use case up to the classifier's suspension point right away.
        val job = launch(start = CoroutineStart.UNDISPATCHED) { result = useCase(image) }
        assertEquals(listOf(image), classifier.requests)

        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertNull(result)
    }

    @Test
    fun `a non-domain exception propagates instead of becoming a Failure`() = runTest {
        val bug = IllegalStateException("programmer error")
        classifier.behavior = { throw bug }

        val thrown = runCatching { useCase(image) }.exceptionOrNull()

        assertSame(bug, thrown)
    }
}
