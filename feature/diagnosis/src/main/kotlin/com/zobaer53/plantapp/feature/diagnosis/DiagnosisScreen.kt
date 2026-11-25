package com.zobaer53.plantapp.feature.diagnosis

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.feature.diagnosis.components.AnalyzingCard
import com.zobaer53.plantapp.feature.diagnosis.components.DiagnosisResultCard
import com.zobaer53.plantapp.feature.diagnosis.components.ErrorCard
import com.zobaer53.plantapp.feature.diagnosis.components.LeafImageCard
import com.zobaer53.plantapp.feature.diagnosis.components.TipsCard
import com.zobaer53.plantapp.feature.diagnosis.ui.LightDarkPreviews
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewData
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewTheme

/** Width at which the picker and the result sit side by side (Material "expanded"). */
private val TwoPaneMinWidth: Dp = 840.dp
private val SinglePaneMaxWidth: Dp = 600.dp
private val TwoPaneMaxWidth: Dp = 1080.dp
private val ButtonMaxWidth: Dp = 420.dp
private val ButtonMinHeight: Dp = 52.dp

@Composable
fun DiagnosisScreen(
    onShowDiseaseInfo: (LeafDisease) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosisViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // System photo picker: no storage permission needed (backported to API 19+ via Play services).
    val pickImage = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        viewModel.onAction(DiagnosisAction.ImageSelected(uri?.toString()))
    }

    // Same guard as dropUnlessResumed: ignore taps that land mid-transition, so a double tap
    // can't push the info screen twice.
    val lifecycleOwner = LocalLifecycleOwner.current
    DiagnosisContent(
        state = state,
        onPickImage = { pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
        onAnalyze = { viewModel.onAction(DiagnosisAction.AnalyzeClicked) },
        onShowDiseaseInfo = { disease ->
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                onShowDiseaseInfo(disease)
            }
        },
        modifier = modifier,
    )
}

