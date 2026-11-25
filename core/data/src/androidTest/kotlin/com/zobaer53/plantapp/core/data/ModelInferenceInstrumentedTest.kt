package com.zobaer53.plantapp.core.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.zobaer53.plantapp.core.data.classifier.LiteRtLeafDiseaseClassifier
import com.zobaer53.plantapp.core.data.classifier.ModelSpec
import com.zobaer53.plantapp.core.data.classifier.ScoreMapper
import com.zobaer53.plantapp.core.data.image.BitmapDecoder
import com.zobaer53.plantapp.core.data.image.UriImageTensorSource
import com.zobaer53.plantapp.core.data.inference.LiteRtInferenceEngine
import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real pipeline (BitmapFactory decode -> bilinear resize -> raw RGB floats -> LiteRT CPU)
 * on a deterministic synthetic image. Golden comparison is optional: put the legacy app's output for
 * [syntheticBitmap] (one float per line) at `src/androidTest/assets/golden/output.txt` to enable it.
 */
@RunWith(AndroidJUnit4::class)
class ModelInferenceInstrumentedTest {

    private val targetContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testContext: Context = InstrumentationRegistry.getInstrumentation().context
    private val spec = ModelSpec.TomatoLeafV1

    private lateinit var inferenceDispatcher: ExecutorCoroutineDispatcher
    private lateinit var engine: LiteRtInferenceEngine
    private lateinit var source: UriImageTensorSource
    private lateinit var imageFile: File

    @Before
    fun setUp() {
        inferenceDispatcher = Executors.newSingleThreadExecutor { r -> Thread(r, "litert-inference-test") }
            .asCoroutineDispatcher()
        engine = LiteRtInferenceEngine(targetContext, spec, inferenceDispatcher)
        source = UriImageTensorSource(BitmapDecoder(targetContext), Dispatchers.IO, Dispatchers.Default)
        imageFile = File(targetContext.cacheDir, "synthetic_leaf_300x200.png").also { file ->
            val bitmap = syntheticBitmap()
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
        }
    }

    @After
    fun tearDown() {
        runBlocking { engine.close() }
        inferenceDispatcher.close()
        imageFile.delete()
    }

    @Test
    fun realModelReturnsTenFiniteScoresSummingToOne() = runBlocking {
        val scores = engine.infer(source.load(imageRef(), spec))

        assertEquals(spec.classCount, scores.size)
        assertTrue("non-finite score in ${scores.toList()}", scores.all { it.isFinite() })
        assertEquals(1f, scores.sum(), 1e-3f)
    }

    @Test
    fun outputMatchesLegacyGoldenWhenPresent() = runBlocking {
        val hasGolden = testContext.assets.list("golden")?.contains("output.txt") == true
        assumeTrue("No golden/output.txt captured (Path B); skipping legacy comparison", hasGolden)
        val golden = testContext.assets.open("golden/output.txt").bufferedReader().use { reader ->
            reader.readLines().filter { it.isNotBlank() }.map { it.trim().toFloat() }.toFloatArray()
        }

        val scores = engine.infer(source.load(imageRef(), spec))

        assertEquals(golden.size, scores.size)
        val maxAbsDiff = golden.indices.maxOf { abs(golden[it] - scores[it]) }
        assertTrue("max |golden - migrated| = $maxAbsDiff", maxAbsDiff < 1e-3f)
    }

    @Test
    fun classifierReturnsOnePredictionPerLabel() = runBlocking {
        val classifier = LiteRtLeafDiseaseClassifier(source, engine, ScoreMapper(spec), spec)

        val predictions = classifier.classify(imageRef())

        assertEquals(LeafDisease.entries.toSet(), predictions.map { it.disease }.toSet())
        assertEquals(spec.classCount, predictions.size)
    }

    @Test
    fun sessionCanBeClosedAndReopened() = runBlocking {
        val tensor = source.load(imageRef(), spec)
        val first = engine.infer(tensor)
        engine.close()
        val second = engine.infer(tensor)

        assertEquals(first.toList(), second.toList())
    }

    @Test
    fun missingFileIsImageUnreadable() = runBlocking {
        val missing = ImageRef(Uri.fromFile(File(targetContext.cacheDir, "does-not-exist.png")).toString())
        try {
            source.load(missing, spec)
            fail("Expected ClassificationException")
        } catch (e: ClassificationException) {
            assertEquals(DiagnosisError.IMAGE_UNREADABLE, e.error)
        }
    }

    private fun imageRef() = ImageRef(Uri.fromFile(imageFile).toString())

    companion object {
        /**
         * 300x200 (not the model size, so the resize path runs), opaque, lossless as PNG.
         * R = x gradient, G = y gradient, B = (x + y) mod 256. Reuse exactly this to capture a legacy golden.
         */
        fun syntheticBitmap(): Bitmap {
            val width = 300
            val height = 200
            val pixels = IntArray(width * height) { i ->
                val x = i % width
                val y = i / width
                val r = x * 255 / (width - 1)
                val g = y * 255 / (height - 1)
                val b = (x + y) % 256
                (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
            return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
        }
    }
}
