package com.liqor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.liqor.R
import com.liqor.domain.Watched
import com.liqor.domain.distanceText
import com.liqor.domain.sentence
import com.liqor.domain.usd
import com.liqor.ui.theme.Caution
import com.liqor.ui.theme.Danger
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary

@Composable
fun TicketCard(
    watched: Watched,
    title: String,
    isSample: Boolean = false,
    stale: Boolean = false,
    closest: Boolean = false,
    panelContent: Boolean = false,
    perforated: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val distance = distanceText(watched.distance)
    val ringColor = if (stale) Stale else when (watched.level) {
        com.liqor.domain.Level.Safe -> Safe
        com.liqor.domain.Level.Caution -> Caution
        com.liqor.domain.Level.Danger -> Danger
    }
    Surface(
        modifier = if (panelContent) {
            modifier.fillMaxWidth()
        } else {
            modifier.fillMaxWidth().shadow(7.dp, RoundedCornerShape(24.dp), clip = false)
        },
        color = if (panelContent) Color.Transparent else Raised,
        shape = if (panelContent) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp),
    ) {
        Column {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            color = if (stale) Stale else TextPrimary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                letterSpacing = 0.09.em,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.size(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                distance,
                                color = if (stale) Stale else TextPrimary,
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = if (closest) 42.sp else 44.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = if (distance.contains('.')) 0.em else (-0.07).em,
                                    lineHeight = 46.sp,
                                ),
                                maxLines = 1,
                            )
                            Text(
                                stringResource(R.string.percent_sign),
                                modifier = Modifier.padding(start = 2.dp, bottom = 7.dp),
                                color = if (stale) Stale else TextPrimary,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = (-0.04).em,
                                ),
                            )
                        }
                    }
                    Gauge(
                        cushion = 1f - watched.travelled.toFloat(),
                        color = ringColor,
                        diameter = 42.dp,
                        stroke = 3.dp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(watched.level, label = if (stale) stringResource(R.string.stale_label) else null, stale = stale)
                    Text(
                        sentence(watched),
                        modifier = Modifier.padding(start = 8.dp),
                        color = if (stale) Stale else TextPrimary,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isSample) {
                        Spacer(Modifier.weight(1f))
                        SampleChip()
                    }
                }
            }
            if (perforated) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                    Canvas(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 14.dp)) {
                        drawLine(
                            Hairline,
                            androidx.compose.ui.geometry.Offset.Zero,
                            androidx.compose.ui.geometry.Offset(size.width, 0f),
                            1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Ground))
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Ground))
                    }
                }
            } else {
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            }
            TrainLine(
                entry = watched.position.entryPrice,
                now = watched.position.oraclePrice,
                liq = watched.position.liquidationPrice,
                travelled = watched.travelled.toFloat(),
                level = watched.level,
                stale = stale,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}
