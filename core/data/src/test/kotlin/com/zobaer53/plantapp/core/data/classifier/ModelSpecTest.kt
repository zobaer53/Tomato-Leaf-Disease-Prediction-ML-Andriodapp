package com.zobaer53.plantapp.core.data.classifier

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelSpecTest {

    private val spec = ModelSpec.TomatoLeafV1

    @Test
    fun `tomato model scales inputs to minus 1 to 1 as it was trained`() {
        assertEquals(127.5f, spec.mean, 0f)
        assertEquals(127.5f, spec.std, 0f)
        assertEquals(-1f, (0f - spec.mean) / spec.std, 0f)
        assertEquals(1f, (255f - spec.mean) / spec.std, 0f)
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
