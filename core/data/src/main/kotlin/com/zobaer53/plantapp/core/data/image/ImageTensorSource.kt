package com.zobaer53.plantapp.core.data.image

import com.zobaer53.plantapp.core.data.classifier.ModelSpec
import com.zobaer53.plantapp.core.domain.model.ImageRef

/** Turns a user-selected image into the model's input tensor. */
internal interface ImageTensorSource {
    /**
     * Main-safe. Returns exactly [ModelSpec.inputElementCount] floats in NHWC RGB order.
     * @throws com.zobaer53.plantapp.core.domain.model.ClassificationException (IMAGE_UNREADABLE)
     */
    suspend fun load(image: ImageRef, spec: ModelSpec): FloatArray
}
