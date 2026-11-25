package com.zobaer53.plantapp.feature.diagnosis.info

import com.zobaer53.plantapp.core.domain.model.DiseaseInfo

sealed interface DiseaseInfoUiState {
    data object Loading : DiseaseInfoUiState
    data class Content(val info: DiseaseInfo) : DiseaseInfoUiState
}
