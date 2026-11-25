package com.zobaer53.plantapp.core.domain.repository

import com.zobaer53.plantapp.core.domain.model.DiseaseInfo
import com.zobaer53.plantapp.core.domain.model.LeafDisease

interface DiseaseInfoRepository {
    suspend fun getDiseaseInfo(disease: LeafDisease): DiseaseInfo
}
