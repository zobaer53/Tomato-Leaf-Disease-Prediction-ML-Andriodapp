package com.zobaer53.plantapp.core.domain.testing

import com.zobaer53.plantapp.core.domain.classifier.LeafDiseaseClassifier
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.model.Prediction

class FakeLeafDiseaseClassifier(
    var behavior: suspend (ImageRef) -> List<Prediction> = { PredictionFixtures.predictions() },
) : LeafDiseaseClassifier {
    val requests: MutableList<ImageRef> = mutableListOf()

    override suspend fun classify(image: ImageRef): List<Prediction> {
        requests += image
        return behavior(image)
    }
}
