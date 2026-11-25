package com.zobaer53.plantapp.feature.diagnosis.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.zobaer53.plantapp.core.domain.model.DiagnosisError
import com.zobaer53.plantapp.feature.diagnosis.R
import com.zobaer53.plantapp.feature.diagnosis.ui.LightDarkPreviews
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewTheme
import com.zobaer53.plantapp.feature.diagnosis.ui.messageRes

private val StatusCardShape = RoundedCornerShape(24.dp)

/** Idle state: photo tips before a photo is chosen, a nudge to analyze once one is. */
@Composable
internal fun TipsCard(hasImage: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = StatusCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            if (hasImage) {
                TipRow(R.string.tips_ready, iconRes = R.drawable.ic_search)
            } else {
                Text(
                    text = stringResource(R.string.tips_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                TipRow(R.string.tips_one_leaf)
                TipRow(R.string.tips_light)
                TipRow(R.string.tips_focus)
            }
        }
    }
}

@Composable
private fun TipRow(@StringRes text: Int, iconRes: Int = R.drawable.ic_eco) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Analyzing state, announced politely to screen readers. */
@Composable
internal fun AnalyzingCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        shape = StatusCardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.analyzing_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.analyzing_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Failure state, announced assertively. Retry is the main button ("Try again"). */
@Composable
internal fun ErrorCard(error: DiagnosisError, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Assertive },
        shape = StatusCardShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Icon(painter = painterResource(R.drawable.ic_error), contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.error_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(error.messageRes()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@LightDarkPreviews
@Composable
private fun StatusCardsPreview() {
    PreviewTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            TipsCard(hasImage = false)
            TipsCard(hasImage = true)
            AnalyzingCard()
            ErrorCard(DiagnosisError.IMAGE_UNREADABLE)
        }
    }
}
