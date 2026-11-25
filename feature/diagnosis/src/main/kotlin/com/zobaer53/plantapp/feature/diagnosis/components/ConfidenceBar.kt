package com.zobaer53.plantapp.feature.diagnosis.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zobaer53.plantapp.feature.diagnosis.ui.LightDarkPreviews
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewTheme

/** Rounded bar that grows from 0 to [confidence] (0..1) the first time it is shown. */
@Composable
internal fun ConfidenceBar(
    confidence: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    thickness: Dp = 8.dp,
) {
    val target = confidence.coerceIn(0f, 1f)
    // Previews are static, so skip the grow-in animation there.
    var shown by remember { mutableStateOf(false) }
    val inPreview = LocalInspectionMode.current
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown || inPreview) target else 0f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "confidence",
    )
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .height(thickness),
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@LightDarkPreviews
@Composable
private fun ConfidenceBarPreview() {
    PreviewTheme {
        Column(Modifier.padding(16.dp)) {
            ConfidenceBar(confidence = 0.82f)
            ConfidenceBar(
                confidence = 0.35f,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
