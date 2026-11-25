package com.zobaer53.plantapp.feature.diagnosis.ui

import androidx.annotation.StringRes
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.feature.diagnosis.R

@StringRes
internal fun LeafDisease.labelRes(): Int = when (this) {
    LeafDisease.HEALTHY -> R.string.disease_healthy
    LeafDisease.BACTERIAL_SPOT -> R.string.disease_bacterial_spot
    LeafDisease.EARLY_BLIGHT -> R.string.disease_early_blight
    LeafDisease.LATE_BLIGHT -> R.string.disease_late_blight
    LeafDisease.LEAF_MOLD -> R.string.disease_leaf_mold
    LeafDisease.SEPTORIA_LEAF_SPOT -> R.string.disease_septoria_leaf_spot
    LeafDisease.SPIDER_MITES -> R.string.disease_spider_mites
    LeafDisease.TARGET_SPOT -> R.string.disease_target_spot
    LeafDisease.MOSAIC_VIRUS -> R.string.disease_mosaic_virus
    LeafDisease.YELLOW_LEAF_CURL_VIRUS -> R.string.disease_yellow_leaf_curl_virus
}

@StringRes
internal fun DiagnosisError.messageRes(): Int = when (this) {
    DiagnosisError.IMAGE_UNREADABLE -> R.string.error_image_unreadable
    DiagnosisError.MODEL_UNAVAILABLE -> R.string.error_model_unavailable
    DiagnosisError.INFERENCE_FAILED -> R.string.error_inference_failed
}
