package com.zobaer53.plantapp.core.domain.usecase

import com.zobaer53.plantapp.core.domain.model.DiseaseInfo
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.testing.FakeDiseaseInfoRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class GetDiseaseInfoUseCaseTest {

    private val requested = mutableListOf<LeafDisease>()
    private val repository = FakeDiseaseInfoRepository()
    private val useCase = GetDiseaseInfoUseCase(repository)

    @Test
    fun `returns the repository's info for the requested disease`() = runTest {
        val info = DiseaseInfo(LeafDisease.LATE_BLIGHT, "Phytophthora infestans", "https://example.org/late")
        repository.behavior = { requested += it; info }

        val result = useCase(LeafDisease.LATE_BLIGHT)

        assertSame(info, result)
        assertEquals(listOf(LeafDisease.LATE_BLIGHT), requested)
    }

    @Test
    fun `forwards every disease unchanged`() = runTest {
        repository.behavior = { requested += it; DiseaseInfo(it, "summary of $it", null) }

        for (disease in LeafDisease.entries) {
            assertEquals(disease, useCase(disease).disease)
        }

        assertEquals(LeafDisease.entries, requested)
    }

    @Test
    fun `a null guide URL is passed through`() = runTest {
        repository.behavior = { DiseaseInfo(it, "No disease signs detected.", null) }

        assertEquals(null, useCase(LeafDisease.HEALTHY).guideUrl)
    }

    @Test
    fun `repository failures propagate`() = runTest {
        val failure = IllegalStateException("no content")
        repository.behavior = { throw failure }

        val thrown = runCatching { useCase(LeafDisease.LEAF_MOLD) }.exceptionOrNull()

        assertSame(failure, thrown)
    }
}
