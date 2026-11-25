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
         * mean=0, std=1 => raw 0..255 floats: byte-for-byte the legacy TensorImage(FLOAT32).load(bitmap).
         *
         * The tensor names suggest a Teachable Machine export, which is usually trained on [-1, 1]
         * inputs (mean=127.5, std=127.5). That is unproven, so legacy parity is kept; change it here
         * only after an A/B on labelled photos.
         */
        val TomatoLeafV1 = ModelSpec(
            assetName = "tomato_leaf_disease.tflite",
            inputWidth = 224,
            inputHeight = 224,
            channels = 3,
            mean = 0f,
            std = 1f,
            labels = ModelLabels.ORDERED,
        )
    }
}
