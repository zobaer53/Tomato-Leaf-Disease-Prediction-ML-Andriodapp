package com.zobaer53.plantapp.core.data.info

import com.zobaer53.plantapp.core.domain.model.LeafDisease
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticDiseaseInfoRepositoryTest {

    private val repository = StaticDiseaseInfoRepository()

    @Test
    fun `every disease has info for itself with a summary`() = runTest {
        LeafDisease.entries.forEach { disease ->
            val info = repository.getDiseaseInfo(disease)
            assertEquals(disease, info.disease)
            assertTrue("blank summary for $disease", info.summary.isNotBlank())
        }
    }

    @Test
    fun `healthy has no guide`() = runTest {
        assertNull(repository.getDiseaseInfo(LeafDisease.HEALTHY).guideUrl)
    }

    @Test
    fun `every disease other than healthy has an https guide`() = runTest {
        LeafDisease.entries.filterNot { it.isHealthy }.forEach { disease ->
            val url = repository.getDiseaseInfo(disease).guideUrl
            assertNotNull("missing guide for $disease", url)
            assertTrue("not https for $disease: $url", url!!.startsWith("https://"))
        }
    }

    @Test
    fun `diseases without a verified page fall back to the legacy guide`() = runTest {
        listOf(LeafDisease.LEAF_MOLD, LeafDisease.TARGET_SPOT).forEach { disease ->
            assertEquals(StaticDiseaseInfoRepository.DEFAULT_GUIDE_URL, repository.getDiseaseInfo(disease).guideUrl)
        }
    }
}
