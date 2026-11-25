package com.zobaer53.plantapp.core.data.info

import com.zobaer53.plantapp.core.domain.model.DiseaseInfo
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.repository.DiseaseInfoRepository
import javax.inject.Inject

/**
 * Bundled, English-only disease summaries and extension-service guide links. Informational only.
 *
 * Every URL except [DEFAULT_GUIDE_URL] returned HTTP 200 with a matching page title on 2026-11-25.
 * [DEFAULT_GUIDE_URL] is the page the v1 app always opened; its host rejects non-browser clients,
 * so it is used only where no verified, disease-specific page exists.
 */
internal class StaticDiseaseInfoRepository @Inject constructor() : DiseaseInfoRepository {

    override suspend fun getDiseaseInfo(disease: LeafDisease): DiseaseInfo = INFO.getValue(disease)

    companion object {
        const val DEFAULT_GUIDE_URL = "https://extension.umn.edu/plant-diseases/tomato-leaf-spot-diseases"

        private val INFO: Map<LeafDisease, DiseaseInfo> = LeafDisease.entries.associateWith { disease ->
            when (disease) {
                LeafDisease.HEALTHY -> DiseaseInfo(
                    disease,
                    "No disease signs were detected on this leaf.",
                    null,
                )
                LeafDisease.BACTERIAL_SPOT -> DiseaseInfo(
                    disease,
                    "Caused by Xanthomonas bacteria. Look for small, dark, water-soaked spots on the leaves. " +
                        "It spreads by splashing water in warm, wet weather.",
                    "https://content.ces.ncsu.edu/bacterial-spot-of-pepper-and-tomato",
                )
                LeafDisease.EARLY_BLIGHT -> DiseaseInfo(
                    disease,
                    "Caused by the fungus Alternaria. Brown spots with concentric rings appear, " +
                        "usually on the older, lower leaves first.",
                    "https://content.ces.ncsu.edu/early-blight-of-tomato",
                )
                LeafDisease.LATE_BLIGHT -> DiseaseInfo(
                    disease,
                    "Caused by Phytophthora infestans. Large, greasy, grey-green to brown blotches form, with " +
                        "white growth underneath in humid weather. It spreads fast in cool, wet conditions.",
                    "https://content.ces.ncsu.edu/tomato-late-blight",
                )
                LeafDisease.LEAF_MOLD -> DiseaseInfo(
                    disease,
                    "Caused by the fungus Passalora fulva. Pale yellow spots appear on the upper leaf surface " +
                        "with olive, velvety mold below. It thrives in high humidity.",
                    DEFAULT_GUIDE_URL,
                )
                LeafDisease.SEPTORIA_LEAF_SPOT -> DiseaseInfo(
                    disease,
                    "Caused by the fungus Septoria lycopersici. Many small, round spots with dark borders and " +
                        "pale centres appear, starting on the lower leaves.",
                    "https://content.ces.ncsu.edu/septoria-leaf-spot-of-tomato",
                )
                LeafDisease.SPIDER_MITES -> DiseaseInfo(
                    disease,
                    "Two-spotted spider mites (Tetranychus urticae) cause yellow stippling and fine webbing " +
                        "on leaves. They build up in hot, dry weather.",
                    "https://hort.extension.wisc.edu/articles/twospotted-spider-mite/",
                )
                LeafDisease.TARGET_SPOT -> DiseaseInfo(
                    disease,
                    "Caused by the fungus Corynespora cassiicola. Brown lesions with concentric rings and " +
                        "yellow halos form on the leaves.",
                    DEFAULT_GUIDE_URL,
                )
                LeafDisease.MOSAIC_VIRUS -> DiseaseInfo(
                    disease,
                    "Leaves show light and dark green mottling and become distorted. The virus spreads on " +
                        "hands and tools. There is no cure, so remove affected plants.",
                    // Covers the closely related tobacco mosaic virus.
                    "https://hort.extension.wisc.edu/articles/tobacco-mosaic/",
                )
                LeafDisease.YELLOW_LEAF_CURL_VIRUS -> DiseaseInfo(
                    disease,
                    "Spread by whiteflies. Leaves curl upward and turn yellow, and plants are stunted. " +
                        "There is no cure, so control whiteflies and remove affected plants.",
                    "https://content.ces.ncsu.edu/tomato-yellow-leaf-curl-virus",
                )
            }
        }
    }
}
