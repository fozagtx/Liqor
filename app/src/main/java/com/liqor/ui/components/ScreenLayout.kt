package com.liqor.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liqor.R
import com.liqor.ui.LocalReducedMotion
import com.liqor.ui.LocalAnimatedScope
import com.liqor.ui.theme.CardPadding
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.HeroCardPalette
import com.liqor.ui.theme.IllustrationSize
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.Radius
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.ScreenSize
import com.liqor.ui.theme.Space
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary
import com.liqor.ui.theme.TypeRamp
import com.liqor.ui.theme.WaveBandsSize

@Composable
fun ScreenHeader(
    right: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Row(
        modifier.height(ScreenSize.headerHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Text(
            stringResource(R.string.app_name),
            Modifier.padding(start = Space.sm),
            color = TextPrimary,
            style = TypeRamp.brand,
        )
        Spacer(Modifier.weight(1f))
        right?.invoke()
    }
}

@Composable
fun StepProgress(currentStep: Int, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(top = Space.xs),
        horizontalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        repeat(2) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(ScreenSize.progressHeight)
                    .clip(CircleShape)
                    .background(if (index < currentStep) TextPrimary else Hairline),
            )
        }
    }
}

@Composable
fun ScreenFooter(
    primaryText: String? = null,
    onPrimary: (() -> Unit)? = null,
    primaryGlyph: String? = null,
    primaryLabelContent: (@Composable () -> Unit)? = null,
    pagerPage: Int? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryPill: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val hasPrimary = primaryText != null && onPrimary != null
    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            if (pagerPage != null) {
                ScreenPagerDots(pagerPage)
            }
            Spacer(Modifier.height(Space.footerDotsButton))
            if (hasPrimary) {
                PillButton(
                    text = primaryText.orEmpty(),
                    onClick = onPrimary,
                    height = ScreenSize.primaryButtonHeight,
                    leadingGlyph = primaryGlyph,
                    labelContent = primaryLabelContent,
                )
            }
            if (hasPrimary) Spacer(Modifier.height(Space.footerButtonSecondary))
            Box(
                Modifier.fillMaxWidth().height(
                    if (secondaryPill) ScreenSize.heroSecondaryHeight else ScreenSize.secondaryHeight,
                ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    secondaryContent != null -> secondaryContent()
                    secondaryText != null && onSecondary != null && secondaryPill -> SecondaryPillButton(
                        text = secondaryText,
                        onClick = onSecondary,
                        height = ScreenSize.heroSecondaryHeight,
                    )
                    secondaryText != null && onSecondary != null -> TextAction(secondaryText, onSecondary)
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(Space.footerFade)
                .offset(y = -Space.footerFade)
                .background(
                    Brush.verticalGradient(
                        listOf(Ground.copy(alpha = 0f), Ground),
                    ),
                ),
        )
    }
}

@Composable
fun HeroFooter(
    primaryText: String? = null,
    onPrimary: (() -> Unit)? = null,
    primaryGlyph: String? = null,
    primaryLabelContent: (@Composable () -> Unit)? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val hasPrimary = primaryText != null && onPrimary != null
    Column(modifier.fillMaxWidth()) {
        if (hasPrimary) {
            PillButton(
                text = primaryText.orEmpty(),
                onClick = onPrimary,
                height = ScreenSize.primaryButtonHeight,
                leadingGlyph = primaryGlyph,
                labelContent = primaryLabelContent,
            )
            Spacer(Modifier.height(Space.heroFooterButtonGap))
        }
        Box(
            Modifier.fillMaxWidth().height(ScreenSize.heroSecondaryHeight),
            contentAlignment = Alignment.Center,
        ) {
            when {
                secondaryContent != null -> secondaryContent()
                secondaryText != null && onSecondary != null -> SecondaryPillButton(
                    text = secondaryText,
                    onClick = onSecondary,
                )
            }
        }
    }
}

@Composable
private fun ScreenPagerDots(page: Int) {
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    Row(
        Modifier.fillMaxWidth().height(ScreenSize.dot),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val width by animateDpAsState(
                targetValue = if (index == page) ScreenSize.activeDot else ScreenSize.dot,
                animationSpec = tween(if (reducedMotion || animatedScope == null) 0 else Motion.Fast),
                label = "screen-pager-dot",
            )
            Box(
                Modifier
                    .padding(horizontal = Space.dotGap / 2)
                    .size(width = width, height = ScreenSize.dot)
                    .clip(RoundedCornerShape(Space.xs))
                    .background(if (index == page) TextPrimary else Hairline),
            )
        }
    }
}

