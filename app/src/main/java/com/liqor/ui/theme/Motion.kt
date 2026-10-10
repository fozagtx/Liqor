package com.liqor.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

object Motion {
    const val Stagger = 40
    const val Micro = 80
    const val Quick = 150
    const val Fast = 250
    const val Medium = 350
    const val Slow = 400
    const val VerySlow = 500

    val SmoothOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val PageSlide = 8.dp
    val TextReveal = 12.dp
    val PageBlur = 3.dp
    val RevealBlur = 3.dp
}
