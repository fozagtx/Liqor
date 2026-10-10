package com.liqor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.liqor.R
import com.liqor.domain.Level
import com.liqor.domain.Reading
import com.liqor.domain.Watched
import com.liqor.domain.distanceText
import com.liqor.ui.components.BrandMark
import com.liqor.ui.components.Bubble
import com.liqor.ui.components.CoinLogo
import com.liqor.ui.components.FormTemplate
import com.liqor.ui.components.HeroTemplate
import com.liqor.ui.components.LineIcon
import com.liqor.ui.components.SampleChip
import com.liqor.ui.components.ScreenHeader
import com.liqor.ui.components.StatusChip
import com.liqor.ui.components.TextAction
import com.liqor.ui.components.TicketCard
import com.liqor.ui.theme.CardPadding
import com.liqor.ui.theme.CautionTint
import com.liqor.ui.theme.DemoLayout
import com.liqor.ui.theme.Elevation
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.IllustrationSize
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.Radius
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.ScreenSize
import com.liqor.ui.theme.Space
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary
import com.liqor.ui.theme.TypeRamp
import kotlin.math.roundToInt

@Composable
fun SplashScreen() {
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val sweep = remember {
        Animatable(if (reducedMotion || animatedScope == null) 300f else 0f)
    }
    LaunchedEffect(reducedMotion, animatedScope) {
        if (reducedMotion || animatedScope == null) {
            sweep.snapTo(300f)
        } else {
            sweep.snapTo(0f)
            sweep.animateTo(300f, tween(Motion.VerySlow, easing = Motion.SmoothOut))
        }
    }
    val sweepAngle = if (reducedMotion || animatedScope == null) 300f else sweep.value
    Box(
        Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(IllustrationSize.splashMark)
                    .clip(RoundedCornerShape(Radius.md))
                    .background(TextPrimary),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(IllustrationSize.splashGauge)) {
                    drawArc(
                        color = Raised,
                        startAngle = -30f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(Space.xs.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
            Text(
                stringResource(R.string.app_name),
                modifier = Modifier.padding(top = Space.md).stagger(1),
                color = TextPrimary,
                style = TypeRamp.title,
            )
            Text(
                stringResource(R.string.splash_tagline),
                modifier = Modifier.padding(top = Space.xs).stagger(2),
                color = TextSecondary,
                style = TypeRamp.body,
            )
        }
        Text(
            stringResource(R.string.read_only_never_signs),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = Space.screenBottom).stagger(3),
            color = TextSecondary,
            style = TypeRamp.caption,
        )
    }
}

