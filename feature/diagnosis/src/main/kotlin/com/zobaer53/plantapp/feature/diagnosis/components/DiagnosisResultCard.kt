package com.zobaer53.plantapp.feature.diagnosis.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zobaer53.plantapp.core.domain.model.Diagnosis
import com.zobaer53.plantapp.core.domain.model.LeafDisease
import com.zobaer53.plantapp.core.domain.model.Prediction
import com.zobaer53.plantapp.feature.diagnosis.R
import com.zobaer53.plantapp.feature.diagnosis.ui.LightDarkPreviews
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewData
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewTheme
import com.zobaer53.plantapp.feature.diagnosis.ui.labelRes

private const val TOP_MATCH_COUNT = 3

/** Visual treatment for a result: healthy, likely diseased, or low confidence. */
private data class ResultTone(
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
    val accent: Color,
    val container: Color,
    val onContainer: Color,
)

@Composable
private fun Diagnosis.tone(): ResultTone {
    val colors = MaterialTheme.colorScheme
    return when {
        !isConfident -> ResultTone(
            R.string.result_unsure_title, R.drawable.ic_info,
            colors.tertiary, colors.tertiaryContainer, colors.onTertiaryContainer,
        )
        isHealthy -> ResultTone(
            R.string.result_healthy_title, R.drawable.ic_check_circle,
            colors.primary, colors.primaryContainer, colors.onPrimaryContainer,
        )
        else -> ResultTone(
            R.string.result_disease_title, R.drawable.ic_warning,
            colors.error, colors.errorContainer, colors.onErrorContainer,
        )
    }
}

/**
 * The legacy result TextView + SOLUTION button, expanded: label, confidence, top-3 with bars,
 * a "not sure" banner when the model is unsure, and the info button only for a disease.
 */
@Composable
internal fun DiagnosisResultCard(
    diagnosis: Diagnosis,
    onShowDiseaseInfo: (LeafDisease) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tone = diagnosis.tone()
    val top = diagnosis.top

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = tone.container, contentColor = tone.onContainer) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                        Icon(painter = painterResource(tone.icon), contentDescription = null)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
                    Text(
                        text = stringResource(tone.title),
                        style = MaterialTheme.typography.labelLarge,
                        color = tone.accent,
                    )
                    Text(
                        text = stringResource(top.disease.labelRes()),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.result_confidence, top.confidencePercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ConfidenceBar(confidence = top.confidence, color = tone.accent)

            if (!diagnosis.isConfident) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = tone.container,
                    contentColor = tone.onContainer,
                ) {
                    Text(
                        text = stringResource(R.string.result_unsure_body),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = stringResource(R.string.result_top_matches),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                diagnosis.ranked.take(TOP_MATCH_COUNT).forEach { prediction ->
                    MatchRow(
                        prediction = prediction,
                        isTop = prediction == top,
                        accent = tone.accent,
                    )
                }
            }

            if (!diagnosis.isHealthy) {
                Button(
                    onClick = { onShowDiseaseInfo(top.disease) },
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.result_learn_more))
                }
            }

            Text(
                text = stringResource(R.string.disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MatchRow(prediction: Prediction, isTop: Boolean, accent: Color) {
    val label = stringResource(prediction.disease.labelRes())
    val description = stringResource(R.string.result_match_description, label, prediction.confidencePercent)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isTop) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.percent_value, prediction.confidencePercent),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ConfidenceBar(
            confidence = prediction.confidence,
            color = if (isTop) accent else MaterialTheme.colorScheme.secondary,
            thickness = 6.dp,
        )
    }
}

@LightDarkPreviews
@Composable
private fun DiseaseResultPreview() {
    PreviewTheme {
        DiagnosisResultCard(PreviewData.confidentDisease, onShowDiseaseInfo = {}, Modifier.padding(16.dp))
    }
}

@LightDarkPreviews
@Composable
private fun HealthyResultPreview() {
    PreviewTheme {
        DiagnosisResultCard(PreviewData.healthy, onShowDiseaseInfo = {}, Modifier.padding(16.dp))
    }
}

@LightDarkPreviews
@Composable
private fun LowConfidenceResultPreview() {
    PreviewTheme {
        DiagnosisResultCard(PreviewData.lowConfidence, onShowDiseaseInfo = {}, Modifier.padding(16.dp))
    }
}
