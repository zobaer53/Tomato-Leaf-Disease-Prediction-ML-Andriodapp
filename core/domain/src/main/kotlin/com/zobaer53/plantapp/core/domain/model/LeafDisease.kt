package com.zobaer53.plantapp.core.domain.model

/** Declaration order is NOT the model contract; the data layer owns index mapping (ModelLabels). */
enum class LeafDisease {
    HEALTHY, BACTERIAL_SPOT, EARLY_BLIGHT, LATE_BLIGHT, LEAF_MOLD,
    SEPTORIA_LEAF_SPOT, SPIDER_MITES, TARGET_SPOT, MOSAIC_VIRUS, YELLOW_LEAF_CURL_VIRUS;

    val isHealthy: Boolean get() = this == HEALTHY
}