@Composable
fun FirstLaunchScreen(sample: Watched, onConnect: () -> Unit) {
    HeroTemplate(
        title = stringResource(R.string.first_launch_headline),
        headerRight = { SampleChip() },
        primaryText = stringResource(R.string.connect_wallet),
        onPrimary = onConnect,
        primaryGlyph = stringResource(R.string.wallet_glyph),
        illustration = {
            TicketCard(
                watched = sample,
                title = stringResource(R.string.ticket_distance_to_liquidation),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        afterBody = {
            Spacer(Modifier.height(Space.lg))
            FeatureRow(0, stringResource(R.string.feature_bubble))
            Spacer(Modifier.height(Space.sm))
            FeatureRow(1, stringResource(R.string.feature_nudge))
            Spacer(Modifier.height(Space.sm))
            FeatureRow(2, stringResource(R.string.feature_read_only))
        },
    )
}

@Composable
fun PositionsFoundScreen(
    reading: Reading.Live,
    onTurnOnBubble: () -> Unit,
    wallet: String = stringResource(R.string.wallet_address),
) {
    val closest = reading.watched.minByOrNull { it.distance } ?: return
    HeroTemplate(
        title = null,
        headerRight = {
            Box(
                Modifier
                    .clip(RoundedCornerShape(Radius.pill))
                    .background(Raised)
                    .height(IllustrationSize.walletChipHeight)
                    .padding(horizontal = Space.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(wallet, color = TextPrimary, style = TypeRamp.label)
            }
        },
        primaryText = stringResource(R.string.turn_on_bubble),
        onPrimary = onTurnOnBubble,
        secondaryCaption = stringResource(R.string.permissions_caption),
        illustration = {
            TicketCard(
                closest,
                stringResource(R.string.ticket_closest_to_liquidation),
                closest = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        afterBody = {
            Text(
                stringResource(R.string.positions_open_count, reading.watched.size),
                color = TextSecondary,
                style = TypeRamp.label,
            )
            Spacer(Modifier.height(Space.sm))
            Column(
                modifier = Modifier.clip(RoundedCornerShape(Radius.md))
                    .background(Raised)
                    .padding(horizontal = CardPadding.content),
            ) {
                reading.watched.take(2).forEachIndexed { index, watched ->
                    if (index > 0) androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(IllustrationSize.hairline)) {
                        drawLine(Hairline, androidx.compose.ui.geometry.Offset.Zero,
                            androidx.compose.ui.geometry.Offset(size.width, 0f), IllustrationSize.hairline.toPx())
                    }
                    PositionRow(watched)
                }
            }
        },
    )
}

@Composable
fun NoPositionsScreen(sample: Watched, onShowSample: () -> Unit) {
    HeroTemplate(
        title = stringResource(R.string.no_positions_headline),
        primaryText = stringResource(R.string.show_sample_bubble),
        onPrimary = onShowSample,
        illustration = {
            TicketCard(
                watched = sample,
                title = stringResource(R.string.ticket_distance_to_liquidation),
                isSample = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
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
        ScreenHeader()
        Box(
            Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.looking_for_positions),
                modifier = Modifier.stagger(1),
                color = TextSecondary,
                style = TypeRamp.body,
            )
        }
    }
}

@Composable
fun CouldNotCheckScreen(onTryAgain: () -> Unit) {
    FormTemplate(
        primaryText = stringResource(R.string.try_again),
        onPrimary = onTryAgain,
    ) {
        Text(
            stringResource(R.string.could_not_check_headline),
            color = TextPrimary,
            style = TypeRamp.title,
        )
        Spacer(Modifier.height(Space.md))
        Text(
            stringResource(R.string.could_not_check_body),
            color = TextSecondary,
            style = TypeRamp.body,
        )
    }
}

@Composable
fun HomeScreen(
    reading: Reading,
    sample: Watched,
    sampleMode: Boolean,
    onRetry: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val live = reading as? Reading.Live
    val watched = if (sampleMode) sample else live?.let(::closestPosition)
    val canRetry = !sampleMode && reading !is Reading.Live && reading != Reading.NoPositions
    FormTemplate(
        primaryText = if (canRetry) stringResource(R.string.try_again) else null,
        onPrimary = if (canRetry) onRetry else null,
        secondaryContent = { TextAction(stringResource(R.string.disconnect), onDisconnect) },
    ) {
        when {
            watched != null -> TicketCard(
                watched = watched,
                title = stringResource(
                    if (sampleMode) R.string.ticket_distance_to_liquidation
                    else R.string.ticket_closest_to_liquidation,
                ),
                isSample = sampleMode,
                modifier = Modifier.fillMaxWidth(),
            )
            reading == Reading.NoPositions -> Text(
                stringResource(R.string.panel_no_positions),
                color = TextSecondary,
                style = TypeRamp.body,
            )
            else -> Text(
                stringResource(R.string.could_not_check_headline),
                color = TextSecondary,
                style = TypeRamp.body,
            )
        }
    }
}

@Composable
fun OverlayPermissionScreen(
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onNotNow: () -> Unit,
    stillOff: Boolean = false,
    demoTimeMs: Long? = null,
) {
    FormTemplate(
        stepLabel = stringResource(R.string.step_one_of_two),
        currentStep = 1,
        primaryText = stringResource(if (stillOff) R.string.try_again else R.string.open_settings),
        onPrimary = onOpenSettings,
        primaryGlyph = if (stillOff) null else stringResource(R.string.external_glyph),
        secondaryContent = {
            TextAction(
                stringResource(if (stillOff) R.string.overlay_continue_without_bubble else R.string.not_now),
                onNotNow,
            )
        },
    ) {
        Text(
            stringResource(if (stillOff) R.string.overlay_still_off_title else R.string.overlay_setup_title),
            color = TextPrimary,
            style = TypeRamp.title,
        )
        Spacer(Modifier.height(Space.xl))
        if (stillOff) {
            Text(
                stringResource(R.string.overlay_still_off_body),
                color = TextSecondary,
                style = TypeRamp.body,
            )
            Spacer(Modifier.height(Space.xl))
        }
        OverlayStepsCard()
        if (!stillOff) {
            Spacer(Modifier.height(Space.cardStack))
            PermissionSettingsCard(Modifier.fillMaxWidth(), demoTimeMs)
        }
        Spacer(Modifier.height(Space.cardStack))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Raised,
            shape = RoundedCornerShape(Radius.md),
            shadowElevation = Elevation.flat,
        ) {
            Column(Modifier.padding(CardPadding.content)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        stringResource(R.string.info_glyph),
                        color = TextSecondary,
                        style = TypeRamp.row,
                    )
                    Spacer(Modifier.width(Space.sm))
                    Text(
                        stringResource(R.string.overlay_restricted_tip),
                        Modifier.weight(1f),
                        color = TextSecondary,
                        style = TypeRamp.caption,
                    )
                }
                Spacer(Modifier.height(Space.xs))
                TextAction(
                    stringResource(R.string.open_app_info),
                    onOpenAppInfo,
                    Modifier.align(Alignment.End),
                )
            }
        }
    }
}

@Composable
fun BubbleOnScreen(onNext: () -> Unit) {
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val shouldAnimate = !reducedMotion && animatedScope != null
    val sweep = remember { Animatable(if (shouldAnimate) 0f else 360f) }
    LaunchedEffect(shouldAnimate) {
        if (!shouldAnimate) {
            sweep.snapTo(360f)
        } else {
            sweep.snapTo(0f)
            sweep.animateTo(360f, tween(Motion.VerySlow, easing = Motion.SmoothOut))
        }
    }

    HeroTemplate(
        title = stringResource(R.string.bubble_on_title),
        body = stringResource(R.string.bubble_on_body),
        stepLabel = stringResource(R.string.step_one_of_two),
        currentStep = 1,
        primaryText = stringResource(R.string.bubble_on_next),
        onPrimary = onNext,
        centeredText = true,
        illustration = {
            Box(Modifier.size(IllustrationSize.bubbleOnGauge), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        Safe,
                        startAngle = -90f,
                        sweepAngle = sweep.value,
                        useCenter = false,
                        topLeft = Offset(Space.xs.toPx(), Space.xs.toPx()),
                        size = Size(size.width - Space.sm.toPx(), size.height - Space.sm.toPx()),
                        style = Stroke(Space.xs.toPx() + Space.xs.toPx() / 4, cap = StrokeCap.Round),
                    )
                }
                Canvas(Modifier.size(IllustrationSize.featureIcon)) {
                    val checkCorner = Offset(size.width * 0.42f, size.height * 0.76f)
                    drawLine(
                        Safe,
                        Offset(size.width * 0.16f, size.height * 0.52f),
                        checkCorner,
                        strokeWidth = Space.xs.toPx() + Space.xs.toPx() / 4,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        Safe,
                        checkCorner,
                        Offset(size.width * 0.84f, size.height * 0.24f),
                        strokeWidth = Space.xs.toPx() + Space.xs.toPx() / 4,
                        cap = StrokeCap.Round,
                    )
                }
            }
        },
    )
}