@Composable
fun HeroTemplate(
    title: String?,
    body: String? = null,
    hasHeroCard: Boolean = false,
    headerRight: (@Composable () -> Unit)? = null,
    stepLabel: String? = null,
    currentStep: Int = 0,
    storySegments: Int = 0,
    storyPage: Int = 0,
    storyProgress: Float = 1f,
    primaryText: String,
    onPrimary: () -> Unit,
    primaryGlyph: String? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    illustration: @Composable BoxScope.(Dp) -> Unit,
    afterBody: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = Space.screenTop, bottom = Space.screenBottom),
    ) {
        HeroContent(
            title = title,
            body = body,
            hasHeroCard = hasHeroCard,
            headerRight = headerRight,
            stepLabel = stepLabel,
            currentStep = currentStep,
            storySegments = storySegments,
            storyPage = storyPage,
            storyProgress = storyProgress,
            illustration = illustration,
            afterBody = afterBody,
            modifier = Modifier.weight(1f),
        )
        HeroFooter(
            primaryText = primaryText,
            onPrimary = onPrimary,
            primaryGlyph = primaryGlyph,
            secondaryContent = secondaryContent,
            secondaryText = secondaryText,
            onSecondary = onSecondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.screenHorizontal),
        )
    }
}

@Composable
fun HeroContent(
    title: String?,
    body: String? = null,
    hasHeroCard: Boolean = false,
    headerRight: (@Composable () -> Unit)? = null,
    stepLabel: String? = null,
    currentStep: Int = 0,
    storySegments: Int = 0,
    storyPage: Int = 0,
    storyProgress: Float = 1f,
    modifier: Modifier = Modifier,
    illustration: @Composable BoxScope.(Dp) -> Unit,
    afterBody: @Composable ColumnScope.() -> Unit = {},
) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        SubcomposeLayout(Modifier.verticalScroll(scrollState)) {
            val width = constraints.maxWidth
            val text = subcompose(HeroTextSlot) {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = Space.screenHorizontal),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (title != null) {
                        Text(
                            title,
                            Modifier.fillMaxWidth(),
                            color = TextPrimary,
                            style = TypeRamp.title.copy(lineBreak = LineBreak.Heading),
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (body != null) {
                        Spacer(Modifier.height(Space.labelTitle))
                        Text(
                            body,
                            Modifier.fillMaxWidth(),
                            color = TextSecondary,
                            style = TypeRamp.body.copy(lineBreak = LineBreak.Heading),
                            textAlign = TextAlign.Center,
                        )
                    }
                    afterBody()
                }
            }.single().measure(
                Constraints(
                    maxWidth = width,
                    maxHeight = Constraints.Infinity,
                ),
            )
            val textTop = Space.heroTextTop.roundToPx()
            val footerGap = Space.heroTextFooter.roundToPx()
            val heroHeight = (
                viewportHeight.roundToPx() - text.height - textTop - footerGap
            ).coerceAtLeast(Space.heroMin.roundToPx())
            val hero = subcompose(HeroVisualSlot) {
                HeroVisual(
                    heroHeight = with(density) { heroHeight.toDp() },
                    hasHeroCard = hasHeroCard,
                    headerRight = headerRight,
                    stepLabel = stepLabel,
                    currentStep = currentStep,
                    storySegments = storySegments,
                    storyPage = storyPage,
                    storyProgress = storyProgress,
                    illustration = illustration,
                )
            }.single().measure(Constraints.fixed(width, heroHeight))
            val contentHeight = heroHeight + textTop + text.height + footerGap

            layout(width, contentHeight) {
                hero.placeRelative(0, 0)
                text.placeRelative(0, heroHeight + textTop)
            }
        }
    }
}

