package com.zobaer53.plantapp.core.data.classifier

import com.zobaer53.plantapp.core.domain.model.LeafDisease
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelLabelsTest {

    @Test
    fun `order matches the legacy switch exactly`() {
        assertEquals(
            listOf(
                LeafDisease.HEALTHY, // case 0: "Healthy leaf"
                LeafDisease.BACTERIAL_SPOT, // case 1: Tomato___Bacterial_spot
                LeafDisease.EARLY_BLIGHT, // case 2: Tomato___Early_blight
                LeafDisease.LATE_BLIGHT, // case 3: Tomato___Late_blight
                LeafDisease.LEAF_MOLD, // case 4: Tomato___Leaf_Mold
                LeafDisease.SEPTORIA_LEAF_SPOT, // case 5: Tomato___Septoria_leaf_spot
                LeafDisease.SPIDER_MITES, // case 6: Tomato___Spider_mites
                LeafDisease.TARGET_SPOT, // case 7: Tomato___Target_Spot
                LeafDisease.MOSAIC_VIRUS, // case 8: Tomato___Tomato_mosaic_virus
                LeafDisease.YELLOW_LEAF_CURL_VIRUS, // case 9: Tomato___Tomato_Yellow_Leaf_Curl_Virus
            ),
            ModelLabels.ORDERED,
        )
    }

    @Test
    fun `there are ten distinct labels`() {
        assertEquals(10, ModelLabels.ORDERED.size)
        assertEquals(10, ModelLabels.ORDERED.toSet().size)
    }

    @Test
    fun `every enum value has an output index`() {
        assertEquals(LeafDisease.entries.toSet(), ModelLabels.ORDERED.toSet())
    }
}