@Composable
fun NotificationPermissionScreen(sample: Watched, onAllow: () -> Unit, onNotNow: () -> Unit) {
    HeroTemplate(
        title = stringResource(R.string.notification_onboarding_title),
        body = stringResource(R.string.notification_onboarding_body),
        stepLabel = stringResource(R.string.step_two_of_two),
        currentStep = 2,
        primaryText = stringResource(R.string.allow_notifications),
        onPrimary = onAllow,
        secondaryContent = {
            TextAction(stringResource(R.string.not_now), onNotNow)
        },
        illustration = {
            NotificationPreview(sample, Modifier.fillMaxSize())
        },
        afterBody = {
            Spacer(Modifier.height(Space.lg))
            Text(
                stringResource(R.string.notification_service_footnote),
                color = TextSecondary,
                style = TypeRamp.caption,
                textAlign = TextAlign.Center,
            )
        },
    )
}

@Composable
private fun FeatureRow(icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(IllustrationSize.featureIcon)
                .clip(CircleShape)
                .background(Raised),
            contentAlignment = Alignment.Center,
        ) {
            LineIcon(icon)
        }
        Text(
            text,
            modifier = Modifier.padding(start = Space.sm),
            color = TextPrimary,
            style = TypeRamp.row,
        )
    }
}

