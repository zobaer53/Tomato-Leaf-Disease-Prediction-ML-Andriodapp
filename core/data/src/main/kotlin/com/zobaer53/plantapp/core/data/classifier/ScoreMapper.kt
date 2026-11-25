package com.zobaer53.plantapp.core.data.classifier

import com.zobaer53.plantapp.core.domain.model.ClassificationException
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.Prediction
import javax.inject.Inject

/** Pairs raw softmax scores with the labels of [spec], in model output order. */
internal class ScoreMapper @Inject constructor(private val spec: ModelSpec) {

    /** size != classCount or any non-finite score -> ClassificationException(INFERENCE_FAILED). Scores coerced into [0,1]. */
    fun map(scores: FloatArray): List<Prediction> {
        if (scores.size != spec.classCount) {
            throw ClassificationException(
                DiagnosisError.INFERENCE_FAILED,
                "Model returned ${scores.size} scores, expected ${spec.classCount}",
            )
        }
        val nonFinite = scores.indexOfFirst { !it.isFinite() }
        if (nonFinite >= 0) {
            throw ClassificationException(
                DiagnosisError.INFERENCE_FAILED,
                "Model returned a non-finite score (${scores[nonFinite]}) at index $nonFinite",
            )
        }
        // Softmax output can drift a few ULPs outside [0,1]; Prediction requires the closed range.
        return scores.mapIndexed { index, score -> Prediction(spec.labels[index], score.coerceIn(0f, 1f)) }
    }
}
