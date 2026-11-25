package com.zobaer53.plantapp.core.data.classifier

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelSpecTest {

    private val spec = ModelSpec.TomatoLeafV1

    @Test
    fun `tomato model keeps legacy raw 0 to 255 normalization`() {
        assertEquals(0f, spec.mean, 0f)
        assertEquals(1f, spec.std, 0f)
    }

    @Test
    fun `tomato model contract matches the tflite file`() {
        assertEquals("tomato_leaf_disease.tflite", spec.assetName)
        assertEquals(224, spec.inputWidth)
        assertEquals(224, spec.inputHeight)
        assertEquals(3, spec.channels)
        assertEquals(1 * 224 * 224 * 3, spec.inputElementCount)
        assertEquals(10, spec.classCount)
        assertEquals(ModelLabels.ORDERED, spec.labels)
    }
}
