package com.zobaer53.plantapp.core.domain.classifier

import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.model.Prediction

interface LeafDiseaseClassifier {
    /**
     * Main-safe. Returns exactly one Prediction per class the model knows, in any order.
     * @throws com.zobaer53.plantapp.core.domain.model.ClassificationException for every expected
     *   failure (never returns partial results).
     * Must let CancellationException propagate.
     */
    suspend fun classify(image: ImageRef): List<Prediction>
}
