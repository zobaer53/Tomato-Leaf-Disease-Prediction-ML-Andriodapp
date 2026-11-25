package com.zobaer53.plantapp.core.data.classifier

import com.zobaer53.plantapp.core.domain.model.LeafDisease

/** Input/output contract of a bundled image-classification model. */
internal data class ModelSpec(
    val assetName: String,
    val inputWidth: Int,
    val inputHeight: Int,
    val channels: Int,
    val mean: Float,
    val std: Float,
    val labels: List<LeafDisease>,
) {
    val inputElementCount: Int get() = inputWidth * inputHeight * channels
    val classCount: Int get() = labels.size

    companion object {
        /**
         * mean=127.5, std=127.5 => inputs scaled to [-1, 1], which is what the model was trained on.
         *
         * v1 fed raw 0..255 floats (mean=0, std=1). On 150 labelled PlantVillage photos (15 per
         * class) that scored 12.7% top-1 at ~94% average confidence, while [-1, 1] scored 93.3%.
         * Reproduce with tools/model-eval/evaluate.py.
         */
        val TomatoLeafV1 = ModelSpec(
            assetName = "tomato_leaf_disease.tflite",
            inputWidth = 224,
            inputHeight = 224,
            channels = 3,
            mean = 127.5f,
            std = 127.5f,
            labels = ModelLabels.ORDERED,
        )
    }
}
