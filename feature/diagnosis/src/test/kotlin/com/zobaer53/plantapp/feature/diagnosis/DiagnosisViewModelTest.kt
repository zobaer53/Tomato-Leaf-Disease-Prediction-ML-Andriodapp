package com.zobaer53.plantapp.feature.diagnosis

import app.cash.turbine.test
import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.model.Prediction
import com.zobaer53.plantapp.core.domain.testing.FakeLeafDiseaseClassifier
import com.zobaer53.plantapp.core.domain.testing.PredictionFixtures
import com.zobaer53.plantapp.core.domain.usecase.DiagnoseLeafUseCase
import com.zobaer53.plantapp.core.domain.usecase.ThresholdConfidencePolicy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiagnosisViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val classifier = FakeLeafDiseaseClassifier()
    private val viewModel = DiagnosisViewModel(DiagnoseLeafUseCase(classifier, ThresholdConfidencePolicy()))

    /** Makes the fake classifier suspend until [complete] is called. */
    private fun gateClassifier(result: List<Prediction> = PredictionFixtures.predictions()): CompletableDeferred<Unit> {
        val gate = CompletableDeferred<Unit>()
        classifier.behavior = {
            gate.await()
            result
        }
        return gate
    }

    @Test
    fun initialState_hasNoImageAndIsIdle() {
        val state = viewModel.uiState.value
        assertNull(state.image)
        assertEquals(AnalysisState.Idle, state.analysis)
        assertFalse(state.canAnalyze)
    }

    @Test
    fun imageSelectedNull_isNoOp() = runTest {
        viewModel.uiState.test {
            assertEquals(DiagnosisUiState(), awaitItem())
            viewModel.onAction(DiagnosisAction.ImageSelected(null))
            expectNoEvents()
        }
    }

    @Test
    fun imageSelectedNull_keepsExistingResult() = runTest {
        viewModel.onAction(DiagnosisAction.ImageSelected(URI_A))
        viewModel.onAction(DiagnosisAction.AnalyzeClicked)
        val before = viewModel.uiState.value
        assertTrue(before.analysis is AnalysisState.Success)

        viewModel.onAction(DiagnosisAction.ImageSelected(null))

        assertEquals(before, viewModel.uiState.value)
    }

    @Test
    fun imageSelected_setsImageAndResetsAnalysis() = runTest {
        viewModel.onAction(DiagnosisAction.ImageSelected(URI_A))
        viewModel.onAction(DiagnosisAction.AnalyzeClicked)
        assertTrue(viewModel.uiState.value.analysis is AnalysisState.Success)

        viewModel.onAction(DiagnosisAction.ImageSelected(URI_B))

        val state = viewModel.uiState.value
        assertEquals(ImageRef(URI_B), state.image)
        assertEquals(AnalysisState.Idle, state.analysis)
        assertTrue(state.canAnalyze)
    }

    @Test
    fun analyzeWithoutImage_isNoOp() = runTest {
        viewModel.uiState.test {
            assertEquals(DiagnosisUiState(), awaitItem())
            viewModel.onAction(DiagnosisAction.AnalyzeClicked)
            expectNoEvents()
        }
        assertTrue(classifier.requests.isEmpty())
    }

    @Test
    fun analyze_goesAnalyzingThenSuccess() = runTest {
        val gate = gateClassifier(PredictionFixtures.predictions(LeafDisease.LATE_BLIGHT, 0.9f))
        viewModel.onAction(DiagnosisAction.ImageSelected(URI_A))

        viewModel.uiState.test {
            assertEquals(AnalysisState.Idle, awaitItem().analysis)

            viewModel.onAction(DiagnosisAction.AnalyzeClicked)
            val analyzing = awaitItem()
            assertEquals(AnalysisState.Analyzing, analyzing.analysis)
            assertFalse(analyzing.canAnalyze)

            gate.complete(Unit)
            val success = awaitItem().analysis as AnalysisState.Success
            assertEquals(LeafDisease.LATE_BLIGHT, success.diagnosis.top.disease)
            assertTrue(success.diagnosis.isConfident)
        }
        assertEquals(listOf(ImageRef(URI_A)), classifier.requests)
    }

    @Test
    fun classifierFailure_givesErrorWithMatchingReason() = runTest {
        classifier.behavior = { throw ClassificationException(DiagnosisError.IMAGE_UNREADABLE) }
        viewModel.onAction(DiagnosisAction.ImageSelected(URI_A))

        viewModel.onAction(DiagnosisAction.AnalyzeClicked)

        val state = viewModel.uiState.value
        assertEquals(AnalysisState.Error(DiagnosisError.IMAGE_UNREADABLE), state.analysis)
        assertTrue("an error can be retried", state.canAnalyze)
    }

    @Test
    fun emptyPredictions_givesInferenceFailedError() = runTest {
        classifier.behavior = { emptyList() }
        viewModel.onAction(DiagnosisAction.ImageSelected(URI_A))

        viewModel.onAction(DiagnosisAction.AnalyzeClicked)

        assertEquals(AnalysisState.Error(DiagnosisError.INFERENCE_FAILED), viewModel.uiState.value.analysis)
    }

    @Test
    fun secondAnalyzeWhileAnalyzing_isIgnored() = runTest {
        val gate = gateClassifier()
        viewModel.onAction(DiagnosisAction.ImageSelected(URI_A))

        viewModel.onAction(DiagnosisAction.AnalyzeClicked)
        viewModel.onAction(DiagnosisAction.AnalyzeClicked)
        assertEquals(1, classifier.requests.size)

        gate.complete(Unit)
        assertTrue(viewModel.uiState.value.analysis is AnalysisState.Success)
        assertEquals(1, classifier.requests.size)
    }

    @Test
    fun selectingNewImageMidAnalysis_cancelsAndResets() = runTest {
        val gate = CompletableDeferred<Unit>()
        var cancelled = false
        classifier.behavior = {
            try {
                gate.await()
            } catch (e: CancellationException) {
                cancelled = true
                throw e
            }
            PredictionFixtures.predictions()
        }
        viewModel.onAction(DiagnosisAction.ImageSelected(URI_A))
        viewModel.onAction(DiagnosisAction.AnalyzeClicked)
        assertEquals(AnalysisState.Analyzing, viewModel.uiState.value.analysis)

        viewModel.onAction(DiagnosisAction.ImageSelected(URI_B))

        assertTrue("running analysis is cancelled", cancelled)
        val state = viewModel.uiState.value
        assertEquals(ImageRef(URI_B), state.image)
        assertEquals(AnalysisState.Idle, state.analysis)

        // A late completion of the old request must not overwrite the new image's state.
        gate.complete(Unit)
        assertEquals(AnalysisState.Idle, viewModel.uiState.value.analysis)
    }

    @Test
    fun lowConfidenceResult_isNotConfidentButKeepsBestGuess() = runTest {
        classifier.behavior = { PredictionFixtures.predictions(LeafDisease.LEAF_MOLD, topConfidence = 0.4f) }
        viewModel.onAction(DiagnosisAction.ImageSelected(URI_A))

        viewModel.onAction(DiagnosisAction.AnalyzeClicked)

        val diagnosis = (viewModel.uiState.value.analysis as AnalysisState.Success).diagnosis
        assertFalse(diagnosis.isConfident)
        assertEquals(LeafDisease.LEAF_MOLD, diagnosis.top.disease)
    }

    private companion object {
        const val URI_A = "content://media/picker/0/1"
        const val URI_B = "content://media/picker/0/2"
    }
}
