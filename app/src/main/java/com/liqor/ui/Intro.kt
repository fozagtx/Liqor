package com.liqor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.liqor.R
import com.liqor.domain.Level
import com.liqor.domain.Watched
import com.liqor.domain.distanceText
import com.liqor.ui.components.BrandMark
import com.liqor.ui.components.Bubble
import com.liqor.ui.components.CoinLogo
import com.liqor.ui.components.PillButton
import com.liqor.ui.components.SampleChip
import com.liqor.ui.components.StatusChip
import com.liqor.ui.components.TextAction
import com.liqor.ui.theme.Danger
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.StaleText
import com.liqor.ui.theme.StaleTint
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IntroScreen(
    sample: Watched,
    onFinish: () -> Unit,
    initialPage: Int = 0,
    demoTimeMs: Long? = null,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            val pageOffset =
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .coerceIn(-1f, 1f)
            IntroPage(
                page = page,
                sample = sample,
                settled = pagerState.settledPage == page,
                pageOffset = pageOffset,
                demoTimeMs = demoTimeMs,
                onSkip = onFinish,
                onContinue = {
                    if (page == 2) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(page + 1) }
                    }
                },
            )
        }
    }
}

@Composable
private fun IntroPage(
    page: Int,
    sample: Watched,
    settled: Boolean,
    pageOffset: Float,
    demoTimeMs: Long?,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val illustrationModifier = Modifier
        .fillMaxWidth()
        .graphicsLayer {
            translationX = if (reducedMotion) 0f else pageOffset * size.width * 0.3f
        }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark()
                Text(
                    stringResource(R.string.app_name),
                    Modifier.padding(start = 7.dp),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.weight(1f))
                if (page < 2) {
                    TextAction(stringResource(R.string.intro_skip), onSkip)
                } else {
                    Spacer(Modifier.width(63.dp))
                }
            }

            when (page) {
                0 -> BeforeAfterIllustration(sample, settled, illustrationModifier)
                1 -> CoverageIllustration(settled, illustrationModifier)
                else -> BubbleDemo(sample, demoTimeMs, illustrationModifier)
            }

            Text(
                stringResource(
                    when (page) {
                        0 -> R.string.intro_before_title
                        1 -> R.string.intro_watches_title
                        else -> R.string.intro_demo_title
                    },
                ),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.04).em,
                ),
            )
            Text(
                stringResource(
                    when (page) {
                        0 -> R.string.intro_before_body
                        1 -> R.string.intro_watches_body
                        else -> R.string.intro_demo_body
                    },
                ),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )

            if (page == 1) {
                Row(
                    Modifier
                        .align(Alignment.Start)
                        .clip(CircleShape)
                        .background(Raised)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(Safe),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.check_glyph),
                            color = Raised,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        )
                    }
                    Text(
                        stringResource(R.string.feature_read_only),
                        Modifier.padding(start = 8.dp),
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    )
                }
                Text(
                    stringResource(R.string.intro_wallet_adapter),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        PagerDots(page)
        Spacer(Modifier.height(14.dp))
        PillButton(
            stringResource(if (page == 2) R.string.intro_get_started else R.string.intro_continue),
            onContinue,
        )
    }
}

@Composable
private fun PagerDots(page: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val width by animateDpAsState(
                targetValue = if (index == page) 18.dp else 6.dp,
                animationSpec = tween(Motion.Fast, easing = Motion.SmoothOut),
                label = "intro-dot-width",
            )
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .width(width)
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (index == page) TextPrimary else Hairline),
            )
        }
    }
}

