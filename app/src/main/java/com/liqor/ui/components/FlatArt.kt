package com.liqor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

@Composable
fun WaveBands(
    color: Color,
    bandCount: Int,
    startFraction: Float,
    thickness: Dp,
    gap: Dp,
    bulge: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val strokeWidth = thickness.toPx()
        val verticalStep = (thickness + gap).toPx()
        val raisedCenter = bulge.toPx() * 2f
        repeat(bandCount.coerceAtLeast(0)) { index ->
            val y = size.height * startFraction + verticalStep * index
            val path = Path().apply {
                moveTo(-size.width * 0.1f, y)
                quadraticTo(
                    size.width * 0.5f,
                    y - raisedCenter,
                    size.width * 1.1f,
                    y,
                )
            }
            drawPath(path, color, style = Stroke(strokeWidth, cap = StrokeCap.Butt))
        }
    }
}
