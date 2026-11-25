package com.zobaer53.plantapp.core.domain.model

import kotlin.math.roundToInt

data class Prediction(val disease: LeafDisease, val confidence: Float) {
    init {
        // NaN fails this check because every comparison with NaN is false.
        require(confidence in 0f..1f) { "confidence must be in [0,1] but was $confidence" }
    }

    val confidencePercent: Int get() = (confidence * 100f).roundToInt()
}