@Composable
private fun BeforeAfterIllustration(
    sample: Watched,
    settled: Boolean,
    modifier: Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val haptic = LocalHapticFeedback.current
    val shouldAnimate = !reducedMotion && animatedScope != null
    val wipe = remember { Animatable(if (shouldAnimate) 0f else 1f) }
    val bubbleReveal = remember { Animatable(if (shouldAnimate) 0f else 1f) }

    LaunchedEffect(settled, shouldAnimate) {
        if (!shouldAnimate) {
            wipe.snapTo(1f)
            bubbleReveal.snapTo(1f)
        } else if (settled) {
            wipe.snapTo(0f)
            bubbleReveal.snapTo(0f)
            wipe.animateTo(1f, tween(Motion.Slow, easing = Motion.SmoothOut))
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            bubbleReveal.animateTo(1f, spring(dampingRatio = 0.7f))
        } else {
            wipe.snapTo(0f)
            bubbleReveal.snapTo(0f)
        }
    }

    Surface(
        modifier = modifier,
        color = Raised,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 5.dp,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 50.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                MiniPhone(
                    label = stringResource(R.string.intro_without_liqor),
                    sample = sample,
                    isWithLiqor = false,
                    bubbleScale = 1f,
                    bubbleAlpha = 1f,
                    modifier = Modifier.weight(1f),
                )
                MiniPhone(
                    label = stringResource(R.string.intro_with_liqor),
                    sample = sample,
                    isWithLiqor = true,
                    bubbleScale = 0.9f + 0.1f * bubbleReveal.value,
                    bubbleAlpha = bubbleReveal.value,
                    modifier = Modifier
                        .weight(1f)
                        .drawWithContent {
                            clipRect(right = size.width * wipe.value) {
                                this@drawWithContent.drawContent()
                            }
                        },
                )
            }
            SampleChip(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 10.dp))
        }
    }
}

