package com.liqor.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayBoundsTest {
    @Test
    fun portraitUsesMaxOfStatusBarAndCutoutInsets() {
        val statusBarTop = 60
        val cutoutTop = 120
        val bounds = bounds(screenHeight = 2_000, topInset = maxOf(statusBarTop, cutoutTop))

        assertEquals(120 + MARGIN - SHADOW_PADDING, bounds.minY)
    }

    @Test
    fun fullscreenStillUsesCutoutInsetWhenStatusBarIsHidden() {
        val visibleStatusBarTop = 0
        val cutoutTop = 120
        val bounds = bounds(screenHeight = 2_000, topInset = maxOf(visibleStatusBarTop, cutoutTop))

        assertEquals(120 + MARGIN - SHADOW_PADDING, bounds.minY)
    }

    @Test
    fun landscapeLeftCutoutMovesLeftSnapTarget() {
        val bounds = bounds(screenWidth = 1_000, leftInset = 100, circleLeft = 8)

        assertEquals(100 + MARGIN - 8, bounds.minX)
    }

    @Test
    fun dragClampKeepsTopSafeAndCanReachTheBottom() {
        val bounds = bounds(screenHeight = 1_000, topInset = 24, bottomInset = 20)

        assertEquals(bounds.minY, bounds.clampDrag(bounds.minX, -100).y)
        val nearBottom = bounds.clampDrag(bounds.minX, 990).y
        assertEquals(bounds.dragMaxY, nearBottom)
        assertTrue(nearBottom > bounds.maxY)
    }

    @Test
    fun shortScreenCollapsesRestingRangeAtMinimum() {
        val bounds = bounds(screenHeight = 100, topInset = 120)

        assertEquals(bounds.minY, bounds.maxY)
    }

    private fun bounds(
        screenWidth: Int = 1_000,
        screenHeight: Int = 2_000,
        leftInset: Int = 0,
        topInset: Int = 0,
        rightInset: Int = 0,
        bottomInset: Int = 0,
        circleLeft: Int = 8,
    ) = OverlayBounds.calculate(
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        leftInset = leftInset,
        topInset = topInset,
        rightInset = rightInset,
        bottomInset = bottomInset,
        margin = MARGIN,
        shadowPadding = SHADOW_PADDING,
        circleLeft = circleLeft,
        diameter = DIAMETER,
        contentHeight = DIAMETER,
    )

    private companion object {
        const val MARGIN = 12
        const val SHADOW_PADDING = 8
        const val DIAMETER = 56
    }
}
