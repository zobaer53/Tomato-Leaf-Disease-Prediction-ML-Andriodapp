package com.zobaer53.plantapp.core.data.inference

import android.content.Context
import com.google.ai.edge.litert.Environment
import com.google.ai.edge.litert.LiteRtException
import com.zobaer53.plantapp.core.data.classifier.ModelSpec
import com.zobaer53.plantapp.core.data.di.InferenceDispatcher
import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Owns the process-wide [LiteRtSession]. [dispatcher] must be single-threaded: the session (and the
 * `CompiledModel` inside it) is created, run and closed only inside `withContext(dispatcher)`, and the
 * blocks never suspend, so calls are serialized on that one thread and need no lock.
 *
 * The session is created lazily on the first [infer]; a failed creation is retried on the next call.
 */
@Singleton
internal class LiteRtInferenceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spec: ModelSpec,
    @InferenceDispatcher private val dispatcher: CoroutineDispatcher,
) : InferenceEngine {

    // Both fields are read and written only on the inference thread.
    private var environment: Environment? = null
    private var session: LiteRtSession? = null

    override suspend fun infer(input: FloatArray): FloatArray = withContext(dispatcher) {
        val active = session ?: openSession().also { session = it }
        try {
            active.infer(input)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LiteRtException) {
            throw ClassificationException(DiagnosisError.INFERENCE_FAILED, "LiteRT run failed", e)
        } catch (e: IllegalStateException) {
            // Input size mismatch caught by LiteRtSession before writeFloat.
            throw ClassificationException(DiagnosisError.INFERENCE_FAILED, e.message, e)
        }
    }

    /** Frees the native model and buffers. A later [infer] creates a new session. */
    suspend fun close() {
        withContext(dispatcher) {
            session?.close()
            session = null
        }
    }

    private fun openSession(): LiteRtSession = try {
        val env = environment ?: Environment.create().also { environment = it }
        LiteRtSession(context.assets, spec, env)
    } catch (e: CancellationException) {
        throw e
    } catch (e: LiteRtException) {
        // Missing or invalid asset, or the runtime could not compile/allocate the model.
        throw ClassificationException(DiagnosisError.MODEL_UNAVAILABLE, "Could not create the LiteRT model", e)
    } catch (e: IllegalStateException) {
        // The model's tensor layout does not match ModelSpec (checked during warm-up).
        throw ClassificationException(DiagnosisError.MODEL_UNAVAILABLE, e.message, e)
    } catch (e: LinkageError) {
        // libLiteRt / litert_jni missing for this ABI (e.g. 32-bit x86): UnsatisfiedLinkError, then
        // NoClassDefFoundError on later attempts.
        throw ClassificationException(DiagnosisError.MODEL_UNAVAILABLE, "LiteRT native runtime unavailable", e)
    }
}
