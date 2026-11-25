package com.zobaer53.plantapp.core.data.inference

/** Runs the model on one preprocessed input tensor. */
internal interface InferenceEngine {
    /**
     * Main-safe. Returns the model's raw output scores.
     * @throws com.zobaer53.plantapp.core.domain.model.ClassificationException (MODEL_UNAVAILABLE) if the
     *   model cannot be created, (INFERENCE_FAILED) if run fails.
     */
    suspend fun infer(input: FloatArray): FloatArray
}
