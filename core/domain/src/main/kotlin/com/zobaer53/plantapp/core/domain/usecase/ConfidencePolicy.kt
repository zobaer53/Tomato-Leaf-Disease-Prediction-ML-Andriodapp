package com.zobaer53.plantapp.core.domain.usecase

import com.zobaer53.plantapp.core.domain.model.Prediction

fun interface ConfidencePolicy {
    fun isConfident(top: Prediction): Boolean
}

class ThresholdConfidencePolicy(
    private val minConfidence: Float = DEFAULT_MIN_CONFIDENCE,
) : ConfidencePolicy {
    init {
        require(minConfidence in 0f..1f) { "minConfidence must be in [0,1] but was $minConfidence" }
    }

    override fun isConfident(top: Prediction): Boolean = top.confidence >= minConfidence

    companion object {
        /** Uncalibrated starting point; tune against labelled photos. */
        const val DEFAULT_MIN_CONFIDENCE: Float = 0.60f
    }
}