@Composable
private fun MiniPhone(
    label: String,
    sample: Watched,
    isWithLiqor: Boolean,
    bubbleScale: Float,
    bubbleAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            maxLines = 1,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .aspectRatio(9f / 17f)
                .clip(RoundedCornerShape(16.dp))
                .background(Ground)
                .border(1.dp, Hairline, RoundedCornerShape(16.dp)),
        ) {
            Column(
                Modifier.fillMaxSize().padding(9.dp),
            ) {
                MiniAppSkeleton(
                    Modifier.weight(1f).graphicsLayer(alpha = if (isWithLiqor) 1f else 0.6f),
                )
                if (isWithLiqor) {
                    CaptionPill(
                        stringResource(R.string.intro_on_screen_caption),
                        Modifier.padding(top = 7.dp),
                    )
                } else {
                    CaptionPill(
                        stringResource(R.string.intro_last_checked_caption),
                        Modifier.padding(top = 7.dp),
                        stale = true,
                    )
                    LiquidatedEventCard(Modifier.padding(top = 6.dp))
                }
            }
            if (isWithLiqor) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Bubble(
                        sampleBubbleUi(sample),
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = maxHeight * 0.36f, end = 1.dp)
                            .graphicsLayer {
                                scaleX = bubbleScale * 0.8f
                                scaleY = bubbleScale * 0.8f
                                alpha = bubbleAlpha
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniAppSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(17.dp).clip(CircleShape).background(Hairline))
            Column(Modifier.weight(1f).padding(start = 7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SkeletonBar(Modifier.fillMaxWidth(0.72f).height(6.dp))
                SkeletonBar(Modifier.fillMaxWidth(0.45f).height(4.dp))
            }
            Box(Modifier.size(5.dp).clip(CircleShape).background(Hairline))
        }
        repeat(5) { index ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(Hairline))
                Column(
                    Modifier.weight(1f).padding(start = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SkeletonBar(Modifier.fillMaxWidth(if (index % 2 == 0) 0.9f else 0.72f).height(5.dp))
                    SkeletonBar(Modifier.fillMaxWidth(if (index % 2 == 0) 0.58f else 0.43f).height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun SkeletonBar(modifier: Modifier = Modifier) {
    Box(modifier.clip(CircleShape).background(StaleTint))
}

@Composable
private fun CaptionPill(
    text: String,
    modifier: Modifier = Modifier,
    stale: Boolean = false,
) {
    Text(
        text,
        modifier
            .clip(CircleShape)
            .background(if (stale) StaleTint else Raised)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        color = if (stale) StaleText else TextSecondary,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
        maxLines = 1,
    )
}

@Composable
private fun LiquidatedEventCard(modifier: Modifier = Modifier) {
    val text = stringResource(R.string.intro_without_event)
    val separator = text.indexOf('·')
    val event = buildAnnotatedString {
        append(text)
        if (separator >= 0) {
            addStyle(
                SpanStyle(color = TextSecondary),
                separator,
                text.length,
            )
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Raised,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(3.dp).height(19.dp).clip(CircleShape).background(Danger))
            Text(
                event,
                Modifier.padding(start = 6.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, lineHeight = 9.sp),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun CoverageIllustration(
    settled: Boolean,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        color = Raised,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 5.dp,
    ) {
        Box {
            Column(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Text(
                    stringResource(R.string.intro_venues_label),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    VenueTile("SOL", R.string.intro_venue_solana, Modifier.weight(1f))
                    VenueTile("JUP", R.string.intro_venue_perps, Modifier.weight(1f))
                    VenueTile("JUP", R.string.intro_venue_beta, Modifier.weight(1f))
                }
                Text(
                    stringResource(R.string.intro_crypto_label),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FanCoinStack(listOf("SOL", "ETH", "BTC", "JUP", "HYPE", "ZEC"), settled)
                    Text(
                        stringResource(R.string.intro_crypto_assets),
                        Modifier.weight(1f).padding(start = 8.dp),
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    )
                }
                Text(
                    stringResource(R.string.intro_stocks_label),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FanCoinStack(listOf("SPCX", "SNDK", "SKHYNIX"), settled)
                    Text(
                        stringResource(R.string.intro_stock_names),
                        Modifier.weight(1f).padding(start = 8.dp),
                        color = TextPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VenueTile(asset: String, nameResource: Int, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Ground)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        CoinLogo(asset, 22.dp)
        Text(
            stringResource(nameResource),
            color = TextPrimary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun FanCoinStack(assets: List<String>, settled: Boolean) {
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val shouldAnimate = !reducedMotion && animatedScope != null && settled
    val stockStack = assets.all { it in stockAssets }
    val iconSize = 28.dp
    val diameter = if (stockStack) 34.dp else 32.dp
    val step = if (stockStack) 27.dp else 20.dp
    Box(
        Modifier.width(diameter + step * (assets.size - 1)).height(diameter),
    ) {
        assets.forEachIndexed { index, asset ->
            val progress = remember(asset) { Animatable(if (shouldAnimate) 0f else 1f) }
            LaunchedEffect(settled, shouldAnimate, asset) {
                if (reducedMotion || animatedScope == null) {
                    progress.snapTo(1f)
                } else if (settled) {
                    progress.snapTo(0f)
                    delay((index * Motion.Stagger).toLong())
                    progress.animateTo(1f, tween(Motion.Medium, easing = Motion.SmoothOut))
                } else {
                    progress.snapTo(0f)
                }
            }
            if (stockStack) {
                CoinLogo(
                    asset,
                    diameter,
                    modifier = Modifier
                        .offset(x = step * index * progress.value)
                        .zIndex(index.toFloat())
                        .graphicsLayer(alpha = progress.value),
                    imageInset = 5.dp,
                    containerColor = Raised,
                    borderColor = Hairline,
                )
            } else {
                Box(
                    Modifier
                        .offset(x = step * index * progress.value)
                        .size(diameter)
                        .zIndex(index.toFloat())
                        .graphicsLayer(alpha = progress.value)
                        .clip(CircleShape)
                        .background(Raised)
                        .border(2.dp, Raised, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CoinLogo(asset, iconSize)
                }
            }
        }
    }
}

private val stockAssets = setOf("SPCX", "SNDK", "SKHYNIX")

@Composable
fun BubbleDemo(sample: Watched, timeMs: Long?, modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val animatedTime = if (timeMs == null && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "bubble-demo")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 7_000f,
            animationSpec = infiniteRepeatable(tween(7_000, easing = LinearEasing)),
            label = "bubble-demo-time",
        ).value
    } else {
        0f
    }
    val time = if (reducedMotion) {
        2_100f
    } else {
        timeMs?.let { ((it % 7_000L + 7_000L) % 7_000L).toFloat() } ?: animatedTime
    }
    val captionIndex = when {
        time < 1_200f -> 0
        time < 2_800f -> 1
        time < 4_600f -> 2
        time < 6_400f -> 3
        else -> 0
    }
    val captions = listOf(
        R.string.intro_demo_caption_overlay,
        R.string.intro_demo_caption_tap,
        R.string.intro_demo_caption_drag,
        R.string.intro_demo_caption_hide,
    )

    if (reducedMotion) {
        ReducedMotionDemo(sample, modifier)
        return
    }

    Surface(
        modifier = modifier,
        color = Raised,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 5.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth()) {
                SampleChip(Modifier.align(Alignment.TopEnd))
                AnimatedDemoPhone(
                    sample,
                    time,
                    Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
                )
            }
        }
    }
    AnimatedContent(
        targetState = captionIndex,
        transitionSpec = {
            fadeIn(tween(Motion.Fast)) togetherWith fadeOut(tween(Motion.Fast))
        },
        label = "bubble-demo-caption",
    ) { index ->
        Text(
            stringResource(captions[index]),
            Modifier.fillMaxWidth().padding(top = 10.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AnimatedDemoPhone(sample: Watched, time: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .width(220.dp)
            .height(300.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Ground)
            .border(1.dp, Hairline, RoundedCornerShape(18.dp)),
    ) {
        val width = maxWidth.value
        val height = maxHeight.value
        val enterProgress = smooth((time / 1_200f).coerceIn(0f, 1f))
        val resetProgress = if (time >= 6_400f) smooth(((time - 6_400f) / 600f).coerceIn(0f, 1f)) else 0f
        val resetAlpha = if (time >= 6_400f) 1f - ((time - 6_400f) / 600f).coerceIn(0f, 1f) else 1f

        var bubbleX = lerp(width + 8f, width - 63f, enterProgress)
        var bubbleY = 104f
        var bubbleScale = 1f
        var bubbleAlpha = enterProgress * resetAlpha
        if (time in 2_800f..4_600f) {
            val p = easeOutBack(((time - 2_800f) / 1_800f).coerceIn(0f, 1f))
            bubbleX = lerp(width - 63f, 4f, p).coerceIn(0f, width - 56f)
        } else if (time in 4_600f..6_400f) {
            val p = smooth(((time - 4_600f) / 1_800f).coerceIn(0f, 1f))
            bubbleX = lerp(4f, (width - 56f) / 2f, p)
            bubbleY = lerp(104f, height - 78f, p)
            val shrink = smooth(((time - 5_700f) / 500f).coerceIn(0f, 1f))
            bubbleScale = lerp(1f, 0.28f, shrink)
            bubbleAlpha = (1f - shrink) * resetAlpha
        } else if (time >= 6_400f) {
            bubbleX = lerp((width - 56f) / 2f, width + 8f, resetProgress)
            bubbleY = lerp(height - 78f, 104f, resetProgress)
        }

        MiniAppSkeleton(
            Modifier.fillMaxSize().padding(12.dp).graphicsLayer(alpha = 0.65f),
        )

        val panelProgress = panelProgress(time)
        if (panelProgress > 0f) {
            DemoPanel(
                sample,
                Modifier
                    .offset(x = (width - 175f).dp, y = 190.dp)
                    .graphicsLayer {
                        alpha = panelProgress
                        scaleX = 0.6f + 0.4f * panelProgress
                        scaleY = 0.6f + 0.4f * panelProgress
                        transformOrigin = TransformOrigin(1f, 0f)
                    },
            )
        }

        val hideAlpha = when {
            time < 4_800f -> 0f
            time < 5_100f -> smooth((time - 4_800f) / 300f)
            time < 6_200f -> 1f
            time < 6_400f -> 1f - smooth((time - 6_200f) / 200f)
            else -> 0f
        }
        if (hideAlpha > 0f) {
            HideTarget(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 25.dp)
                    .graphicsLayer(alpha = hideAlpha),
            )
        }

        Bubble(
            sampleBubbleUi(sample),
            Modifier
                .offset(x = bubbleX.dp, y = bubbleY.dp)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                    alpha = bubbleAlpha
                },
        )

        val finger = fingerPosition(time, width, height, bubbleX, bubbleY)
        if (finger.alpha > 0f) {
            Finger(
                Modifier
                    .offset(x = finger.x.dp, y = finger.y.dp)
                    .graphicsLayer {
                        scaleX = finger.scale
                        scaleY = finger.scale
                        alpha = finger.alpha
                    },
            )
        }
    }
}

@Composable
private fun DemoPanel(sample: Watched, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.width(170.dp),
        color = Raised,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 5.dp,
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinLogo(sample.position.asset, 20.dp)
                Text(
                    stringResource(
                        R.string.intro_demo_position_name,
                        sample.position.asset,
                        sample.position.side,
                    ),
                    Modifier.weight(1f).padding(start = 6.dp),
                    color = TextPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    maxLines = 1,
                )
                StatusChip(
                    sample.level,
                    label = "${distanceText(sample.distance)}%",
                )
            }
            Canvas(Modifier.fillMaxWidth().height(18.dp).padding(top = 5.dp)) {
                val start = size.width * 0.1f
                val end = size.width * 0.9f
                val y = size.height * 0.6f
                val now = start + (end - start) * sample.travelled.toFloat().coerceIn(0f, 1f)
                drawLine(Hairline, Offset(start, y), Offset(end, y), 3.dp.toPx())
                drawLine(levelColor(sample.level), Offset(start, y), Offset(now, y), 3.dp.toPx())
                drawCircle(TextSecondary, 2.dp.toPx(), Offset(start, y))
                drawCircle(levelColor(sample.level), 2.5.dp.toPx(), Offset(now, y))
                drawCircle(Danger, 2.dp.toPx(), Offset(end, y))
            }
        }
    }
}

@Composable
private fun ReducedMotionDemo(sample: Watched, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Raised,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 5.dp,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.End,
        ) {
            SampleChip()
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StaticDemoFrame(sample, 0, Modifier.weight(1f))
                StaticDemoFrame(sample, 1, Modifier.weight(1f))
                StaticDemoFrame(sample, 2, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StaticDemoFrame(sample: Watched, type: Int, modifier: Modifier = Modifier) {
    val caption = stringResource(
        when (type) {
            0 -> R.string.intro_demo_caption_tap
            1 -> R.string.intro_demo_caption_drag
            else -> R.string.intro_demo_caption_hide
        },
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Ground)
                .border(1.dp, Hairline, RoundedCornerShape(11.dp)),
        ) {
            MiniAppSkeleton(Modifier.fillMaxSize().padding(6.dp).graphicsLayer(alpha = 0.65f))
            if (type == 0) {
                Surface(
                    Modifier.align(Alignment.TopCenter).padding(top = 34.dp).width(84.dp),
                    color = Raised,
                    shape = RoundedCornerShape(7.dp),
                    shadowElevation = 3.dp,
                ) {
                    Column(Modifier.padding(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinLogo(sample.position.asset, 13.dp)
                            Text(
                                stringResource(
                                    R.string.intro_demo_position_name,
                                    sample.position.asset,
                                    sample.position.side,
                                ),
                                Modifier.padding(start = 4.dp),
                                color = TextPrimary,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 6.sp),
                            )
                        }
                        Canvas(Modifier.fillMaxWidth().height(9.dp).padding(top = 4.dp)) {
                            drawLine(Hairline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2.dp.toPx())
                            drawCircle(levelColor(sample.level), 1.7.dp.toPx(), Offset(size.width * 0.55f, size.height / 2))
                        }
                    }
                }
                Bubble(
                    sampleBubbleUi(sample),
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 1.dp, bottom = 2.dp)
                        .graphicsLayer(scaleX = 0.48f, scaleY = 0.48f, transformOrigin = TransformOrigin(1f, 1f)),
                )
            } else if (type == 1) {
                Bubble(
                    sampleBubbleUi(sample),
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 1.dp)
                        .graphicsLayer(scaleX = 0.52f, scaleY = 0.52f, transformOrigin = TransformOrigin(0f, 0.5f)),
                )
            } else {
                HideTarget(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                    size = 30.dp,
                )
                Bubble(
                    sampleBubbleUi(sample),
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .graphicsLayer(scaleX = 0.42f, scaleY = 0.42f, transformOrigin = TransformOrigin(0.5f, 1f)),
                )
            }
        }
        Text(
            caption,
            Modifier.fillMaxWidth().padding(top = 5.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, lineHeight = 9.sp),
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

@Composable
private fun HideTarget(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 40.dp) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(TextPrimary.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.hide_glyph),
            color = Raised,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp),
        )
    }
}

@Composable
private fun Finger(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(TextPrimary.copy(alpha = 0.25f))
            .border(1.5.dp, TextPrimary.copy(alpha = 0.5f), CircleShape),
    )
}

private data class FingerPosition(
    val x: Float,
    val y: Float,
    val scale: Float = 1f,
    val alpha: Float = 1f,
)

private fun fingerPosition(
    time: Float,
    width: Float,
    height: Float,
    bubbleX: Float,
    bubbleY: Float,
): FingerPosition {
    return when {
        time < 1_200f -> FingerPosition(width * 0.48f, height * 0.7f, alpha = 0f)
        time < 1_550f -> {
            val p = smooth((time - 1_200f) / 350f)
            FingerPosition(lerp(width * 0.48f, width - 47f, p), lerp(height * 0.7f, 134f, p))
        }
        time < 2_800f -> {
            val press = smooth(((time - 1_650f) / 150f).coerceIn(0f, 1f))
            FingerPosition(width - 47f, 134f, 1f - press * 0.15f)
        }
        time < 4_600f -> FingerPosition(bubbleX + 16f, bubbleY + 22f)
        time < 6_400f -> FingerPosition(bubbleX + 16f, bubbleY + 38f)
        else -> FingerPosition(
            bubbleX + 16f,
            bubbleY + 38f,
            alpha = 1f - smooth((time - 6_400f) / 600f),
        )
    }
}

private fun panelProgress(time: Float): Float = when {
    time < 1_450f -> 0f
    time < 1_750f -> smooth((time - 1_450f) / 300f)
    time < 2_350f -> 1f
    time < 2_800f -> 1f - smooth((time - 2_350f) / 450f)
    else -> 0f
}

private fun smooth(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun easeOutBack(value: Float): Float {
    val t = value.coerceIn(0f, 1f) - 1f
    return 1f + t * t * (1.8f * t + 0.8f)
}

private fun lerp(start: Float, end: Float, progress: Float): Float =
    start + (end - start) * progress.coerceIn(0f, 1f)

private fun sampleBubbleUi(sample: Watched): BubbleUi = BubbleUi(
    value = distanceText(sample.distance),
    state = BubbleState.Sample,
    cushion = 1f - sample.travelled.toFloat(),
    animateDanger = false,
    asset = sample.position.asset,
    side = sample.position.side,
)

private fun levelColor(level: Level): Color = when (level) {
    Level.Safe -> Safe
    Level.Caution -> com.liqor.ui.theme.Caution
    Level.Danger -> Danger
}
