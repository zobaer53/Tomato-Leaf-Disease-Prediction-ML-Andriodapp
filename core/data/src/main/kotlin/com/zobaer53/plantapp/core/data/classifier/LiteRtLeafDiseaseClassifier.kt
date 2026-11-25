package com.zobaer53.plantapp.core.data.classifier

import com.zobaer53.plantapp.core.data.image.ImageTensorSource
import com.zobaer53.plantapp.core.data.inference.InferenceEngine
import com.zobaer53.plantapp.core.domain.classifier.LeafDiseaseClassifier
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.model.Prediction
import javax.inject.Inject

/**
 * image -> NHWC float tensor -> LiteRT CompiledModel (CPU) -> one Prediction per label.
 * Main-safe: each collaborator moves its own work off the caller's thread.
 * Every expected failure is a ClassificationException; cancellation propagates untouched.
 */
internal class LiteRtLeafDiseaseClassifier @Inject constructor(
    private val tensorSource: ImageTensorSource,
    private val engine: InferenceEngine,
    private val scoreMapper: ScoreMapper,
    private val spec: ModelSpec,
) : LeafDiseaseClassifier {
    override suspend fun classify(image: ImageRef): List<Prediction> =
        scoreMapper.map(engine.infer(tensorSource.load(image, spec)))
}
