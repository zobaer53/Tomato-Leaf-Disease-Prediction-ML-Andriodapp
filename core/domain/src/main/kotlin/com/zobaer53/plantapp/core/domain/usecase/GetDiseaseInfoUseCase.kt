package com.zobaer53.plantapp.core.domain.usecase

import com.zobaer53.plantapp.core.domain.model.DiseaseInfo
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.repository.DiseaseInfoRepository

class GetDiseaseInfoUseCase(private val repository: DiseaseInfoRepository) {
    suspend operator fun invoke(disease: LeafDisease): DiseaseInfo = repository.getDiseaseInfo(disease)
}
