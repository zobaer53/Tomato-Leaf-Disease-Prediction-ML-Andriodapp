package com.zobaer53.plantapp.feature.diagnosis.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zobaer53.plantapp.core.domain.model.ImageRef
import com.zobaer53.plantapp.feature.diagnosis.R
import com.zobaer53.plantapp.feature.diagnosis.ui.LightDarkPreviews
import com.zobaer53.plantapp.feature.diagnosis.ui.PreviewTheme

/**
 * Circular leaf photo (the legacy CircleImageView). Shows the `addimage` artwork until a photo is
 * picked, a scrim with a progress ring while analyzing, and a fallback when the photo can't load.
 * The whole circle is a 48dp+ touch target that opens the picker.
 */
@Composable
internal fun LeafImageCard(
    image: ImageRef?,
    isAnalyzing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 200.dp,
) {
    val description = stringResource(
        if (image == null) R.string.image_none_description else R.string.image_selected_description,
    )
    var loadFailed by remember(image) { mutableStateOf(false) }
    val borderColor = if (image == null) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.primary
    }

    Box(modifier = modifier.size(diameter)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(width = 2.dp, color = borderColor, shape = CircleShape)
                .clickable(
                    enabled = !isAnalyzing,
                    onClickLabel = stringResource(R.string.image_click_label),
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics { contentDescription = description },
        ) {
            if (image == null) {
                Image(
                    painter = painterResource(R.drawable.addimage),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(diameter * 0.24f),
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(image.value)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onState = { state -> loadFailed = state is AsyncImagePainter.State.Error },
                    modifier = Modifier.fillMaxSize(),
                )
                if (loadFailed) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_broken_image),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.image_load_failed),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isAnalyzing, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(diameter * 0.3f))
                }
            }
        }

        // Decorative "add photo" badge; the circle itself carries the click and semantics.
        AnimatedVisibility(
            visible = !isAnalyzing,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .padding(diameter * 0.04f)
                    .size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(R.drawable.ic_add_photo), contentDescription = null)
                }
            }
        }
    }
}

@LightDarkPreviews
@Composable
private fun LeafImageCardEmptyPreview() {
    PreviewTheme {
        LeafImageCard(image = null, isAnalyzing = false, onClick = {}, modifier = Modifier.padding(16.dp))
    }
}

@LightDarkPreviews
@Composable
private fun LeafImageCardAnalyzingPreview() {
    PreviewTheme {
        LeafImageCard(
            image = ImageRef("content://preview/leaf.jpg"),
            isAnalyzing = true,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