@Composable
fun PositionRow(watched: Watched, modifier: Modifier = Modifier, stale: Boolean = false) {
    val position = watched.position
    val leverage = (position.sizeUsd / position.collateralUsd).roundToInt().coerceAtLeast(1)
    val level = stringResource(watched.level.stringId())
    val details = if (position.protocol == "jupiter-beta") {
        stringResource(R.string.position_details_beta, leverage, level)
    } else {
        stringResource(R.string.position_details, leverage, level)
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = Space.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinLogo(position.asset, IllustrationSize.rowLogo)
        Column(Modifier.weight(1f).padding(start = Space.sm)) {
            Text(
                stringResource(R.string.position_name, position.asset, position.side),
                color = TextPrimary,
                style = TypeRamp.bodyStrong,
            )
            Text(
                details,
                color = TextSecondary,
                style = TypeRamp.caption,
            )
        }
        StatusChip(watched.level, label = "${distanceText(watched.distance)}%", stale = stale)
        Text(
            stringResource(R.string.chevron_glyph),
            modifier = Modifier.padding(start = Space.sm),
            color = TextSecondary,
            style = TypeRamp.action,
        )
    }
}

@Composable
private fun OverlayStepsCard(modifier: Modifier = Modifier) {
    val steps = listOf(
        R.string.overlay_step_one,
        R.string.overlay_step_two,
        R.string.overlay_step_three,
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Raised,
        shape = RoundedCornerShape(Radius.md),
        shadowElevation = Elevation.card,
    ) {
        Column(Modifier.padding(CardPadding.content)) {
            steps.forEachIndexed { index, resource ->
                Column {
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = IllustrationSize.stepRowHeight),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(IllustrationSize.stepNumberLarge).clip(CircleShape).background(TextPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", color = Raised, style = TypeRamp.label)
                        }
                        Spacer(Modifier.width(Space.lg))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(resource),
                                color = TextPrimary,
                                style = TypeRamp.row,
                            )
                            if (index == 1) {
                                Spacer(Modifier.height(Space.xs))
                                Text(
                                    stringResource(R.string.overlay_settings_helper),
                                    color = TextSecondary,
                                    style = TypeRamp.caption,
                                )
                            }
                        }
                    }
                    if (index == 1) Spacer(Modifier.height(Space.md))
                    if (index < steps.lastIndex) {
                        androidx.compose.foundation.Canvas(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = IllustrationSize.stepNumberLarge + Space.lg)
                                .height(IllustrationSize.hairline),
                        ) {
                            drawLine(
                                Hairline,
                                androidx.compose.ui.geometry.Offset.Zero,
                                androidx.compose.ui.geometry.Offset(size.width, 0f),
                                IllustrationSize.hairline.toPx(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSettingsCard(
    modifier: Modifier = Modifier,
    demoTimeMs: Long? = null,
) {
    val reducedMotion = LocalReducedMotion.current
    val time = when {
        reducedMotion -> 1_600f
        demoTimeMs != null -> ((demoTimeMs % 1_600L + 1_600L) % 1_600L).toFloat()
        else -> {
            val transition = rememberInfiniteTransition(label = "overlay-switch")
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1_600f,
                animationSpec = infiniteRepeatable(tween(1_600, easing = LinearEasing)),
                label = "overlay-switch-time",
            ).value
        }
    }
    val switchProgress = if (reducedMotion) 1f else permissionSwitchProgress(time)
    val fingerProgress = permissionFingerProgress(time)
    val fingerAlpha = if (reducedMotion) 0f else permissionFingerAlpha(time)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Raised,
        shape = RoundedCornerShape(Radius.md),
        shadowElevation = Elevation.card,
    ) {
        Column(Modifier.padding(CardPadding.content)) {
            Text(
                stringResource(R.string.overlay_what_you_see),
                color = TextSecondary,
                style = TypeRamp.label,
            )
            Text(
                stringResource(R.string.overlay_display_over_apps),
                modifier = Modifier.padding(top = Space.sm, bottom = Space.md),
                color = TextPrimary,
                style = TypeRamp.bodyStrong,
            )
            Box(Modifier.fillMaxWidth()) {
                PermissionRow(
                    stringResource(R.string.app_name),
                    stringResource(R.string.overlay_allowed),
                    switchProgress,
                )
                if (fingerAlpha > 0f) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .offset(
                                x = (
                                    -DemoLayout.settingFingerStartOffset.value +
                                        DemoLayout.settingFingerTravel.value * fingerProgress
                                    ).dp,
                            )
                            .size(IllustrationSize.demoFinger)
                            .graphicsLayer(alpha = fingerAlpha)
                            .clip(CircleShape)
                            .background(TextPrimary.copy(alpha = 0.25f))
                            .border(Space.xs * 0.375f, TextPrimary.copy(alpha = 0.5f), CircleShape),
                    )
                }
            }
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(IllustrationSize.hairline)) {
                drawLine(Hairline, androidx.compose.ui.geometry.Offset.Zero,
                    androidx.compose.ui.geometry.Offset(size.width, 0f), IllustrationSize.hairline.toPx())
            }
            PermissionRow(stringResource(R.string.overlay_any_other_app), "", 0f)
        }
    }
}

