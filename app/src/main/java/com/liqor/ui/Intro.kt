package com.liqor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.liqor.R
import com.liqor.domain.Level
import com.liqor.domain.Watched
import com.liqor.domain.distanceText
import com.liqor.ui.components.BrandMark
import com.liqor.ui.components.Bubble
import com.liqor.ui.components.CoinLogo
import com.liqor.ui.components.HeroContent
import com.liqor.ui.components.HeroFooter
import com.liqor.ui.components.StatusChip
import com.liqor.ui.components.SecondaryPillButton
import com.liqor.ui.components.cardBorder
import com.liqor.ui.theme.Danger
import com.liqor.ui.theme.DemoLayout
import com.liqor.ui.theme.Elevation
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.StaleText
import com.liqor.ui.theme.StaleTint
import com.liqor.ui.theme.ScreenSize
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary
import com.liqor.ui.theme.Space
import com.liqor.ui.theme.CardPadding
import com.liqor.ui.theme.Radius
import com.liqor.ui.theme.IllustrationSize
import com.liqor.ui.theme.TypeRamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

@Composable
fun IntroScreen(
    sample: Watched,
    onFinish: () -> Unit,
    initialPage: Int = 0,
    demoTimeMs: Long? = null,
) {
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val autoAdvance = !reducedMotion && demoTimeMs == null && animatedScope != null
    val storyProgress = remember(autoAdvance) {
        Animatable(if (autoAdvance) 0f else 1f)
    }

    LaunchedEffect(pagerState.settledPage, autoAdvance) {
        val page = pagerState.settledPage
        if (!autoAdvance || page >= 2) {
            storyProgress.snapTo(1f)
        } else {
            storyProgress.snapTo(0f)
            storyProgress.animateTo(
                1f,
                tween(INTRO_STORY_DURATION_MS, easing = LinearEasing),
            )
            if (pagerState.settledPage == page && !pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(page + 1)
            }
        }
    }

    val page = pagerState.currentPage
    val labelFadeDuration = if (reducedMotion) 0 else Motion.Quick
    Column(
        Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = Space.screenTop, bottom = Space.screenBottom),
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
                storyProgress = if (autoAdvance) storyProgress.value else 1f,
            )
        }
        HeroFooter(
            primaryText = stringResource(if (page == 2) R.string.intro_get_started else R.string.intro_continue),
            onPrimary = {
                if (page == 2) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(page + 1) }
                }
            },
            primaryLabelContent = {
                AnimatedContent(
                    targetState = page == 2,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = {
                        fadeIn(tween(labelFadeDuration)) togetherWith fadeOut(tween(labelFadeDuration))
                    },
                    label = "intro-primary-label",
                ) { isLastPage ->
                    Text(
                        stringResource(if (isLastPage) R.string.intro_get_started else R.string.intro_continue),
                        style = TypeRamp.button,
                        textAlign = TextAlign.Center,
                    )
                }
            },
            secondaryContent = {
                AnimatedContent(
                    targetState = page < 2,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        fadeIn(tween(labelFadeDuration)) togetherWith fadeOut(tween(labelFadeDuration))
                    },
                    label = "intro-secondary-label",
                ) { showSkip ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (showSkip) {
                            SecondaryPillButton(
                                text = stringResource(R.string.intro_skip),
                                onClick = onFinish,
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.screenHorizontal),
        )
    }
}

@Composable
private fun IntroPage(
    page: Int,
    sample: Watched,
    settled: Boolean,
    pageOffset: Float,
    demoTimeMs: Long?,
    storyProgress: Float,
) {
    val reducedMotion = LocalReducedMotion.current
    val illustrationModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            translationX = if (reducedMotion) 0f else pageOffset * size.width * 0.3f
        }

    HeroContent(
        title = stringResource(
            when (page) {
                0 -> R.string.intro_before_title
                1 -> R.string.intro_watches_title
                else -> R.string.intro_demo_title
            },
        ),
        body = stringResource(
            when (page) {
                0 -> R.string.intro_before_body
                1 -> R.string.intro_watches_body
                else -> R.string.intro_demo_body
            },
        ),
        hasHeroCard = true,
        storySegments = 3,
        storyPage = page,
        storyProgress = storyProgress,
        illustration = {
            when (page) {
                0 -> BeforeAfterIllustration(sample, settled, illustrationModifier)
                1 -> CoverageIllustration(settled, illustrationModifier)
                else -> BubbleDemo(sample, demoTimeMs, illustrationModifier)
            }
        },
    )
}

