package com.liqor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liqor.ui.theme.Hairline

@Composable
fun Gauge(
    cushion: Float,
    color: Color,
    diameter: Dp,
    stroke: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(diameter)) {
        val width = stroke.toPx()
        val inset = width / 2f
        val arcSize = size.minDimension - width
        val topLeft = Offset(inset, inset)
        val arcOffset = Offset(inset, inset)
        drawArc(
            color = Hairline,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
        val sweep = 360f * cushion.coerceIn(0f, 1f)
        if (sweep > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcOffset,
                size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                style = Stroke(width = width, cap = StrokeCap.Round),
            )
        }
    }
}