@Composable
private fun HeroVisual(
    heroHeight: Dp,
    hasHeroCard: Boolean,
    headerRight: (@Composable () -> Unit)?,
    stepLabel: String?,
    currentStep: Int,
    storySegments: Int,
    storyPage: Int,
    storyProgress: Float,
    illustration: @Composable BoxScope.(Dp) -> Unit,
) {
    if (hasHeroCard) {
        val cardShape = RoundedCornerShape(Radius.lg)
        Box(
            Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .padding(horizontal = Space.heroCardHorizontal)
                .clip(cardShape)
                .background(HeroCardPalette.background)
                .border(
                    IllustrationSize.cardBorder,
                    HeroCardPalette.border,
                    cardShape,
                ),
        ) {
            WaveBands(
                color = HeroCardPalette.wave,
                bandCount = 3,
                startFraction = 0.55f,
                thickness = WaveBandsSize.heroThickness,
                gap = WaveBandsSize.heroGap,
                bulge = WaveBandsSize.heroBulge,
                modifier = Modifier.fillMaxSize(),
            )
            Column(Modifier.fillMaxSize().padding(CardPadding.hero)) {
                val segmentCount = storySegments.takeIf { it > 0 }
                    ?: if (stepLabel != null) 2 else 0
                if (segmentCount > 0) {
                    StoryProgress(
                        segmentCount = segmentCount,
                        activeSegment = if (storySegments > 0) storyPage else currentStep - 1,
                        progress = storyProgress,
                    )
                    Spacer(Modifier.height(Space.heroProgressHeader))
                }
                ScreenHeader(
                    right = headerRight ?: stepLabel?.let { step ->
                        {
                            Text(step, color = TextSecondary, style = TypeRamp.label)
                        }
                    },
                )
                HeroIllustrationArea(Modifier.fillMaxWidth().weight(1f), illustration)
            }
        }
    } else {
        Column(Modifier.fillMaxWidth().height(heroHeight)) {
            Column(Modifier.fillMaxWidth().padding(horizontal = Space.screenHorizontal)) {
                ScreenHeader(
                    right = headerRight ?: stepLabel?.let { step ->
                        {
                            Text(step, color = TextSecondary, style = TypeRamp.label)
                        }
                    },
                )
                if (stepLabel != null) {
                    StepProgress(currentStep)
                    Spacer(Modifier.height(Space.stepContent))
                } else {
                    Spacer(Modifier.height(Space.headerContent))
                }
            }
            HeroIllustrationArea(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = Space.screenHorizontal),
                illustration,
            )
        }
    }
}

@Composable
private fun HeroIllustrationArea(
    modifier: Modifier,
    illustration: @Composable BoxScope.(Dp) -> Unit,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val illustrationHeight = minOf(maxHeight, Space.heroIllustrationMax)
        Box(
            Modifier.fillMaxWidth().height(illustrationHeight),
            contentAlignment = Alignment.Center,
            content = { illustration(illustrationHeight) },
        )
    }
}

private object HeroTextSlot
private object HeroVisualSlot

@Composable
private fun StoryProgress(segmentCount: Int, activeSegment: Int, progress: Float) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Space.storySegmentGap),
    ) {
        repeat(segmentCount) { index ->
            val fill = when {
                index < activeSegment -> 1f
                index == activeSegment -> progress.coerceIn(0f, 1f)
                else -> 0f
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(ScreenSize.progressHeight)
                    .clip(CircleShape)
                    .background(HeroCardPalette.progressTrack),
            ) {
                if (fill > 0f) {
                    Box(
                        Modifier
                            .fillMaxWidth(fill)
                            .fillMaxHeight()
                            .background(HeroCardPalette.progressFill),
                    )
                }
            }
        }
    }
}

@Composable
fun FormTemplate(
    headerRight: (@Composable () -> Unit)? = null,
    stepLabel: String? = null,
    currentStep: Int = 0,
    primaryText: String? = null,
    onPrimary: (() -> Unit)? = null,
    primaryGlyph: String? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    secondaryPill: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(
                start = Space.screenHorizontal,
                top = Space.screenTop,
                end = Space.screenHorizontal,
                bottom = Space.screenBottom,
            ),
    ) {
        ScreenHeader(
            right = headerRight ?: stepLabel?.let { step ->
                {
                    Text(step, color = TextSecondary, style = TypeRamp.label)
                }
            },
        )
        if (stepLabel != null) {
            StepProgress(currentStep)
            Spacer(Modifier.height(Space.stepContent))
        } else {
            Spacer(Modifier.height(Space.headerContent))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            content = content,
        )
        ScreenFooter(
            primaryText = primaryText,
            onPrimary = onPrimary,
            primaryGlyph = primaryGlyph,
            secondaryContent = secondaryContent,
            secondaryText = secondaryText,
            onSecondary = onSecondary,
            secondaryPill = secondaryPill,
        )
    }
}
