package com.liqor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.liqor.R
import com.liqor.domain.Level
import com.liqor.domain.usd
import com.liqor.ui.theme.Caution
import com.liqor.ui.theme.Danger
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TypeRamp
import kotlin.math.roundToInt

@Composable
fun TrainLine(
    entry: Double,
    now: Double,
    liq: Double,
    travelled: Float,
    level: Level,
    stale: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val stateColor = if (stale) Stale else when (level) {
        Level.Safe -> Safe
        Level.Caution -> Caution
        Level.Danger -> Danger
    }
    val nowColor = if (stale) Stale else TextPrimary
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth().height(66.dp)) {
        val widthPx = with(density) { maxWidth.toPx() }
        val markerX = widthPx * travelled.coerceIn(0f, 1f)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .align(Alignment.TopStart)
                .offset(y = 24.dp),
        ) {
            val y = size.height / 2f
            val end = size.width - 2.dp.toPx()
            val marker = markerX.coerceIn(2.dp.toPx(), end)
            drawLine(Hairline, androidx.compose.ui.geometry.Offset(2.dp.toPx(), y),
                androidx.compose.ui.geometry.Offset(end, y), 4.dp.toPx(), StrokeCap.Round)
            drawLine(stateColor, androidx.compose.ui.geometry.Offset(marker, y),
                androidx.compose.ui.geometry.Offset(end, y), 4.dp.toPx(), StrokeCap.Round)
            drawLine(nowColor, androidx.compose.ui.geometry.Offset(2.dp.toPx(), y),
                androidx.compose.ui.geometry.Offset(marker, y), 4.dp.toPx(), StrokeCap.Round)
            drawLine(stateColor, androidx.compose.ui.geometry.Offset(end, y - 7.dp.toPx()),
                androidx.compose.ui.geometry.Offset(end, y + 7.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        }
        Box(
            Modifier
                .offset { IntOffset(markerX.toInt(), with(density) { 30.dp.roundToPx() }) }
                .clip(CircleShape)
                .background(nowColor)
                .padding(3.dp),
        )
        Text(
            text = stringResource(R.string.train_now_price, usd(now)),
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val x = (markerX - placeable.width / 2f).coerceIn(
                        0f,
                        (widthPx - placeable.width).coerceAtLeast(0f),
                    )
                    layout(placeable.width, placeable.height) {
                        placeable.place(x.roundToInt(), 0)
                    }
                }
                .clip(CircleShape)
                .background(TextPrimary)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            color = Raised,
            style = TypeRamp.label,
            maxLines = 1,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.train_entry_price, usd(entry)),
                color = if (stale) Stale else TextPrimary,
                style = TypeRamp.label,
            )
            Text(
                stringResource(R.string.train_liquidation_price, usd(liq)),
                color = if (stale) Stale else TextPrimary,
                style = TypeRamp.label,
            )
        }
    }
}
