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
    lineHeight = 14.sp,
    letterSpacing = 0.08.em,
)

object TypeRamp {
    val title = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.03).em,
    )
    val body = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
    val bodyStrong = body.copy(fontWeight = FontWeight.Medium)
    val row = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val caption = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val label = TypographyBase.labelSmall.label()
    val button = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.02.em,
    )
    val action = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val brand = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    )
    val micro = TextStyle(
        fontFamily = InstrumentSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
    )
}

val LiqorTypography = TypographyBase.copy(
    displayLarge = TypographyBase.displayLarge.instrumentSans(),
    displayMedium = TypographyBase.displayMedium.instrumentSans(),
    displaySmall = TypographyBase.displaySmall.instrumentSans(),
    headlineLarge = TypographyBase.headlineLarge.instrumentSans(),
    headlineMedium = TypographyBase.headlineMedium.instrumentSans(),
    headlineSmall = TypeRamp.title,
    titleLarge = TypographyBase.titleLarge.instrumentSans(),
    titleMedium = TypographyBase.titleMedium.instrumentSans(),
    titleSmall = TypographyBase.titleSmall.instrumentSans(),
    bodyLarge = TypographyBase.bodyLarge.instrumentSans(),
    bodyMedium = TypeRamp.body,
    bodySmall = TypographyBase.bodySmall.instrumentSans(),
    labelLarge = TypographyBase.labelLarge.label(),
    labelMedium = TypographyBase.labelMedium.label(),
    labelSmall = TypeRamp.label,
)