private const val INTRO_STORY_DURATION_MS = 7_000

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
            bubbleReveal.animateTo(1f, tween(Motion.Medium, easing = Motion.SmoothOut))
        } else {
            wipe.snapTo(0f)
            bubbleReveal.snapTo(0f)
        }
    }

    Surface(
        modifier = modifier.cardBorder(RoundedCornerShape(Radius.lg)),
        color = Raised,
        shape = RoundedCornerShape(Radius.lg),
        shadowElevation = Elevation.card,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val phoneWidthLimit = (maxWidth - CardPadding.content * 2 - Space.sm) / 2
            val density = LocalDensity.current
            val labelHeight = with(density) { TypeRamp.label.lineHeight.toDp() }
            val standardPhoneWidth = minOf(
                phoneWidthLimit,
                (maxHeight - Space.xxxl - CardPadding.content - labelHeight - Space.xs) * (9f / 17f),
            )
            val compact = standardPhoneWidth < IllustrationSize.miniPhoneCompactWidth
            val compactLabelHeight = with(density) { TypeRamp.micro.lineHeight.toDp() * 2 }
            val phoneAspectRatio = if (compact) 2f / 3f else 9f / 17f
            val phoneWidth = if (compact) {
                minOf(
                    phoneWidthLimit,
                    (maxHeight - Space.xxxl - CardPadding.content - compactLabelHeight - Space.xs) * phoneAspectRatio,
                )
            } else {
                standardPhoneWidth
            }
            Row(
                Modifier.fillMaxWidth().padding(
                    start = CardPadding.content,
                    end = CardPadding.content,
                    top = Space.xxxl,
                    bottom = CardPadding.content,
                ),
                horizontalArrangement = Arrangement.Center,
            ) {
                MiniPhone(
                    label = stringResource(R.string.intro_without_liqor),
                    sample = sample,
                    settled = settled,
                    isWithLiqor = false,
                    bubbleScale = 1f,
                    bubbleAlpha = 1f,
                    compact = compact,
                    phoneAspectRatio = phoneAspectRatio,
                    modifier = Modifier.width(phoneWidth),
                )
                Spacer(Modifier.width(Space.sm))
                MiniPhone(
                    label = stringResource(R.string.intro_with_liqor),
                    sample = sample,
                    settled = settled,
                    isWithLiqor = true,
                    bubbleScale = 0.9f + 0.1f * bubbleReveal.value,
                    bubbleAlpha = bubbleReveal.value,
                    compact = compact,
                    phoneAspectRatio = phoneAspectRatio,
                    modifier = Modifier
                        .width(phoneWidth)
                        .drawWithContent {
                            clipRect(right = size.width * wipe.value) {
                                this@drawWithContent.drawContent()
                            }
                        },
                )
            }
        }
    }
}

