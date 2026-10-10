package com.liqor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.liqor.R

private val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans_regular, FontWeight.Normal),
    Font(R.font.instrument_sans_medium, FontWeight.Medium),
    Font(R.font.instrument_sans_semibold, FontWeight.SemiBold),
)

private val TypographyBase = Typography()

private fun TextStyle.instrumentSans() = copy(fontFamily = InstrumentSans)

private fun TextStyle.label() = copy(
    fontFamily = InstrumentSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    letterSpacing = 0.08.em,
)

val LiqorTypography = TypographyBase.copy(
    displayLarge = TypographyBase.displayLarge.instrumentSans(),
    displayMedium = TypographyBase.displayMedium.instrumentSans(),
    displaySmall = TypographyBase.displaySmall.instrumentSans(),
    headlineLarge = TypographyBase.headlineLarge.instrumentSans(),
    headlineMedium = TypographyBase.headlineMedium.instrumentSans(),
    headlineSmall = TypographyBase.headlineSmall.instrumentSans(),
    titleLarge = TypographyBase.titleLarge.instrumentSans(),
    titleMedium = TypographyBase.titleMedium.instrumentSans(),
    titleSmall = TypographyBase.titleSmall.instrumentSans(),
    bodyLarge = TypographyBase.bodyLarge.instrumentSans(),
    bodyMedium = TypographyBase.bodyMedium.instrumentSans(),
    bodySmall = TypographyBase.bodySmall.instrumentSans(),
    labelLarge = TypographyBase.labelLarge.label(),
    labelMedium = TypographyBase.labelMedium.label(),
    labelSmall = TypographyBase.labelSmall.label(),
)