/**
 * Legacy flow (Select -> Predict -> result -> Solution) as a single scrolling screen.
 * Content is capped at 600dp and centred; at 840dp+ the picker and the result sit side by side.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosisContent(
    state: DiagnosisUiState,
    onPickImage: () -> Unit,
    onAnalyze: () -> Unit,
    onShowDiseaseInfo: (LeafDisease) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.diagnosis_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        // safeDrawing also covers display cutouts in landscape.
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
        ) {
            val twoPane = maxWidth >= TwoPaneMinWidth
            // Padding goes inside the scroll so content scrolls behind the system bars.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
            ) {
                if (twoPane) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        modifier = Modifier
                            .widthIn(max = TwoPaneMaxWidth)
                            .fillMaxWidth(),
                    ) {
                        PhotoPane(state, onPickImage, onAnalyze, imageDiameter = 240.dp, Modifier.weight(1f))
                        AnalysisPane(state, onShowDiseaseInfo, scrollIntoView = false, Modifier.weight(1f))
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier
                            .widthIn(max = SinglePaneMaxWidth)
                            .fillMaxWidth(),
                    ) {
                        PhotoPane(state, onPickImage, onAnalyze, imageDiameter = 200.dp)
                        AnalysisPane(state, onShowDiseaseInfo, scrollIntoView = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoPane(
    state: DiagnosisUiState,
    onPickImage: () -> Unit,
    onAnalyze: () -> Unit,
    imageDiameter: Dp,
    modifier: Modifier = Modifier,
) {
    val analysis = state.analysis
    val isAnalyzing = analysis is AnalysisState.Analyzing
    val needsNewPhoto = analysis is AnalysisState.Success ||
        (analysis is AnalysisState.Error && analysis.error == DiagnosisError.IMAGE_UNREADABLE)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.diagnosis_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        LeafImageCard(
            image = state.image,
            isAnalyzing = isAnalyzing,
            onClick = onPickImage,
            diameter = imageDiameter,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = ButtonMaxWidth)
                .fillMaxWidth(),
        ) {
            when {
                state.image == null -> PrimaryButton(
                    text = stringResource(R.string.diagnosis_select_photo),
                    iconRes = R.drawable.ic_add_photo,
                    onClick = onPickImage,
                )

                // After a result (legacy hid Predict here) or an unreadable photo, the useful next
                // step is another photo, not another run on the same one.
                needsNewPhoto -> PrimaryButton(
                    text = stringResource(R.string.diagnosis_change_photo),
                    iconRes = R.drawable.ic_add_photo,
                    onClick = onPickImage,
                )

                else -> {
                    AnalyzeButton(
                        isAnalyzing = isAnalyzing,
                        isRetry = analysis is AnalysisState.Error,
                        enabled = state.canAnalyze,
                        onClick = onAnalyze,
                    )
                    // Still enabled while analyzing: a new photo cancels the running analysis.
                    OutlinedButton(
                        onClick = onPickImage,
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = ButtonMinHeight),
                    ) {
                        ButtonIcon(R.drawable.ic_add_photo)
                        Text(stringResource(R.string.diagnosis_change_photo))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalysisPane(
    state: DiagnosisUiState,
    onShowDiseaseInfo: (LeafDisease) -> Unit,
    scrollIntoView: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = state.analysis,
        contentKey = { it::class },
        transitionSpec = {
            fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) using
                SizeTransform(clip = false)
        },
        label = "analysis",
        modifier = modifier.fillMaxWidth(),
    ) { target ->
        // On phones the status/result lands below the buttons: scroll the incoming card into view.
        // The requester sits on the card itself because the container is still animating its size.
        val bringIntoView = remember { BringIntoViewRequester() }
        if (scrollIntoView && target !is AnalysisState.Idle) {
            LaunchedEffect(Unit) {
                withFrameNanos { } // wait until the card has been laid out
                bringIntoView.bringIntoView()
            }
        }
        val cardModifier = Modifier.bringIntoViewRequester(bringIntoView)
        when (target) {
            AnalysisState.Idle -> TipsCard(hasImage = state.image != null, modifier = cardModifier)
            AnalysisState.Analyzing -> AnalyzingCard(modifier = cardModifier)
            is AnalysisState.Success -> DiagnosisResultCard(target.diagnosis, onShowDiseaseInfo, cardModifier)
            is AnalysisState.Error -> ErrorCard(target.error, modifier = cardModifier)
        }
    }
}

@Composable
private fun PrimaryButton(text: String, iconRes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonMinHeight),
    ) {
        ButtonIcon(iconRes)
        Text(text)
    }
}

@Composable
private fun AnalyzeButton(isAnalyzing: Boolean, isRetry: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ButtonMinHeight),
    ) {
        if (isAnalyzing) {
            CircularProgressIndicator(
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.diagnosis_analyzing))
        } else {
            ButtonIcon(R.drawable.ic_search)
            Text(stringResource(if (isRetry) R.string.diagnosis_try_again else R.string.diagnosis_analyze))
        }
    }
}

@Composable
private fun ButtonIcon(iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
    )
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
}

// region Previews

private val previewImage = ImageRef("content://preview/leaf.jpg")

@Composable
private fun DiagnosisContentPreview(state: DiagnosisUiState) {
    PreviewTheme {
        DiagnosisContent(state = state, onPickImage = {}, onAnalyze = {}, onShowDiseaseInfo = {})
    }
}

@LightDarkPreviews
@Composable
private fun IdlePreview() = DiagnosisContentPreview(DiagnosisUiState())

@LightDarkPreviews
@Composable
private fun ImageSelectedPreview() = DiagnosisContentPreview(DiagnosisUiState(image = previewImage))

@LightDarkPreviews
@Composable
private fun AnalyzingPreview() =
    DiagnosisContentPreview(DiagnosisUiState(image = previewImage, analysis = AnalysisState.Analyzing))

@LightDarkPreviews
@Composable
private fun ConfidentResultPreview() = DiagnosisContentPreview(
    DiagnosisUiState(image = previewImage, analysis = AnalysisState.Success(PreviewData.confidentDisease)),
)

@LightDarkPreviews
@Composable
private fun LowConfidenceResultPreview() = DiagnosisContentPreview(
    DiagnosisUiState(image = previewImage, analysis = AnalysisState.Success(PreviewData.lowConfidence)),
)

@LightDarkPreviews
@Composable
private fun ErrorPreview() = DiagnosisContentPreview(
    DiagnosisUiState(image = previewImage, analysis = AnalysisState.Error(DiagnosisError.INFERENCE_FAILED)),
)

@Preview(name = "Tablet, result", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
private fun TabletResultPreview() = DiagnosisContentPreview(
    DiagnosisUiState(image = previewImage, analysis = AnalysisState.Success(PreviewData.confidentDisease)),
)

// endregion
