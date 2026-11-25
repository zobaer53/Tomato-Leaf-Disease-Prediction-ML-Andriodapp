package com.zobaer53.plantapp.core.domain.testing

import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.model.Prediction

object PredictionFixtures {
    /** All 10 classes: [top] gets [topConfidence], the remainder is split evenly. */
    fun predictions(
        top: LeafDisease = LeafDisease.EARLY_BLIGHT,
        topConfidence: Float = 0.9f,
    ): List<Prediction> {
        require(topConfidence in 0f..1f) { "topConfidence must be in [0,1] but was $topConfidence" }
        val others = LeafDisease.entries.filter { it != top }
        val share = ((1f - topConfidence) / others.size).coerceIn(0f, 1f)
        return listOf(Prediction(top, topConfidence)) + others.map { Prediction(it, share) }
    }
}
