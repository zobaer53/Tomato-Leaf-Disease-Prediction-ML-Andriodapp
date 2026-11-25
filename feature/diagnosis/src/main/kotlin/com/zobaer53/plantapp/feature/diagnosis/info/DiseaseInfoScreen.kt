package com.zobaer53.plantapp.feature.diagnosis.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.zobaer53.plantapp.core.domain.model.DiseaseInfo
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.feature.diagnosis.R
import com.zobaer53.plantapp.feature.diagnosis.ui.LightDarkPreviews
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewData
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewTheme
import com.zobaer53.plantapp.feature.diagnosis.ui.labelRes
import java.net.URI

@Composable
fun DiseaseInfoScreen(
    disease: LeafDisease,
    onBack: () -> Unit,
    onOpenGuide: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiseaseInfoViewModel = hiltViewModel<DiseaseInfoViewModel, DiseaseInfoViewModel.Factory>(
        creationCallback = { factory -> factory.create(disease) },
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiseaseInfoContent(
        disease = disease,
        state = state,
        // Ignore repeated back taps while the exit transition runs.
        onBack = dropUnlessResumed(block = onBack),
        onOpenGuide = onOpenGuide,
        modifier = modifier,
    )
}

/** Replaces the legacy WebView "Solution" screen: summary in-app, full guide in a Custom Tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiseaseInfoContent(
    disease: LeafDisease,
    state: DiseaseInfoUiState,
    onBack: () -> Unit,
    onOpenGuide: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(disease.labelRes()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        when (state) {
            DiseaseInfoUiState.Loading -> {
                val loading = stringResource(R.string.info_loading)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = loading })
                }
            }

            is DiseaseInfoUiState.Content -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                InfoBody(
                    info = state.info,
                    onOpenGuide = onOpenGuide,
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun InfoBody(info: DiseaseInfo, onOpenGuide: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier) {
        InfoSection(
            title = stringResource(if (info.disease.isHealthy) R.string.info_healthy_about else R.string.info_about),
        ) {
            Text(
                text = info.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        info.guideUrl?.let { url ->
            val host = remember(url) { runCatching { URI(url).host }.getOrNull() ?: url }
            InfoSection(title = stringResource(R.string.info_what_to_do)) {
                Text(
                    text = stringResource(R.string.info_guide_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Button(
                    onClick = { onOpenGuide(url) },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Text(stringResource(R.string.info_open_guide))
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Icon(
                        painter = painterResource(R.drawable.ic_open_in_new),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                }
                Text(
                    text = stringResource(R.string.info_guide_source, host),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text(text = stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun InfoSection(title: String, content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            content()
        }
    }
}

@LightDarkPreviews
@Composable
private fun DiseaseInfoContentPreview() {
    PreviewTheme {
        DiseaseInfoContent(
            disease = LeafDisease.EARLY_BLIGHT,
            state = DiseaseInfoUiState.Content(PreviewData.earlyBlightInfo),
            onBack = {},
            onOpenGuide = {},
        )
    }
}

@LightDarkPreviews
@Composable
private fun DiseaseInfoLoadingPreview() {
    PreviewTheme {
        DiseaseInfoContent(
            disease = LeafDisease.EARLY_BLIGHT,
            state = DiseaseInfoUiState.Loading,
            onBack = {},
            onOpenGuide = {},
        )
    }
}
