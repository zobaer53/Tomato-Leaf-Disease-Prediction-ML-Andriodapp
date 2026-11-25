package com.zobaer53.plantapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.zobaer53.plantapp.R

/** Carbon Block, the v1 display face. It ships one weight, so styles keep the default Normal weight. */
private val Carbon = FontFamily(Font(R.font.carbon_bl, FontWeight.Normal))

private val Default = Typography()

/** Carbon for display and headline styles; the platform sans for titles, body and labels. */
internal val PlantAppTypography = Typography(
    displayLarge = Default.displayLarge.copy(fontFamily = Carbon),
    displayMedium = Default.displayMedium.copy(fontFamily = Carbon),
    displaySmall = Default.displaySmall.copy(fontFamily = Carbon),
    headlineLarge = Default.headlineLarge.copy(fontFamily = Carbon),
    headlineMedium = Default.headlineMedium.copy(fontFamily = Carbon),
    headlineSmall = Default.headlineSmall.copy(fontFamily = Carbon),
)
