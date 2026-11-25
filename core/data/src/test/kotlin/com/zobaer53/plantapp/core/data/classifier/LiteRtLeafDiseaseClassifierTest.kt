package com.zobaer53.plantapp.core.data.classifier

import com.zobaer53.plantapp.core.data.image.ImageTensorSource
import com.zobaer53.plantapp.core.data.inference.InferenceEngine
import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LiteRtLeafDiseaseClassifierTest {

    private class FakeImageTensorSource(
        var behavior: suspend (ImageRef, ModelSpec) -> FloatArray = { _, spec -> FloatArray(spec.inputElementCount) },
    ) : ImageTensorSource {
        val requests = mutableListOf<Pair<ImageRef, ModelSpec>>()
        override suspend fun load(image: ImageRef, spec: ModelSpec): FloatArray {
            requests += image to spec
            return behavior(image, spec)
        }
    }

    private class FakeInferenceEngine(
        var behavior: suspend (FloatArray) -> FloatArray = { FloatArray(10).apply { this[2] = 1f } },
    ) : InferenceEngine {
        val inputs = mutableListOf<FloatArray>()
        override suspend fun infer(input: FloatArray): FloatArray {
            inputs += input
            return behavior(input)
        }
    }

    private val image = ImageRef("content://media/picker/0/42")
    private val spec = ModelSpec.TomatoLeafV1
    private val source = FakeImageTensorSource()
    private val engine = FakeInferenceEngine()
    private val classifier = LiteRtLeafDiseaseClassifier(source, engine, ScoreMapper(spec), spec)

    @Test
    fun `happy path returns one prediction per label`() = runTest {
        val scores = floatArrayOf(0.01f, 0.02f, 0.80f, 0.03f, 0.04f, 0.02f, 0.03f, 0.02f, 0.02f, 0.01f)
        engine.behavior = { scores }

        val predictions = classifier.classify(image)

        assertEquals(10, predictions.size)
        assertEquals(LeafDisease.entries.toSet(), predictions.map { it.disease }.toSet())
        assertEquals(LeafDisease.EARLY_BLIGHT, predictions.maxBy { it.confidence }.disease)
        assertEquals(0.80f, predictions.first { it.disease == LeafDisease.EARLY_BLIGHT }.confidence, 0f)
    }

    @Test
    fun `image and spec are passed to the source and the tensor reaches the engine`() = runTest {
        val tensor = FloatArray(spec.inputElementCount) { it.toFloat() }
        source.behavior = { _, _ -> tensor }

        classifier.classify(image)

        assertEquals(listOf(image to spec), source.requests)
        assertEquals(1, engine.inputs.size)
        assertSame(tensor, engine.inputs.single())
    }

    @Test
    fun `unreadable image propagates and the engine is not called`() = runTest {
        source.behavior = { _, _ -> throw ClassificationException(DiagnosisError.IMAGE_UNREADABLE) }

        val error = runCatchingClassification { classifier.classify(image) }

        assertEquals(DiagnosisError.IMAGE_UNREADABLE, error.error)
        assertTrue(engine.inputs.isEmpty())
    }

    @Test
    fun `engine failures propagate unchanged`() = runTest {
        listOf(DiagnosisError.MODEL_UNAVAILABLE, DiagnosisError.INFERENCE_FAILED).forEach { expected ->
            engine.behavior = { throw ClassificationException(expected) }
            assertEquals(expected, runCatchingClassification { classifier.classify(image) }.error)
        }
    }

    @Test
    fun `malformed engine output becomes INFERENCE_FAILED`() = runTest {
        engine.behavior = { FloatArray(3) }
        assertEquals(DiagnosisError.INFERENCE_FAILED, runCatchingClassification { classifier.classify(image) }.error)
    }

    @Test
    fun `cancellation is not swallowed`() = runTest {
        engine.behavior = { throw CancellationException("cancelled") }
        try {
            classifier.classify(image)
            fail("Expected CancellationException")
        } catch (e: CancellationException) {
            assertEquals("cancelled", e.message)
        }
    }

    private suspend fun runCatchingClassification(block: suspend () -> Unit): ClassificationException {
        try {
            block()
        } catch (e: ClassificationException) {
            return e
        }
        throw AssertionError("Expected ClassificationException")
    }
}
