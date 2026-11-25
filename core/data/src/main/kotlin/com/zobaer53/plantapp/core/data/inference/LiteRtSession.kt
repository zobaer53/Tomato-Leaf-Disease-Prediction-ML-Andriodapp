package com.zobaer53.plantapp.core.data.inference

import android.content.res.AssetManager
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.Environment
import com.google.ai.edge.litert.TensorBuffer
import com.zobaer53.plantapp.core.data.classifier.ModelSpec

/**
 * One CompiledModel (CPU) plus its input/output TensorBuffers, allocated once and reused.
 *
 * NOT thread-safe: create, call [infer] and [close] on the same single inference thread.
 * The constructor runs one warm-up inference and closes everything it allocated if any step throws.
 *
 * @throws com.google.ai.edge.litert.LiteRtException from the constructor if the asset is missing,
 *   is not a valid model, or the runtime cannot allocate buffers.
 */
internal class LiteRtSession(
    assets: AssetManager,
    private val spec: ModelSpec,
    environment: Environment,
) : AutoCloseable {

    // Legacy app ran the Interpreter on CPU without a delegate; stay on CPU for numerical parity.
    private val model: CompiledModel = CompiledModel.create(
        assets,
        spec.assetName,
        CompiledModel.Options(Accelerator.CPU),
        environment,
    )
    private val inputs = mutableListOf<TensorBuffer>()
    private val outputs = mutableListOf<TensorBuffer>()

    init {
        try {
            inputs += model.createInputBuffers()
            outputs += model.createOutputBuffers()
            check(inputs.size == 1 && outputs.size == 1) {
                "Expected 1 input and 1 output tensor but got ${inputs.size} and ${outputs.size}"
            }
            val warmUp = infer(FloatArray(spec.inputElementCount))
            check(warmUp.size == spec.classCount) {
                "Model has ${warmUp.size} outputs but the spec declares ${spec.classCount} labels"
            }
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    /** Blocking. Returns a fresh array with the raw output scores. */
    fun infer(input: FloatArray): FloatArray {
        // writeFloat silently accepts a shorter array, which would produce a wrong result.
        check(input.size == spec.inputElementCount) {
            "Input has ${input.size} floats, model expects ${spec.inputElementCount}"
        }
        inputs[0].writeFloat(input)
        model.run(inputs, outputs)
        return outputs[0].readFloat()
    }

    /** Idempotent. Buffers are closed before the model. */
    override fun close() {
        (inputs + outputs).forEach { it.close() }
        inputs.clear()
        outputs.clear()
        model.close()
    }
}
