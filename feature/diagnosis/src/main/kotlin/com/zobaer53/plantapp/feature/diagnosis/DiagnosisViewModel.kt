package com.zobaer53.plantapp.feature.diagnosis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zobaer53.plantapp.core.domain.model.DiagnosisResult
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.usecase.DiagnoseLeafUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    private val diagnoseLeaf: DiagnoseLeafUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosisUiState())
    val uiState: StateFlow<DiagnosisUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null

    fun onAction(action: DiagnosisAction) {
        when (action) {
            is DiagnosisAction.ImageSelected -> onImageSelected(action.uri)
            DiagnosisAction.AnalyzeClicked -> analyze()
        }
    }

    private fun onImageSelected(uri: String?) {
        // A dismissed picker (or an unusable blank result) leaves the screen as it was.
        if (uri.isNullOrBlank()) return
        analysisJob?.cancel()
        analysisJob = null
        _uiState.update { it.copy(image = ImageRef(uri), analysis = AnalysisState.Idle) }
    }

    private fun analyze() {
        val current = _uiState.value
        if (!current.canAnalyze) return
        val image = current.image ?: return

        _uiState.update { it.copy(analysis = AnalysisState.Analyzing) }
        analysisJob = viewModelScope.launch {
            val analysis = when (val result = diagnoseLeaf(image)) {
                is DiagnosisResult.Success -> AnalysisState.Success(result.diagnosis)
                is DiagnosisResult.Failure -> AnalysisState.Error(result.error)
            }
            // Only publish the result for the image it was computed from.
            _uiState.update { state -> if (state.image == image) state.copy(analysis = analysis) else state }
        }
    }
}