@Composable
private fun MiniPhone(
    label: String,
    sample: Watched,
    settled: Boolean,
    isWithLiqor: Boolean,
    bubbleScale: Float,
    bubbleAlpha: Float,
    compact: Boolean,
    phoneAspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    val fontScale = LocalDensity.current.fontScale
    val captionLines = if (compact || fontScale > 1f) 3 else 1
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            color = TextSecondary,
            style = if (compact) TypeRamp.micro else TypeRamp.label,
            maxLines = if (compact) 2 else 1,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = Space.xs)
                .aspectRatio(phoneAspectRatio)
                .clip(RoundedCornerShape(Radius.sm))
                .background(Ground)
                .cardBorder(RoundedCornerShape(Radius.sm)),
        ) {
            Column(
                Modifier.fillMaxSize().padding(
                    if (compact) Space.xs else CardPadding.inner,
                ),
            ) {
                MiniAppSkeleton(
                    Modifier.weight(1f),
                    tokenAssets = beforeAfterTokenAssets,
                    settled = settled,
                    skeletonAlpha = if (isWithLiqor) 1f else 0.6f,
                    compactSpacing = fontScale > 1f,
                )
                if (!isWithLiqor) {
                    if (!compact) {
                        CaptionPill(
                            stringResource(R.string.intro_last_checked_caption),
                            Modifier
                                .then(if (fontScale > 1f) Modifier.fillMaxWidth() else Modifier)
                                .padding(top = Space.sm),
                            stale = true,
                            maxLines = captionLines,
                        )
                    }
                    LiquidatedEventCard(
                        modifier = Modifier.padding(top = Space.xs),
                        compact = compact,
                        settled = settled,
                    )
                }
            }
            if (isWithLiqor) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(
                                top = maxHeight * if (compact) 0f else 0.36f,
                                end = IllustrationSize.hairline,
                            )
                            .graphicsLayer {
                                scaleX = bubbleScale * 0.8f
                                scaleY = bubbleScale * 0.8f
                                alpha = bubbleAlpha
                            }
                            .size(IllustrationSize.demoBubble),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Bubble(sampleBubbleUi(sample))
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = Space.md)
                                .clip(CircleShape)
                                .background(TextPrimary)
                                .padding(
                                    start = Space.xs / 2,
                                    end = Space.sm / 2,
                                    top = Space.xs / 2,
                                    bottom = Space.xs / 2,
                                ),
                            horizontalArrangement = Arrangement.spacedBy(Space.xs / 2),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MiniAppAvatar(
                                asset = "SOL",
                                size = IllustrationSize.bubbleTagLogo,
                                index = beforeAfterTokenAssets.size + 1,
                                settled = settled,
                            )
                            Text(
                                stringResource(R.string.bubble_tag_long, "SOL"),
                                color = Raised,
                                style = TypeRamp.label,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val beforeAfterTokenAssets = listOf("SOL", "ETH", "BTC", "JUP", "HYPE", "ZEC")

@Composable
private fun MiniAppSkeleton(
    modifier: Modifier = Modifier,
    tokenAssets: List<String>? = null,
    settled: Boolean = false,
    skeletonAlpha: Float = 1f,
    compactSpacing: Boolean = false,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(if (compactSpacing) Space.xs else Space.sm),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = if (compactSpacing) Space.xs / 2 else Space.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniAppAvatar(
                asset = tokenAssets?.takeIf { it.isNotEmpty() }?.get(0),
                size = IllustrationSize.headerAvatar,
                index = 0,
                settled = settled,
            )
            Column(Modifier.weight(1f).padding(start = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                SkeletonBar(
                    Modifier.fillMaxWidth(0.72f).height(IllustrationSize.skeletonMain),
                    skeletonAlpha,
                )
                SkeletonBar(
                    Modifier.fillMaxWidth(0.45f).height(IllustrationSize.skeletonSmall),
                    skeletonAlpha,
                )
            }
            Box(
                Modifier.size(Space.xs)
                    .graphicsLayer(alpha = skeletonAlpha)
                    .clip(CircleShape)
                    .background(Hairline),
            )
        }
        repeat(5) { index ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val avatarIndex = index + 1
                MiniAppAvatar(
                    asset = tokenAssets?.takeIf { it.isNotEmpty() }?.let {
                        it[avatarIndex % it.size]
                    },
                    size = IllustrationSize.skeletonAvatar,
                    index = avatarIndex,
                    settled = settled,
                )
                Column(
                    Modifier.weight(1f).padding(start = Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.xs),
                ) {
                    SkeletonBar(
                        Modifier.fillMaxWidth(if (index % 2 == 0) 0.9f else 0.72f)
                            .height(IllustrationSize.skeletonMedium),
                        skeletonAlpha,
                    )
                    SkeletonBar(
                        Modifier.fillMaxWidth(if (index % 2 == 0) 0.58f else 0.43f)
                            .height(IllustrationSize.skeletonSmall),
                        skeletonAlpha,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniAppAvatar(asset: String?, size: Dp, index: Int, settled: Boolean) {
    if (asset == null) {
        Box(Modifier.size(size).clip(CircleShape).background(Hairline))
        return
    }

    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val shouldAnimate = !reducedMotion && animatedScope != null && settled
    val progress = remember(asset, index) { Animatable(if (shouldAnimate) 0f else 1f) }
    LaunchedEffect(settled, shouldAnimate, asset, index) {
        if (reducedMotion || animatedScope == null) {
            progress.snapTo(1f)
        } else if (settled) {
            progress.snapTo(0f)
            delay((index * Motion.IconStagger).toLong())
            progress.animateTo(1f, tween(Motion.Medium, easing = Motion.SmoothOut))
        } else {
            progress.snapTo(0f)
        }
    }
    val density = LocalDensity.current
    CoinLogo(
        asset,
        size,
        modifier = Modifier.graphicsLayer {
            alpha = progress.value
            translationX = with(density) {
                (Space.sm * (1f - progress.value)).toPx()
            }
            scaleX = 0.9f + 0.1f * progress.value
            scaleY = 0.9f + 0.1f * progress.value
        },
    )
}

@Composable
private fun SkeletonBar(modifier: Modifier = Modifier, alpha: Float = 1f) {
    Box(modifier.graphicsLayer(alpha = alpha).clip(CircleShape).background(StaleTint))
}

@Composable
private fun CaptionPill(
    text: String,
    modifier: Modifier = Modifier,
    stale: Boolean = false,
    maxLines: Int = 1,
) {
    Text(
        text,
        modifier
            .clip(CircleShape)
            .background(if (stale) StaleTint else Raised)
            .padding(horizontal = Space.sm, vertical = Space.xs),
        color = if (stale) StaleText else TextSecondary,
        style = TypeRamp.micro,
        maxLines = maxLines,
    )
}

@Composable
private fun LiquidatedEventCard(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    settled: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth()
            .cardBorder(RoundedCornerShape(IllustrationSize.eventRadius)),
        color = Raised,
        shape = RoundedCornerShape(IllustrationSize.eventRadius),
        shadowElevation = Elevation.card,
    ) {
        Row(
            Modifier.padding(
                horizontal = if (compact) Space.xs else Space.sm,
                vertical = Space.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(IllustrationSize.eventAccent).height(Space.lg + Space.xs).clip(CircleShape).background(Danger))
            MiniAppAvatar(
                asset = "SOL",
                size = IllustrationSize.skeletonAvatar,
                index = beforeAfterTokenAssets.size,
                settled = settled,
            )
            Column(Modifier.padding(start = if (compact) Space.xs else Space.sm)) {
                Text(
                    stringResource(R.string.intro_without_event),
                    color = TextPrimary,
                    style = TypeRamp.micro,
                    maxLines = 2,
                )
                Text(
                    stringResource(R.string.intro_without_event_time),
                    color = TextSecondary,
                    style = TypeRamp.micro,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun CoverageIllustration(
    settled: Boolean,
    modifier: Modifier,
    animateHighlight: Boolean = true,
) {
    val shouldHighlight =
        animateHighlight && settled && !LocalReducedMotion.current && LocalAnimatedScope.current != null
    val highlightPhase = if (shouldHighlight) {
        val transition = rememberInfiniteTransition(label = "venue-highlights")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 3f,
            animationSpec = infiniteRepeatable(
                tween(Motion.VenueHighlightPeriod * 3, easing = LinearEasing),
            ),
            label = "venue-highlight-phase",
        ).value
    } else {
        0f
    }
    val activeVenue = floor(highlightPhase).toInt().coerceIn(0, 2)
    val venuePulse = (sin((highlightPhase - activeVenue) * PI) * 0.08).toFloat().coerceAtLeast(0f)
    Surface(
        modifier = modifier.cardBorder(RoundedCornerShape(Radius.lg)),
        color = Raised,
        shape = RoundedCornerShape(Radius.lg),
        shadowElevation = Elevation.card,
    ) {
        Column(
            Modifier.fillMaxSize().padding(CardPadding.content),
            verticalArrangement = Arrangement.spacedBy(Space.md),
        ) {
                Text(
                    stringResource(R.string.intro_venues_label),
                    color = TextSecondary,
                    style = TypeRamp.label,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                    VenueTile(
                        "SOL",
                        R.string.intro_venue_solana,
                        Modifier.weight(1f),
                        pulseAlpha = if (shouldHighlight && activeVenue == 0) venuePulse else 0f,
                    )
                    VenueTile(
                        "JUP",
                        R.string.intro_venue_perps,
                        Modifier.weight(1f),
                        pulseAlpha = if (shouldHighlight && activeVenue == 1) venuePulse else 0f,
                    )
                    VenueTile(
                        "JUP",
                        R.string.intro_venue_beta,
                        Modifier.weight(1f),
                        pulseAlpha = if (shouldHighlight && activeVenue == 2) venuePulse else 0f,
                    )
                }
                Text(
                    stringResource(R.string.intro_crypto_label),
                    color = TextSecondary,
                    style = TypeRamp.label,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FanCoinStack(listOf("SOL", "ETH", "BTC", "JUP", "HYPE", "ZEC"), settled)
                    Text(
                        stringResource(R.string.intro_crypto_assets),
                        Modifier.weight(1f).padding(start = Space.sm),
                        color = TextPrimary,
                        style = TypeRamp.caption,
                    )
                }
                Text(
                    stringResource(R.string.intro_stocks_label),
                    color = TextSecondary,
                    style = TypeRamp.label,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FanCoinStack(listOf("SPCX", "SNDK", "SKHYNIX"), settled, sequenceOffset = 6)
                    Text(
                        stringResource(R.string.intro_stock_names),
                        Modifier.weight(1f).padding(start = Space.sm),
                        color = TextPrimary,
                        style = TypeRamp.caption,
                    )
                }
        }
    }
}

@Composable
private fun VenueTile(
    asset: String,
    nameResource: Int,
    modifier: Modifier = Modifier,
    pulseAlpha: Float = 0f,
) {
    val venueHeight = if (LocalDensity.current.fontScale > 1f) {
        IllustrationSize.venueHeightLarge
    } else {
        IllustrationSize.venueHeight
    }
    Column(
        modifier
            .height(venueHeight)
            .clip(RoundedCornerShape(Radius.sm))
            .background(lerp(Ground, TextPrimary, pulseAlpha))
            .padding(horizontal = Space.xs, vertical = Space.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        CoinLogo(asset, IllustrationSize.venueLogo)
        Text(
            stringResource(nameResource),
            color = TextPrimary,
            style = TypeRamp.caption,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun FanCoinStack(assets: List<String>, settled: Boolean, sequenceOffset: Int = 0) {
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val shouldAnimate = !reducedMotion && animatedScope != null && settled
    val driftPhase = if (shouldAnimate) {
        val transition = rememberInfiniteTransition(label = "token-icon-drift")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                tween(Motion.IconDriftPeriod, easing = LinearEasing),
            ),
            label = "token-icon-drift-phase",
        ).value
    } else {
        0f
    }
    val stockStack = assets.all { it in stockAssets }
    val iconSize = IllustrationSize.logo
    val diameter = if (stockStack) IllustrationSize.stockLogo else IllustrationSize.logo
    val step = diameter - Space.sm
    val density = LocalDensity.current
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
                    delay(((sequenceOffset + index) * Motion.IconStagger).toLong())
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
                        .offset(x = step * index)
                        .zIndex(index.toFloat())
                        .graphicsLayer {
                            alpha = progress.value
                            translationX = with(density) {
                                (Space.sm * (1f - progress.value)).toPx()
                            }
                            translationY = if (progress.value >= 1f && shouldAnimate) {
                                with(density) { Motion.IconDriftAmplitude.toPx() } *
                                    sin(driftPhase + 2f * PI.toFloat() * index / assets.size)
                            } else {
                                0f
                            }
                            scaleX = 0.9f + 0.1f * progress.value
                            scaleY = 0.9f + 0.1f * progress.value
                        },
                    imageInset = IllustrationSize.stockLogoInset,
                    containerColor = Raised,
                    borderColor = Hairline,
                )
            } else {
                Box(
                    Modifier
                        .offset(x = step * index)
                        .size(diameter)
                        .zIndex(index.toFloat())
                        .graphicsLayer {
                            alpha = progress.value
                            translationX = with(density) {
                                (Space.sm * (1f - progress.value)).toPx()
                            }
                            translationY = if (progress.value >= 1f && shouldAnimate) {
                                with(density) { Motion.IconDriftAmplitude.toPx() } *
                                    sin(driftPhase + 2f * PI.toFloat() * index / assets.size)
                            } else {
                                0f
                            }
                            scaleX = 0.9f + 0.1f * progress.value
                            scaleY = 0.9f + 0.1f * progress.value
                        }
                        .clip(CircleShape)
                        .background(Raised)
                        .border(Space.xs / 2, Raised, CircleShape),
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
    val hasAnimatedScope = LocalAnimatedScope.current != null
    val shouldAnimate = !reducedMotion && hasAnimatedScope
    val animatedTime = if (timeMs == null && shouldAnimate) {
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

    if (reducedMotion || (timeMs == null && !hasAnimatedScope)) {
        ReducedMotionDemo(sample, modifier)
        return
    }

    Surface(
        modifier = modifier.cardBorder(RoundedCornerShape(Radius.lg)),
        color = Raised,
        shape = RoundedCornerShape(Radius.lg),
        shadowElevation = Elevation.card,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val phoneHeight = (maxHeight - Space.xxxl).coerceAtLeast(IllustrationSize.miniPhoneHeight)
            AnimatedContent(
                targetState = captionIndex,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(
                        start = CardPadding.content,
                        end = CardPadding.content,
                        bottom = CardPadding.content,
                    ),
                transitionSpec = {
                    fadeIn(tween(Motion.Fast)) togetherWith fadeOut(tween(Motion.Fast))
                },
                label = "bubble-demo-caption",
            ) { index ->
                Text(
                    stringResource(captions[index]),
                    color = TextSecondary,
                    style = TypeRamp.caption,
                    textAlign = TextAlign.Center,
                )
            }
            Box(Modifier.align(Alignment.TopCenter).padding(top = DemoLayout.phoneTop)) {
                AnimatedDemoPhone(
                    sample,
                    time,
                    Modifier.height(phoneHeight),
                )
            }
        }
    }
}

@Composable
private fun AnimatedDemoPhone(sample: Watched, time: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .width(IllustrationSize.phoneWidth)
            .clip(RoundedCornerShape(Radius.sm))
            .background(Ground)
            .cardBorder(RoundedCornerShape(Radius.sm)),
    ) {
        val width = maxWidth.value
        val height = maxHeight.value
        val enterProgress = smooth((time / 1_200f).coerceIn(0f, 1f))
        val resetProgress = if (time >= 6_400f) smooth(((time - 6_400f) / 600f).coerceIn(0f, 1f)) else 0f
        val resetAlpha = if (time >= 6_400f) 1f - ((time - 6_400f) / 600f).coerceIn(0f, 1f) else 1f

        var bubbleX = lerp(width + Space.sm.value, width - DemoLayout.bubbleRightInset.value, enterProgress)
        var bubbleY = DemoLayout.bubbleTop.value
        var bubbleScale = 1f
        var bubbleAlpha = enterProgress * resetAlpha
        if (time in 2_800f..4_600f) {
            val p = easeOutBack(((time - 2_800f) / 1_800f).coerceIn(0f, 1f))
            bubbleX = lerp(
                width - DemoLayout.bubbleRightInset.value,
                Space.xs.value,
                p,
            ).coerceIn(0f, width - IllustrationSize.demoBubble.value)
        } else if (time in 4_600f..6_400f) {
            val p = smooth(((time - 4_600f) / 1_800f).coerceIn(0f, 1f))
            bubbleX = lerp(
                Space.xs.value,
                (width - IllustrationSize.demoBubble.value) / 2f,
                p,
            )
            bubbleY = lerp(DemoLayout.bubbleTop.value, height - DemoLayout.bubbleHideBottom.value, p)
            val shrink = smooth(((time - 5_700f) / 500f).coerceIn(0f, 1f))
            bubbleScale = lerp(1f, 0.28f, shrink)
            bubbleAlpha = (1f - shrink) * resetAlpha
        } else if (time >= 6_400f) {
            bubbleX = lerp(
                (width - IllustrationSize.demoBubble.value) / 2f,
                width + Space.sm.value,
                resetProgress,
            )
                bubbleY = lerp(
                    height - DemoLayout.bubbleHideBottom.value,
                    DemoLayout.bubbleTop.value,
                    resetProgress,
                )
        }

        MiniAppSkeleton(
            Modifier.fillMaxSize().padding(CardPadding.inner).graphicsLayer(alpha = 0.65f),
        )

        val panelProgress = panelProgress(time)
        if (panelProgress > 0f) {
            DemoPanel(
                sample,
                Modifier
                    .offset(
                        x = (
                            bubbleX +
                                IllustrationSize.demoBubble.value -
                                IllustrationSize.demoPanelWidth.value
                            ).dp,
                        y = (bubbleY + IllustrationSize.demoBubble.value + DemoLayout.bubblePanelGap.value).dp,
                    )
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
                    .padding(bottom = DemoLayout.hideTargetBottom)
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
        modifier = modifier.width(IllustrationSize.demoPanelWidth)
            .cardBorder(RoundedCornerShape(Radius.md)),
        color = Raised,
        shape = RoundedCornerShape(Radius.md),
        shadowElevation = Elevation.card,
    ) {
        Column(Modifier.padding(CardPadding.inner)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinLogo(sample.position.asset, DemoLayout.panelRowIcon)
                Text(
                    stringResource(
                        R.string.intro_demo_position_name,
                        sample.position.asset,
                        sample.position.side,
                    ),
                    Modifier.weight(1f).padding(start = Space.xs),
                    color = TextPrimary,
                    style = TypeRamp.row,
                    maxLines = 1,
                )
                StatusChip(
                    sample.level,
                    label = "${distanceText(sample.distance)}%",
                )
            }
            Canvas(Modifier.fillMaxWidth().height(IllustrationSize.demoTrack).padding(top = Space.xs)) {
                val start = size.width * 0.1f
                val end = size.width * 0.9f
                val y = size.height * 0.6f
                val now = start + (end - start) * sample.travelled.toFloat().coerceIn(0f, 1f)
                drawLine(Hairline, Offset(start, y), Offset(end, y), IllustrationSize.demoTrackStroke.toPx())
                drawLine(levelColor(sample.level), Offset(start, y), Offset(now, y), IllustrationSize.demoTrackStroke.toPx())
                drawCircle(TextSecondary, Space.xs.toPx() / 2, Offset(start, y))
                drawCircle(levelColor(sample.level), Space.xs.toPx() * 0.625f, Offset(now, y))
                drawCircle(Danger, Space.xs.toPx() / 2, Offset(end, y))
            }
        }
    }
}

@Composable
private fun ReducedMotionDemo(sample: Watched, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.cardBorder(RoundedCornerShape(Radius.lg)),
        color = Raised,
        shape = RoundedCornerShape(Radius.lg),
        shadowElevation = Elevation.card,
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
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
                .height(IllustrationSize.miniFrameHeight)
                .clip(RoundedCornerShape(Radius.sm))
                .background(Ground)
                .cardBorder(RoundedCornerShape(Radius.sm)),
        ) {
            MiniAppSkeleton(Modifier.fillMaxSize().padding(Space.xs).graphicsLayer(alpha = 0.65f))
            if (type == 0) {
                Surface(
                    Modifier.align(Alignment.TopCenter).padding(top = DemoLayout.miniPanelTop)
                        .width(IllustrationSize.demoMiniPanelWidth)
                        .cardBorder(RoundedCornerShape(DemoLayout.miniPanelRadius)),
                    color = Raised,
                    shape = RoundedCornerShape(DemoLayout.miniPanelRadius),
                    shadowElevation = Elevation.card,
                ) {
                    Column(Modifier.padding(DemoLayout.miniPanelInner)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CoinLogo(sample.position.asset, IllustrationSize.venueLogo)
                            Text(
                                stringResource(
                                    R.string.intro_demo_position_name,
                                    sample.position.asset,
                                    sample.position.side,
                                ),
                                Modifier.padding(start = Space.xs),
                                color = TextPrimary,
                                style = TypeRamp.micro,
                                maxLines = 1,
                            )
                        }
                        Canvas(Modifier.fillMaxWidth().height(DemoLayout.miniPanelTrack).padding(top = Space.xs)) {
                            drawLine(Hairline, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), Space.xs.toPx() / 2)
                            drawCircle(levelColor(sample.level), Space.xs.toPx() * 0.425f, Offset(size.width * 0.55f, size.height / 2))
                        }
                    }
                }
                Bubble(
                    sampleBubbleUi(sample),
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = IllustrationSize.hairline, bottom = Space.xs / 2)
                        .graphicsLayer(scaleX = 0.48f, scaleY = 0.48f, transformOrigin = TransformOrigin(1f, 1f)),
                )
            } else if (type == 1) {
                Bubble(
                    sampleBubbleUi(sample),
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = IllustrationSize.hairline)
                        .graphicsLayer(scaleX = 0.52f, scaleY = 0.52f, transformOrigin = TransformOrigin(0f, 0.5f)),
                )
            } else {
                HideTarget(
                    Modifier.align(Alignment.BottomCenter).padding(bottom = Space.xs),
                    size = IllustrationSize.hideTarget,
                )
                Bubble(
                    sampleBubbleUi(sample),
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Space.xs)
                        .graphicsLayer(scaleX = 0.42f, scaleY = 0.42f, transformOrigin = TransformOrigin(0.5f, 1f)),
                )
            }
        }
        Text(
            caption,
            Modifier.fillMaxWidth().padding(top = DemoLayout.miniCaptionTop),
            color = TextSecondary,
            style = TypeRamp.caption,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun HideTarget(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = IllustrationSize.hideTarget,
) {
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
            style = TypeRamp.bodyStrong.copy(fontSize = 20.sp, lineHeight = 24.sp),
        )
    }
}

@Composable
private fun Finger(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(IllustrationSize.demoFinger)
            .clip(CircleShape)
            .background(TextPrimary.copy(alpha = 0.25f))
            .border(Space.xs * 0.375f, TextPrimary.copy(alpha = 0.5f), CircleShape),
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
        time < 1_200f -> FingerPosition(DemoLayout.fingerTravelStartX.value, height * 0.7f, alpha = 0f)
        time < 1_550f -> {
            val p = smooth((time - 1_200f) / 350f)
            FingerPosition(
                lerp(
                    DemoLayout.fingerTravelStartX.value,
                    width - DemoLayout.fingerStartXInset.value,
                    p,
                ),
                lerp(height * 0.7f, DemoLayout.fingerStartY.value, p),
            )
        }
        time < 2_800f -> {
            val press = smooth(((time - 1_650f) / 150f).coerceIn(0f, 1f))
            FingerPosition(
                width - DemoLayout.fingerStartXInset.value,
                DemoLayout.fingerStartY.value,
                1f - press * 0.15f,
            )
        }
        time < 4_600f -> FingerPosition(
            bubbleX + DemoLayout.fingerCenterOffset.value,
            bubbleY + DemoLayout.fingerTapOffset.value,
        )
        time < 6_400f -> FingerPosition(
            bubbleX + DemoLayout.fingerCenterOffset.value,
            bubbleY + DemoLayout.fingerHideOffset.value,
        )
        else -> FingerPosition(
            bubbleX + DemoLayout.fingerCenterOffset.value,
            bubbleY + DemoLayout.fingerHideOffset.value,
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

fun sampleBubbleUi(sample: Watched): BubbleUi = BubbleUi(
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
