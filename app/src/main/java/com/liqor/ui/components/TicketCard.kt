package com.liqor.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.liqor.R
import com.liqor.domain.Watched
import com.liqor.domain.distanceText
import com.liqor.domain.sentence
import com.liqor.domain.usd
import com.liqor.ui.LocalAnimatedScope
import com.liqor.ui.LocalReducedMotion
import com.liqor.ui.theme.Caution
import com.liqor.ui.theme.Danger
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.CardPadding
import com.liqor.ui.theme.Elevation
import com.liqor.ui.theme.IllustrationSize
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.Radius
import com.liqor.ui.theme.Space
import com.liqor.ui.theme.TypeRamp
import kotlin.math.roundToInt

@Composable
fun TicketCard(
    watched: Watched,
    title: String,
    animateSample: Boolean = false,
    stale: Boolean = false,
    closest: Boolean = false,
    panelContent: Boolean = false,
    perforated: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val fullDistance = distanceText(watched.distance)
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val shouldAnimateSample = animateSample && !reducedMotion && animatedScope != null
    val sampleProgress = remember { Animatable(if (shouldAnimateSample) 0f else 1f) }
    LaunchedEffect(shouldAnimateSample, animateSample) {
        if (shouldAnimateSample) {
            sampleProgress.snapTo(0f)
            sampleProgress.animateTo(1f, tween(Motion.TicketCount, easing = Motion.SmoothOut))
        } else {
            sampleProgress.snapTo(1f)
        }
    }
    val targetDistance = fullDistance.toFloatOrNull()
    val distance = if (animateSample && targetDistance != null && sampleProgress.value < 1f) {
        (targetDistance * sampleProgress.value).roundToInt().toString()
    } else {
        fullDistance
    }
    val travelled = watched.travelled.toFloat() *
        (if (animateSample) sampleProgress.value else 1f)
    val ringColor = if (stale) Stale else when (watched.level) {
        com.liqor.domain.Level.Safe -> Safe
        com.liqor.domain.Level.Caution -> Caution
        com.liqor.domain.Level.Danger -> Danger
    }
    val cardShape = RoundedCornerShape(Radius.lg)
    Surface(
        modifier = if (panelContent) {
            modifier.fillMaxWidth()
        } else {
            modifier.fillMaxWidth()
                .shadow(Elevation.card, cardShape, clip = false)
                .cardBorder(cardShape)
        },
        color = if (panelContent) Color.Transparent else Raised,
        shape = if (panelContent) RoundedCornerShape(Space.none) else cardShape,
    ) {
        Column {
            Column(Modifier.padding(CardPadding.hero)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            color = if (stale) Stale else TextPrimary,
                            style = TypeRamp.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.size(Space.xs))
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
                                modifier = Modifier.padding(
                                    start = Space.ticketPercentStart,
                                    bottom = Space.ticketPercentBottom,
                                ),
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
                        cushion = 1f - travelled,
                        color = ringColor,
                        diameter = IllustrationSize.ticketGauge,
                        stroke = IllustrationSize.ticketGaugeStroke,
                        modifier = Modifier.padding(top = Space.ticketGaugeTop),
                    )
                }
                Row(
                    modifier = Modifier.padding(top = Space.ticketRowTop),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusChip(watched.level, label = if (stale) stringResource(R.string.stale_label) else null, stale = stale)
                    Text(
                        sentence(watched),
                        modifier = Modifier.padding(start = Space.sm),
                        color = if (stale) Stale else TextPrimary,
                        style = TypeRamp.row,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (perforated) {
                Box(Modifier.fillMaxWidth()) {
                    Canvas(
                        Modifier
                            .fillMaxWidth()
                            .height(IllustrationSize.hairline)
                            .padding(horizontal = Space.ticketDashInset),
                    ) {
                        drawLine(
                            Hairline,
                            androidx.compose.ui.geometry.Offset.Zero,
                            androidx.compose.ui.geometry.Offset(size.width, 0f),
                            IllustrationSize.hairline.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(Space.ticketDashSize.toPx(), Space.ticketDashSize.toPx()),
                            ),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Box(Modifier.size(IllustrationSize.ticketPerforationSize).clip(CircleShape).background(Ground))
                        Box(Modifier.size(IllustrationSize.ticketPerforationSize).clip(CircleShape).background(Ground))
                    }
                }
            } else {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(IllustrationSize.hairline)
                        .background(Hairline),
                )
            }
            TrainLine(
                entry = watched.position.entryPrice,
                now = watched.position.oraclePrice,
                liq = watched.position.liquidationPrice,
                travelled = travelled,
                level = watched.level,
                stale = stale,
                modifier = Modifier.padding(horizontal = CardPadding.hero, vertical = Space.md),
            )
        }
    }
}
