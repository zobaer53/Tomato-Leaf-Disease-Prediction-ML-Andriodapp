package com.zobaer53.plantapp.feature.diagnosis.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.zobaer53.plantapp.core.domain.model.Diagnosis
import com.zobaer53.plantapp.core.domain.model.DiseaseInfo
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.model.Prediction

/** Light and dark previews. The brand theme lives in :app, so previews use a plain MaterialTheme. */
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
internal annotation class LightDarkPreviews

@Composable
internal fun PreviewTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(content = content)
    }
}

internal object PreviewData {
    private fun diagnosis(vararg scores: Pair<LeafDisease, Float>, isConfident: Boolean): Diagnosis {
        val ranked = scores.map { (disease, confidence) -> Prediction(disease, confidence) }
            .sortedByDescending { it.confidence }
        return Diagnosis(top = ranked.first(), ranked = ranked, isConfident = isConfident)
    }

    val confidentDisease: Diagnosis = diagnosis(
        LeafDisease.EARLY_BLIGHT to 0.91f,
        LeafDisease.TARGET_SPOT to 0.05f,
        LeafDisease.SEPTORIA_LEAF_SPOT to 0.03f,
        LeafDisease.HEALTHY to 0.01f,
        isConfident = true,
    )

    val healthy: Diagnosis = diagnosis(
        LeafDisease.HEALTHY to 0.97f,
        LeafDisease.LEAF_MOLD to 0.02f,
        LeafDisease.SPIDER_MITES to 0.01f,
        isConfident = true,
    )

    val lowConfidence: Diagnosis = diagnosis(
        LeafDisease.LATE_BLIGHT to 0.42f,
        LeafDisease.EARLY_BLIGHT to 0.31f,
        LeafDisease.BACTERIAL_SPOT to 0.19f,
        LeafDisease.HEALTHY to 0.08f,
        isConfident = false,
    )

    val earlyBlightInfo = DiseaseInfo(
        disease = LeafDisease.EARLY_BLIGHT,
        summary = "Caused by the Alternaria fungus. Look for brown spots with concentric rings, " +
            "usually on the older, lower leaves first.",
        guideUrl = "https://content.ces.ncsu.edu/early-blight-of-tomato",
    )
}
