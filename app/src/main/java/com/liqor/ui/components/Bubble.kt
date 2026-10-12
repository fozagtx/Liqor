package com.liqor.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.liqor.ui.LocalAnimatedScope
import com.liqor.ui.LocalReducedMotion
import com.liqor.ui.theme.Caution
import com.liqor.ui.theme.Danger
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TypeRamp
import com.liqor.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class BubbleTag(
    val asset: String?,
    val text: String,
    val background: Color,
)

@Composable
fun Bubble(
    ui: BubbleUi,
    modifier: Modifier = Modifier,
) {
    val canRotate = ui.rotation.size > 1 &&
        !LocalReducedMotion.current &&
        LocalAnimatedScope.current != null
    var rotationIndex by remember(ui.rotation) { mutableIntStateOf(0) }
    LaunchedEffect(canRotate, ui.rotation) {
        rotationIndex = 0
        if (canRotate) {
            while (true) {
                delay(Motion.BubbleRotationInterval.toLong())
                rotationIndex = (rotationIndex + 1) % ui.rotation.size
            }
        }
    }
    val displayUi = when {
        ui.rotation.isEmpty() -> ui
        canRotate -> ui.rotation[rotationIndex.coerceIn(0, ui.rotation.lastIndex)]
        else -> ui.rotation.first()
    }
    val asset = displayUi.asset
    val side = displayUi.side
    val tagText = when {
        displayUi.state == BubbleState.Idle -> stringResource(R.string.bubble_no_positions)
        displayUi.state == BubbleState.Unknown || displayUi.state == BubbleState.Sample -> null
        asset == null || asset.isBlank() -> null
        side == "long" -> stringResource(R.string.bubble_tag_long, asset)
        side == "short" -> stringResource(R.string.bubble_tag_short, asset)
        else -> null
    }
    val bubbleDescription = when {
        displayUi.state == BubbleState.Idle ->
            stringResource(R.string.bubble_no_positions_content_description)
        displayUi.state == BubbleState.Unknown ->
            stringResource(R.string.bubble_unknown_content_description)
        displayUi.state == BubbleState.Sample && asset != null && side != null ->
            stringResource(
                R.string.bubble_sample_content_description,
                asset,
                side,
                displayUi.value,
            )
        displayUi.state == BubbleState.Sample -> stringResource(R.string.bubble_sample_description)
        displayUi.state == BubbleState.Stale && asset != null && side != null ->
            stringResource(
                R.string.bubble_stale_content_description,
                asset,
                side,
                displayUi.value,
            )
        displayUi.state == BubbleState.Stale ->
            stringResource(R.string.bubble_unknown_content_description)
        asset != null && side != null -> stringResource(
            R.string.bubble_content_description,
            asset,
            side,
            displayUi.value,
        )
        else -> stringResource(R.string.bubble_unknown_content_description)
    }
    val color = when (displayUi.state) {
        BubbleState.Safe, BubbleState.Sample -> Safe
        BubbleState.Caution -> Caution
        BubbleState.Danger -> Danger
        BubbleState.Stale, BubbleState.Idle, BubbleState.Unknown -> Stale
    }
    val disc = if (displayUi.state == BubbleState.Danger) Danger else Raised
    val numberColor = if (displayUi.state == BubbleState.Danger) Raised else TextPrimary
    val value = displayUi.value.take(4)
    val density = LocalDensity.current
    val slidePixels = with(density) { Motion.BubblePositionSlide.roundToPx() }
    val animatedColor by animateColorAsState(
        color,
        tween(Motion.BubblePositionChange),
        label = "bubble-ring",
    )
    val tag = when {
        displayUi.state == BubbleState.Idle && tagText != null -> BubbleTag(
            asset = null,
            text = tagText,
            background = TextPrimary,
        )
        asset != null && tagText != null -> BubbleTag(
            asset = asset,
            text = tagText,
            background = if (displayUi.state == BubbleState.Stale) Stale else TextPrimary,
        )
        else -> null
    }
    val halo = remember { Animatable(0f) }
    val shouldAnimate = !LocalReducedMotion.current && LocalAnimatedScope.current != null
    LaunchedEffect(displayUi.state, displayUi.animateDanger, shouldAnimate) {
        halo.snapTo(0f)
        if (shouldAnimate && displayUi.state == BubbleState.Danger && displayUi.animateDanger) {
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
            if (displayUi.state == BubbleState.Danger) {
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
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(6.dp, CircleShape, clip = false)
                    .clip(CircleShape)
                    .background(disc),
                contentAlignment = Alignment.Center,
            ) {
                Gauge(
                    cushion = if (displayUi.state == BubbleState.Idle) 1f else displayUi.cushion,
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
                        AnimatedContent(
                            targetState = value,
                            transitionSpec = {
                                (
                                    fadeIn(tween(Motion.BubblePositionChange, easing = Motion.SmoothOut)) +
                                        slideInVertically(
                                            tween(Motion.BubblePositionChange, easing = Motion.SmoothOut),
                                        ) { slidePixels }
                                    ) togetherWith (
                                    fadeOut(tween(Motion.BubblePositionChange, easing = Motion.SmoothOut)) +
                                        slideOutVertically(
                                            tween(Motion.BubblePositionChange, easing = Motion.SmoothOut),
                                        ) { -slidePixels }
                                    )
                            },
                            contentAlignment = Alignment.Center,
                            label = "bubble-value",
                        ) { currentValue ->
                            Text(
                                text = currentValue,
                                color = numberColor,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = if (currentValue.contains('.')) 0.em else (-0.06).em,
                                ),
                                maxLines = 1,
                            )
                        }
                        if (displayUi.state != BubbleState.Unknown && displayUi.state != BubbleState.Idle) {
                            Text(
                                text = stringResource(R.string.percent_sign),
                                color = numberColor,
                                style = TypeRamp.label.copy(letterSpacing = (-0.04).em),
                            )
                        }
                    }
                }
            }
            if (displayUi.state == BubbleState.Stale) {
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
                        style = TypeRamp.caption,
                    )
                }
            }
        }
        AnimatedContent(
            targetState = tag,
            transitionSpec = {
                (
                    fadeIn(tween(Motion.BubblePositionChange, easing = Motion.SmoothOut)) +
                        slideInVertically(
                            tween(Motion.BubblePositionChange, easing = Motion.SmoothOut),
                        ) { slidePixels }
                    ) togetherWith (
                    fadeOut(tween(Motion.BubblePositionChange, easing = Motion.SmoothOut)) +
                        slideOutVertically(
                            tween(Motion.BubblePositionChange, easing = Motion.SmoothOut),
                        ) { -slidePixels }
                    )
            },
            contentAlignment = Alignment.Center,
            label = "bubble-token",
        ) { currentTag ->
            if (currentTag != null) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, 1f),
                ) {
                    Row(
                        modifier = Modifier
                            .offset(y = (-4).dp)
                            .widthIn(max = if (currentTag.asset == null) 88.dp else 76.dp)
                            .clip(CircleShape)
                            .background(currentTag.background)
                            .padding(start = 3.dp, end = 6.dp, top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        currentTag.asset?.let { CoinLogo(it, 12.dp) }
                        Text(
                            text = currentTag.text,
                            color = Raised,
                            style = TypeRamp.label,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = currentTag.asset != null,
                            overflow = if (currentTag.asset == null) {
                                TextOverflow.Clip
                            } else {
                                TextOverflow.Ellipsis
                            },
                        )
                    }
                }
            }
        }
    }
}
