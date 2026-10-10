package com.liqor.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liqor.R
import com.liqor.ui.LocalReducedMotion
import com.liqor.ui.LocalAnimatedScope
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.ScreenSize
import com.liqor.ui.theme.Space
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary
import com.liqor.ui.theme.TypeRamp

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
    pagerPage: Int? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    secondaryCaption: String? = null,
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
                    onClick = onPrimary ?: {},
                    height = ScreenSize.primaryButtonHeight,
                    leadingGlyph = primaryGlyph,
                )
            }
            if (hasPrimary) Spacer(Modifier.height(Space.footerButtonSecondary))
            Box(
                Modifier.fillMaxWidth().height(ScreenSize.secondaryHeight),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    secondaryContent != null -> secondaryContent()
                    secondaryCaption != null -> Text(
                        secondaryCaption,
                        color = TextSecondary,
                        style = TypeRamp.caption,
                        textAlign = TextAlign.Center,
                    )
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
    headerRight: (@Composable () -> Unit)? = null,
    stepLabel: String? = null,
    currentStep: Int = 0,
    pagerPage: Int? = null,
    primaryText: String,
    onPrimary: () -> Unit,
    primaryGlyph: String? = null,
    secondaryContent: (@Composable () -> Unit)? = null,
    secondaryCaption: String? = null,
    centeredText: Boolean = false,
    illustration: @Composable BoxScope.(Dp) -> Unit,
    afterBody: @Composable ColumnScope.() -> Unit = {},
) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Ground),
    ) {
        val slotHeight = (LocalConfiguration.current.screenHeightDp.dp - ScreenSize.heroHeightOffset)
            .coerceIn(Space.heroMin, Space.heroMax)
        Column(
            Modifier
                .fillMaxSize()
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
            ) {
                Box(
                    Modifier.fillMaxWidth().height(slotHeight),
                    contentAlignment = Alignment.Center,
                    content = { illustration(slotHeight) },
                )
                Spacer(Modifier.height(Space.xl))
                if (title != null) {
                    Text(
                        title,
                        Modifier.fillMaxWidth(),
                        color = TextPrimary,
                        style = TypeRamp.title,
                        textAlign = if (centeredText) TextAlign.Center else TextAlign.Start,
                    )
                }
                if (body != null) {
                    Spacer(Modifier.height(Space.titleRows))
                    Text(
                        body,
                        Modifier.fillMaxWidth(),
                        color = TextSecondary,
                        style = TypeRamp.body,
                        textAlign = if (centeredText) TextAlign.Center else TextAlign.Start,
                    )
                }
                afterBody()
            }
            ScreenFooter(
                primaryText = primaryText,
                onPrimary = onPrimary,
                primaryGlyph = primaryGlyph,
                pagerPage = pagerPage,
                secondaryContent = secondaryContent,
                secondaryCaption = secondaryCaption,
            )
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
    secondaryCaption: String? = null,
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
            secondaryCaption = secondaryCaption,
        )
    }
}
