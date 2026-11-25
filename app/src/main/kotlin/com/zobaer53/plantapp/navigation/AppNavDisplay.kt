package com.zobaer53.plantapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.zobaer53.plantapp.feature.diagnosis.navigation.DiagnosisRoute
import com.zobaer53.plantapp.feature.diagnosis.navigation.DiseaseInfoRoute
import com.zobaer53.plantapp.feature.diagnosis.navigation.diagnosisEntries

/**
 * The app's single navigation host. The back stack survives configuration changes and process
 * death; each entry gets its own saveable state and ViewModelStore, so a [DiseaseInfoRoute]
 * ViewModel is created per disease and cleared when the entry is popped.
 */
@Composable
fun AppNavDisplay(
    onOpenGuide: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(DiagnosisRoute)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.pop() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            diagnosisEntries(
                onNavigateToDiseaseInfo = { disease -> backStack.push(DiseaseInfoRoute(disease)) },
                onNavigateBack = { backStack.pop() },
                onOpenGuide = onOpenGuide,
            )
        },
    )
}

/** Ignores a repeat of the current top key, so a double tap cannot stack the same screen twice. */
private fun NavBackStack<NavKey>.push(key: NavKey) {
    if (lastOrNull() != key) add(key)
}

/** Never pops the root: NavDisplay requires a non-empty back stack. */
private fun NavBackStack<NavKey>.pop() {
    if (size > 1) removeAt(lastIndex)
}
