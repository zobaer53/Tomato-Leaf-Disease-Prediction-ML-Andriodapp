package com.zobaer53.plantapp.core.domain.model

/** [ranked] is sorted by confidence descending; [top] == ranked.first(). */
data class Diagnosis(val top: Prediction, val ranked: List<Prediction>, val isConfident: Boolean) {
    init {
        require(ranked.isNotEmpty() && ranked.first() == top)
    }

    val isHealthy: Boolean get() = top.disease.isHealthy
}
