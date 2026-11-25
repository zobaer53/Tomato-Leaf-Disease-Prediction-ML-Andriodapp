package com.zobaer53.plantapp.feature.diagnosis

import com.zobaer53.plantapp.core.domain.model.Diagnosis
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.ImageRef

data class DiagnosisUiState(
    val image: ImageRef? = null,
    val analysis: AnalysisState = AnalysisState.Idle,
) {
    val canAnalyze: Boolean get() = image != null && analysis !is AnalysisState.Analyzing
}

sealed interface AnalysisState {
    data object Idle : AnalysisState
    data object Analyzing : AnalysisState
    data class Success(val diagnosis: Diagnosis) : AnalysisState
    data class Error(val error: DiagnosisError) : AnalysisState
}

sealed interface DiagnosisAction {
    /** uri == null means the picker was dismissed: no state change. */
    data class ImageSelected(val uri: String?) : DiagnosisAction
    data object AnalyzeClicked : DiagnosisAction
}
