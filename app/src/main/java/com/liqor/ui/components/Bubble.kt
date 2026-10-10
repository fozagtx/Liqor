package com.liqor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import com.liqor.R
import com.liqor.ui.BubbleState
import com.liqor.ui.BubbleUi
import com.liqor.ui.theme.Caution
import com.liqor.ui.theme.Danger
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary
import kotlinx.coroutines.launch

@Composable
fun Bubble(ui: BubbleUi, modifier: Modifier = Modifier) {
    val asset = ui.asset
    val side = ui.side
    val tagText = when {
        ui.state == BubbleState.Unknown || ui.state == BubbleState.Sample -> null
        asset == null || asset.isBlank() -> null
        side == "long" -> stringResource(R.string.bubble_tag_long, asset)
        side == "short" -> stringResource(R.string.bubble_tag_short, asset)
        else -> null
    }
    val bubbleDescription = when {
        ui.state == BubbleState.Unknown ->
            stringResource(R.string.bubble_unknown_content_description)
        ui.state == BubbleState.Sample && asset != null && side != null ->
            stringResource(
                R.string.bubble_sample_content_description,
                asset,
                side,
                ui.value,
            )
        ui.state == BubbleState.Sample -> stringResource(R.string.bubble_sample_description)
        ui.state == BubbleState.Stale && asset != null && side != null ->
            stringResource(
                R.string.bubble_stale_content_description,
                asset,
                side,
                ui.value,
            )
        ui.state == BubbleState.Stale ->
            stringResource(R.string.bubble_unknown_content_description)
        asset != null && side != null -> stringResource(
            R.string.bubble_content_description,
            asset,
            side,
            ui.value,
        )
        else -> stringResource(R.string.bubble_unknown_content_description)
    }
    val color = when (ui.state) {
        BubbleState.Safe, BubbleState.Sample -> Safe
        BubbleState.Caution -> Caution
        BubbleState.Danger -> Danger
        BubbleState.Stale, BubbleState.Unknown -> Stale
    }
    val disc = if (ui.state == BubbleState.Danger) Danger else Raised
    val numberColor = if (ui.state == BubbleState.Danger) Raised else TextPrimary
    val value = ui.value.take(4)
    val animatedColor by animateColorAsState(color, tween(300), label = "bubble-ring")
    val halo = remember { Animatable(0f) }
    LaunchedEffect(ui.state, ui.animateDanger) {
        halo.snapTo(0f)
        if (ui.state == BubbleState.Danger && ui.animateDanger) {
            repeat(3) {
                halo.animateTo(1f, tween(1_200))
                halo.animateTo(0f, tween(1_200))
            }
        }
    }
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = bubbleDescription
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val alpha = halo.value * 0.22f
                        this.alpha = alpha
                        scaleX = 1f + halo.value * 0.2f
                        scaleY = 1f + halo.value * 0.2f
                    }
                    .background(Danger, CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(6.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(disc),
                contentAlignment = Alignment.Center,
            ) {
                Gauge(
                    cushion = ui.cushion,
                    color = animatedColor,
                    diameter = 56.dp,
                    stroke = 2.dp,
                )
                Row(
                    modifier = Modifier
                        .graphicsLayer(scaleX = 0.85f)
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(
                        LocalDensity provides Density(density.density, 1f),
                    ) {
                        Text(
                            text = value,
                            color = numberColor,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = if (value.contains('.')) 0.em else (-0.06).em,
                            ),
                            maxLines = 1,
                        )
                        if (ui.state != BubbleState.Unknown) {
                            Text(
                                text = stringResource(R.string.percent_sign),
                                color = numberColor,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    letterSpacing = (-0.04).em,
                                ),
                            )
                        }
                    }
                }
            }
            if (ui.state == BubbleState.Stale) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Stale)
                        .border(2.dp, Raised, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.clock_glyph),
                        color = Raised,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    )
                }
            }
        }
        if (ui.state == BubbleState.Sample) {
            Text(
                text = stringResource(R.string.sample_tag),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(CircleShape)
                    .background(TextPrimary)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                color = Raised,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                textAlign = TextAlign.Center,
            )
        } else if (tagText != null) {
            val tagBackground = if (ui.state == BubbleState.Stale) Stale else TextPrimary
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, 1f),
            ) {
                Row(
                    modifier = Modifier
                        .offset(y = (-4).dp)
                        .widthIn(max = 76.dp)
                        .clip(CircleShape)
                        .background(tagBackground)
                        .padding(start = 3.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (asset != null) CoinLogo(asset, 12.dp)
                    Text(
                        text = tagText,
                        color = Raised,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.08.em,
                        ),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
