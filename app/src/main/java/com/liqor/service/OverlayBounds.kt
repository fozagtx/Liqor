package com.liqor.service

data class OverlayPosition(
    val x: Int,
    val y: Int,
)

data class OverlayBounds(
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int,
    val dragMinX: Int,
    val dragMaxX: Int,
    val dragMaxY: Int,
) {
    fun clampResting(x: Int, y: Int): OverlayPosition =
        OverlayPosition(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))

    fun clampDrag(x: Int, y: Int): OverlayPosition =
        OverlayPosition(x.coerceIn(dragMinX, dragMaxX), y.coerceIn(minY, dragMaxY))

    companion object {
        fun calculate(
            screenWidth: Int,
            screenHeight: Int,
            leftInset: Int,
            topInset: Int,
            rightInset: Int,
            bottomInset: Int,
            margin: Int,
            shadowPadding: Int,
            circleLeft: Int,
            diameter: Int,
            contentHeight: Int,
            contentLeft: Int = circleLeft,
            contentWidth: Int = diameter,
        ): OverlayBounds {
            val minX = leftInset + margin - minOf(circleLeft, contentLeft)
            val maxX = (screenWidth - rightInset - margin - maxOf(
                circleLeft + diameter,
                contentLeft + contentWidth,
            ))
                .coerceAtLeast(minX)
            val minY = topInset + margin - shadowPadding
            val maxY = (screenHeight - bottomInset - margin - contentHeight - shadowPadding)
                .coerceAtLeast(minY)
            val dragMinX = leftInset - circleLeft
            val dragMaxX = (screenWidth - rightInset - circleLeft - diameter)
                .coerceAtLeast(dragMinX)
            val dragMaxY = (screenHeight - diameter - shadowPadding).coerceAtLeast(minY)

            return OverlayBounds(
                minX = minX,
                maxX = maxX,
                minY = minY,
                maxY = maxY,
                dragMinX = dragMinX,
                dragMaxX = dragMaxX,
                dragMaxY = dragMaxY,
            )
        }
    }
}
