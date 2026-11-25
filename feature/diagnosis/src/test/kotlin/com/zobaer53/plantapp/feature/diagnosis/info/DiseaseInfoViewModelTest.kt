package com.zobaer53.plantapp.feature.diagnosis.info

import app.cash.turbine.test
import com.zobaer53.plantapp.core.domain.model.DiseaseInfo
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.testing.FakeDiseaseInfoRepository
import com.zobaer53.plantapp.core.domain.usecase.GetDiseaseInfoUseCase
import com.zobaer53.plantapp.feature.diagnosis.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DiseaseInfoViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsInfoForTheGivenDisease_loadingThenContent() = runTest {
        val gate = CompletableDeferred<Unit>()
        val requested = mutableListOf<LeafDisease>()
        val repository = FakeDiseaseInfoRepository { disease ->
            requested += disease
            gate.await()
            DiseaseInfo(disease, "summary", "https://example.org/guide")
        }

        val viewModel = DiseaseInfoViewModel(LeafDisease.SEPTORIA_LEAF_SPOT, GetDiseaseInfoUseCase(repository))

        viewModel.uiState.test {
            assertEquals(DiseaseInfoUiState.Loading, awaitItem())
            gate.complete(Unit)
            assertEquals(
                DiseaseInfoUiState.Content(
                    DiseaseInfo(LeafDisease.SEPTORIA_LEAF_SPOT, "summary", "https://example.org/guide"),
                ),
                awaitItem(),
            )
        }
        assertEquals(listOf(LeafDisease.SEPTORIA_LEAF_SPOT), requested)
    }
}
