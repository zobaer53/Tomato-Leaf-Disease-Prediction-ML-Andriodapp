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
         * MobileNetV3-Large fine-tuned on PlantVillage + PlantDoc tomato photos
         * (tools/model-training/train_tomato_model.ipynb), float16 weights.
         *
         * mean=0, std=1 => raw 0..255 RGB floats. Scaling to [-1, 1] happens inside the model, so the
         * app and the training pipeline cannot disagree about it (v1's mistake).
         *
         * Held-out accuracy (tools/model-training/metrics_v2.json): 99.0% on PlantVillage lab photos,
         * 55.1% on PlantDoc field photos. The previous model scored 95.1% and 21.7% on the same sets.
         */
        val TomatoLeafV2 = ModelSpec(
            assetName = "tomato_leaf_disease_v2.tflite",
            inputWidth = 224,
            inputHeight = 224,
            channels = 3,
            mean = 0f,
            std = 1f,
            labels = ModelLabels.ORDERED,
        )
    }
}
