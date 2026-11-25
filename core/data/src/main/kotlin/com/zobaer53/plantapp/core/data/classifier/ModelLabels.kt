package com.zobaer53.plantapp.core.data.classifier

import com.zobaer53.plantapp.core.domain.model.LeafDisease

internal object ModelLabels {
    /**
     * Index i of the model's output tensor <-> ORDERED[i]. Order copied from the legacy `switch(maxPos)`
     * in the v1 MainActivity; the .tflite file carries no label metadata, so this list is the contract.
     */
    val ORDERED: List<LeafDisease> = listOf(
        LeafDisease.HEALTHY,
        LeafDisease.BACTERIAL_SPOT,
        LeafDisease.EARLY_BLIGHT,
        LeafDisease.LATE_BLIGHT,
        LeafDisease.LEAF_MOLD,
        LeafDisease.SEPTORIA_LEAF_SPOT,
        LeafDisease.SPIDER_MITES,
        LeafDisease.TARGET_SPOT,
        LeafDisease.MOSAIC_VIRUS,
        LeafDisease.YELLOW_LEAF_CURL_VIRUS,
    )
}
