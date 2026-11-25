package com.zobaer53.plantapp.core.domain.testing

import com.zobaer53.plantapp.core.domain.model.DiseaseInfo
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.repository.DiseaseInfoRepository

class FakeDiseaseInfoRepository(
    var behavior: suspend (LeafDisease) -> DiseaseInfo = {
        DiseaseInfo(it, "summary of $it", "https://example.org/$it")
    },
) : DiseaseInfoRepository {
    override suspend fun getDiseaseInfo(disease: LeafDisease): DiseaseInfo = behavior(disease)
}