@Composable
private fun PermissionRow(name: String, status: String, switchProgress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.rowVertical),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Column(Modifier.weight(1f).padding(start = Space.sm)) {
            Text(name, color = TextPrimary, style = TypeRamp.row)
            if (status.isNotEmpty()) {
                Text(status, color = TextSecondary, style = TypeRamp.caption)
            }
        }
        Box(
            Modifier
                .width(IllustrationSize.switchTrackWidth)
                .height(IllustrationSize.switchTrackHeight)
                .clip(CircleShape)
                .background(lerp(Hairline, TextPrimary, switchProgress.coerceIn(0f, 1f))),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (Space.xs / 2 + IllustrationSize.switchThumbTravel * switchProgress.coerceIn(0f, 1f)))
                    .size(IllustrationSize.switchThumb)
                    .clip(CircleShape)
                    .background(Raised),
            )
        }
    }
}

private fun permissionSwitchProgress(time: Float): Float = when {
    time < 600f -> 0f
    time < 900f -> smoothStep((time - 600f) / 300f)
    time < 1_250f -> 1f
    time < 1_550f -> 1f - smoothStep((time - 1_250f) / 300f)
    else -> 0f
}

private fun permissionFingerProgress(time: Float): Float = when {
    time < 200f -> 0f
    time < 600f -> smoothStep((time - 200f) / 400f)
    time < 1_300f -> 1f
    time < 1_550f -> 1f - smoothStep((time - 1_300f) / 250f)
    else -> 0f
}

private fun permissionFingerAlpha(time: Float): Float = when {
    time < 200f -> 0f
    time < 450f -> smoothStep((time - 200f) / 250f)
    time < 900f -> 1f
    time < 1_100f -> 1f - smoothStep((time - 900f) / 200f)
    else -> 0f
}

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

@Composable
private fun NotificationPreview(sample: Watched, modifier: Modifier = Modifier) {
    Surface(
        modifier.fillMaxSize(),
        color = Raised,
        shape = RoundedCornerShape(Radius.lg),
        shadowElevation = Elevation.card,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SampleChip(Modifier.align(Alignment.TopEnd).padding(CardPadding.content))
            Box(
                Modifier
                    .size(width = IllustrationSize.phoneWidth, height = IllustrationSize.demoPhoneHeight),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(Radius.sm))
                        .background(Ground)
                        .border(IllustrationSize.hairline, Hairline, RoundedCornerShape(Radius.sm)),
                )
                Column(
                    Modifier.fillMaxSize().padding(
                        horizontal = CardPadding.inner,
                        vertical = CardPadding.content,
                    ),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark()
                        Text(
                            stringResource(R.string.app_name),
                            Modifier.padding(start = Space.sm),
                            color = TextPrimary,
                            style = TypeRamp.bodyStrong,
                        )
                    }
                    Surface(
                        Modifier.fillMaxWidth().padding(top = CardPadding.content),
                        color = Raised,
                        shape = RoundedCornerShape(Radius.md),
                        shadowElevation = Elevation.card,
                    ) {
                        Row(
                            Modifier.padding(CardPadding.content),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BrandMark()
                            Column(Modifier.padding(start = Space.sm)) {
                                Text(
                                    stringResource(R.string.notification_preview_title),
                                    color = TextPrimary,
                                    style = TypeRamp.bodyStrong,
                                )
                                Text(
                                    stringResource(R.string.notification_preview_body),
                                    modifier = Modifier.padding(top = Space.xs),
                                    color = TextSecondary,
                                    style = TypeRamp.caption,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(Space.md))
                    repeat(3) { index ->
                        androidx.compose.foundation.Canvas(
                            Modifier.fillMaxWidth(if (index == 0) 0.88f else 0.62f).height(IllustrationSize.skeletonMain),
                        ) {
                            drawRoundRect(Hairline, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
                        }
                        Spacer(Modifier.height(Space.sm))
                    }
                }
                Bubble(
                    sampleBubbleUi(sample),
                    Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = Space.xl)
                        .graphicsLayer(scaleX = 0.8f, scaleY = 0.8f),
                    showSampleTag = false,
                )
            }
        }
    }
}

private fun Level.stringId(): Int = when (this) {
    Level.Safe -> R.string.level_safe
    Level.Caution -> R.string.level_caution
    Level.Danger -> R.string.level_danger
}
