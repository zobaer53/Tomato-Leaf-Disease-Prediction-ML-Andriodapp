package com.zobaer53.plantapp.feature.diagnosis.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.feature.diagnosis.DiagnosisScreen
import com.zobaer53.plantapp.feature.diagnosis.info.DiseaseInfoScreen
import kotlinx.serialization.Serializable

// The ONLY symbols :app may reference from this module.

@Serializable
data object DiagnosisRoute : NavKey

@Serializable
data class DiseaseInfoRoute(val disease: LeafDisease) : NavKey

/**
 * Registers both screens. The host must add `rememberViewModelStoreNavEntryDecorator()` so each
 * [DiseaseInfoRoute] gets its own DiseaseInfoViewModel.
 */
fun EntryProviderScope<NavKey>.diagnosisEntries(
    onNavigateToDiseaseInfo: (LeafDisease) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenGuide: (url: String) -> Unit,
) {
    entry<DiagnosisRoute> {
        DiagnosisScreen(onShowDiseaseInfo = onNavigateToDiseaseInfo)
    }
    entry<DiseaseInfoRoute> { key ->
        DiseaseInfoScreen(disease = key.disease, onBack = onNavigateBack, onOpenGuide = onOpenGuide)
    }
}
