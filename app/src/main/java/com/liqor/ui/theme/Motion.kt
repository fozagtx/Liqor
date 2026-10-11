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
    const val IconStagger = 60
    const val IconDriftPeriod = 2_400
    const val VenueHighlightPeriod = 1_200
    const val NotificationDrop = 320
    const val NotificationSwing = 600
    const val NotificationSwingRepeat = 4_000
    const val BubbleCheckDraw = 500
    const val TicketCount = 700
    const val RetrySpin = 400
    const val WalletClock = 6_000
    const val BubbleRotationInterval = 3_000
    const val BubblePositionChange = 300

    val SmoothOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val IconDriftAmplitude = 2.dp
    val BubblePositionSlide = 6.dp
    val PageSlide = 8.dp
    val TextReveal = 12.dp
    val PageBlur = 3.dp
    val RevealBlur = 3.dp
}
