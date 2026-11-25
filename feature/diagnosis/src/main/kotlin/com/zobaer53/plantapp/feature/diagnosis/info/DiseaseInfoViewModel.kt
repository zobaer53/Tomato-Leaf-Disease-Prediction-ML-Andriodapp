package com.zobaer53.plantapp.feature.diagnosis.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.usecase.GetDiseaseInfoUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = DiseaseInfoViewModel.Factory::class)
class DiseaseInfoViewModel @AssistedInject constructor(
    @Assisted private val disease: LeafDisease,
    private val getDiseaseInfo: GetDiseaseInfoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiseaseInfoUiState>(DiseaseInfoUiState.Loading)
    val uiState: StateFlow<DiseaseInfoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = DiseaseInfoUiState.Content(getDiseaseInfo(disease))
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(disease: LeafDisease): DiseaseInfoViewModel
    }
}
