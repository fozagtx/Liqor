package com.liqor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
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
import com.liqor.ui.components.PermissionSheet
import com.liqor.ui.components.WaveBands
import com.liqor.ui.components.ScreenHeader
import com.liqor.ui.components.StatusChip
import com.liqor.ui.components.TextAction
import com.liqor.ui.components.TicketCard
import com.liqor.ui.components.cardBorder
import com.liqor.ui.components.rememberPermissionSheetState
import com.liqor.ui.theme.CardPadding
import com.liqor.ui.theme.Caution
import com.liqor.ui.theme.CautionTint
import com.liqor.ui.theme.DemoLayout
import com.liqor.ui.theme.Elevation
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.IllustrationSize
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.WaveBandsSize
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun SplashScreen() {
    Box(
        Modifier.fillMaxSize().background(Ground),
    ) {
        WaveBands(
            color = Raised,
            bandCount = 5,
            startFraction = 0.42f,
            thickness = WaveBandsSize.splashThickness,
            gap = WaveBandsSize.splashGap,
            bulge = WaveBandsSize.splashBulge,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(size = IllustrationSize.splashBrandMark)
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(start = Space.md),
                    color = TextPrimary,
                    style = TypeRamp.title,
                )
            }
        }
    }
}

@Composable
fun FirstLaunchScreen(sample: Watched, onConnect: () -> Unit) {
    HeroTemplate(
        title = stringResource(R.string.first_launch_headline),
        body = stringResource(R.string.first_launch_body),
        hasHeroCard = true,
        primaryText = stringResource(R.string.connect_wallet),
        onPrimary = onConnect,
        primaryGlyph = stringResource(R.string.wallet_glyph),
        illustration = {
            TicketCard(
                watched = sample,
                title = stringResource(R.string.ticket_distance_to_liquidation),
                animateSample = true,
                modifier = Modifier.fillMaxWidth(),
            )
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
        headerRight = { WalletAddressChip(wallet) },
        primaryText = stringResource(R.string.turn_on_bubble),
        onPrimary = onTurnOnBubble,
        illustration = {
            TicketCard(
                closest,
                stringResource(R.string.ticket_closest_to_liquidation),
                closest = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        afterBody = {
            PositionListSection(reading.watched.take(2), reading.watched.size)
        },
    )
}

@Composable
fun NoPositionsScreen(sample: Watched, onShowSample: () -> Unit) {
    HeroTemplate(
        title = stringResource(R.string.no_positions_headline),
        primaryText = stringResource(R.string.show_bubble_preview),
        onPrimary = onShowSample,
        illustration = {
            TicketCard(
                watched = sample,
                title = stringResource(R.string.ticket_distance_to_liquidation),
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
fun CouldNotCheckScreen(walletFailed: Boolean = false, onTryAgain: () -> Unit) {
    val shouldAnimate = !LocalReducedMotion.current && LocalAnimatedScope.current != null
    val refreshRotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    HeroTemplate(
        title = stringResource(
            if (walletFailed) R.string.wallet_failed_headline else R.string.could_not_check_headline,
        ),
        body = stringResource(
            if (walletFailed) R.string.wallet_failed_body else R.string.could_not_check_body,
        ),
        primaryText = stringResource(R.string.try_again),
        onPrimary = {
            if (shouldAnimate) {
                scope.launch {
                    refreshRotation.snapTo(0f)
                    refreshRotation.animateTo(
                        360f,
                        tween(Motion.RetrySpin, easing = LinearEasing),
                    )
                    onTryAgain()
                }
            } else {
                onTryAgain()
            }
        },
        illustration = {
            ErrorIllustration(
                image = if (walletFailed) R.drawable.illus_wallet else R.drawable.illus_unplugged,
                imageWidth = if (walletFailed) {
                    IllustrationSize.walletImageWidth
                } else {
                    IllustrationSize.unpluggedImageWidth
                },
                aspectRatio = if (walletFailed) 511f / 438f else 720f / 259f,
                refreshRotation = refreshRotation.value,
                badgeOffsetX = if (walletFailed) -Space.xl else Space.xs,
                badgeOffsetY = -(Space.xl + Space.md),
            )
        },
    )
}

@Composable
private fun ErrorIllustration(
    image: Int,
    imageWidth: androidx.compose.ui.unit.Dp,
    aspectRatio: Float,
    refreshRotation: Float,
    badgeOffsetX: androidx.compose.ui.unit.Dp,
    badgeOffsetY: androidx.compose.ui.unit.Dp,
) {
    Box(
        Modifier.size(IllustrationSize.errorArtFrame),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.width(imageWidth).aspectRatio(aspectRatio),
            contentScale = ContentScale.Fit,
        )
        RetryBadge(
            rotation = refreshRotation,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = badgeOffsetX, y = badgeOffsetY),
        )
    }
}

@Composable
private fun RetryBadge(rotation: Float, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(IllustrationSize.retryBadge)
            .clip(CircleShape)
            .background(CautionTint),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            Modifier
                .size(IllustrationSize.retryGlyph)
                .graphicsLayer(rotationZ = rotation),
        ) {
            val strokeWidth = Space.xs.toPx() * 0.625f
            val inset = strokeWidth / 2f
            drawArc(
                Caution,
                startAngle = 45f,
                sweepAngle = 285f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
            val angle = Math.toRadians(330.0)
            val radius = size.minDimension / 2f - inset
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val tip = Offset(
                centerX + kotlin.math.cos(angle).toFloat() * radius,
                centerY + kotlin.math.sin(angle).toFloat() * radius,
            )
            val directionX = -kotlin.math.sin(angle).toFloat()
            val directionY = kotlin.math.cos(angle).toFloat()
            val baseX = tip.x - directionX * Space.sm.toPx()
            val baseY = tip.y - directionY * Space.sm.toPx()
            val sideX = -directionY * Space.xs.toPx()
            val sideY = directionX * Space.xs.toPx()
            val arrowHead = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(baseX + sideX, baseY + sideY)
                lineTo(baseX - sideX, baseY - sideY)
                close()
            }
            drawPath(arrowHead, Caution)
        }
    }
}

@Composable
fun HomeScreen(
    reading: Reading,
    sample: Watched,
    sampleMode: Boolean,
    bubbleOn: Boolean,
    onBubbleChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onDisconnect: () -> Unit,
    overlaySheetVisible: Boolean = false,
    onOpenOverlaySettings: () -> Unit = {},
    onDismissOverlaySheet: () -> Unit = {},
) {
    val live = reading as? Reading.Live
    val watched = if (sampleMode) sample else live?.let(::closestPosition)
    val canRetry = !sampleMode && reading !is Reading.Live && reading != Reading.NoPositions
    Box(Modifier.fillMaxSize()) {
        FormTemplate(
            headerRight = { WalletAddressChip(stringResource(R.string.wallet_address)) },
            primaryText = if (canRetry) stringResource(R.string.try_again) else null,
            onPrimary = if (canRetry) onRetry else null,
            secondaryText = stringResource(R.string.disconnect),
            onSecondary = onDisconnect,
            secondaryPill = true,
        ) {
            if (watched != null) {
                TicketCard(
                    watched = watched,
                    title = stringResource(
                        if (sampleMode) R.string.ticket_distance_to_liquidation
                        else R.string.ticket_closest_to_liquidation,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Space.cardStack))
                PositionListSection(
                    watched = if (sampleMode) listOf(sample) else live?.orEmptyByDistance().orEmpty(),
                    totalCount = if (sampleMode) 1 else live?.watched?.size ?: 0,
                )
                Spacer(Modifier.height(Space.cardStack))
            } else {
                Text(
                    stringResource(
                        if (reading == Reading.NoPositions) R.string.panel_no_positions
                        else R.string.could_not_check_headline,
                    ),
                    color = TextSecondary,
                    style = TypeRamp.body,
                )
                Spacer(Modifier.height(Space.cardStack))
            }
            BubbleStatusCard(bubbleOn, onBubbleChange)
        }
        PermissionSheet(
            visible = overlaySheetVisible,
            icon = { BrandMark() },
            title = stringResource(R.string.overlay_sheet_title),
            body = stringResource(R.string.overlay_sheet_body),
            primaryText = stringResource(R.string.open_settings),
            primaryGlyph = stringResource(R.string.external_glyph),
            onPrimary = onOpenOverlaySettings,
            onDismiss = onDismissOverlaySheet,
        )
    }
}

private fun Reading.Live.orEmptyByDistance(): List<Watched> =
    watched.sortedBy { it.distance }

@Composable
private fun WalletAddressChip(wallet: String) {
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
}

@Composable
private fun PositionListSection(watched: List<Watched>, totalCount: Int) {
    Text(
        stringResource(R.string.positions_open_count, totalCount),
        color = TextSecondary,
        style = TypeRamp.label,
    )
    Spacer(Modifier.height(Space.sm))
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(Radius.md))
            .background(Raised)
            .cardBorder(RoundedCornerShape(Radius.md))
            .padding(horizontal = CardPadding.content),
    ) {
        watched.forEachIndexed { index, position ->
            if (index > 0) {
                androidx.compose.foundation.Canvas(
                    Modifier.fillMaxWidth().height(IllustrationSize.hairline),
                ) {
                    drawLine(
                        Hairline,
                        androidx.compose.ui.geometry.Offset.Zero,
                        androidx.compose.ui.geometry.Offset(size.width, 0f),
                        IllustrationSize.hairline.toPx(),
                    )
                }
            }
            PositionRow(position)
        }
    }
}

@Composable
private fun BubbleStatusCard(bubbleOn: Boolean, onBubbleChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(Radius.md)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Raised)
            .cardBorder(shape)
            .toggleable(bubbleOn, role = Role.Switch, onValueChange = onBubbleChange)
            .padding(CardPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = IllustrationSize.smallIcon)
        Column(Modifier.weight(1f).padding(start = Space.sm)) {
            Text(stringResource(R.string.bubble_title), color = TextPrimary, style = TypeRamp.row)
            Text(
                stringResource(if (bubbleOn) R.string.bubble_status_on else R.string.bubble_status_off),
                color = TextSecondary,
                style = TypeRamp.caption,
            )
        }
        PermissionSwitchTrack(if (bubbleOn) 1f else 0f)
    }
}

@Composable
fun OverlayPermissionScreen(
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit = {},
    onNotNow: () -> Unit,
    stillOff: Boolean = false,
    demoTimeMs: Long? = null,
    sheetInitiallyOpen: Boolean = false,
    sheetReopenKey: Int = 0,
) {
    val sheetState = rememberPermissionSheetState(sheetInitiallyOpen, sheetReopenKey)
    Box(Modifier.fillMaxSize()) {
        FormTemplate(
            stepLabel = stringResource(R.string.step_one_of_two),
            currentStep = 1,
            primaryText = stringResource(if (stillOff) R.string.try_again else R.string.open_settings),
            onPrimary = onOpenSettings,
            primaryGlyph = if (stillOff) null else stringResource(R.string.external_glyph),
            secondaryText = stringResource(if (stillOff) R.string.overlay_continue_without_bubble else R.string.not_now),
            onSecondary = onNotNow,
            secondaryPill = true,
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
                modifier = Modifier.fillMaxWidth().cardBorder(RoundedCornerShape(Radius.md)),
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
        PermissionSheet(
            visible = sheetState.visible,
            icon = { BrandMark() },
            title = stringResource(R.string.overlay_sheet_title),
            body = stringResource(R.string.overlay_sheet_body),
            primaryText = stringResource(R.string.open_settings),
            primaryGlyph = stringResource(R.string.external_glyph),
            onPrimary = onOpenSettings,
            onDismiss = sheetState.dismiss,
        )
    }
}

@Composable
fun BubbleOnScreen(onNext: () -> Unit) {
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    val shouldAnimate = !reducedMotion && animatedScope != null
    val ringProgress = remember { Animatable(if (shouldAnimate) 0f else 360f) }
    val checkProgress = remember { Animatable(if (shouldAnimate) 0f else 1f) }
    val haloProgress = remember { Animatable(if (shouldAnimate) 0f else 1f) }
    LaunchedEffect(shouldAnimate) {
        if (!shouldAnimate) {
            ringProgress.snapTo(360f)
            checkProgress.snapTo(1f)
            haloProgress.snapTo(1f)
        } else {
            ringProgress.snapTo(0f)
            checkProgress.snapTo(0f)
            haloProgress.snapTo(0f)
            ringProgress.animateTo(360f, tween(Motion.VerySlow, easing = Motion.SmoothOut))
            checkProgress.animateTo(1f, tween(Motion.BubbleCheckDraw, easing = Motion.SmoothOut))
            haloProgress.animateTo(1f, tween(Motion.Medium, easing = Motion.SmoothOut))
        }
    }

    HeroTemplate(
        title = stringResource(R.string.bubble_on_title),
        body = stringResource(R.string.bubble_on_body),
        stepLabel = stringResource(R.string.step_one_of_two),
        currentStep = 1,
        primaryText = stringResource(R.string.bubble_on_next),
        onPrimary = onNext,
        illustration = {
            Box(
                Modifier.size(IllustrationSize.bubbleOnGauge),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    if (checkProgress.value >= 1f && haloProgress.value < 1f) {
                        drawCircle(
                            Safe.copy(alpha = 0.3f * (1f - haloProgress.value)),
                            radius = size.minDimension * 0.43f * (1f + 0.15f * haloProgress.value),
                            center = center,
                            style = Stroke(Space.xs.toPx() / 2f),
                        )
                    }
                    drawArc(
                        Safe,
                        startAngle = -90f,
                        sweepAngle = ringProgress.value,
                        useCenter = false,
                        topLeft = Offset(Space.xs.toPx(), Space.xs.toPx()),
                        size = Size(
                            size.width - Space.sm.toPx(),
                            size.height - Space.sm.toPx(),
                        ),
                        style = Stroke(
                            Space.xs.toPx() + Space.xs.toPx() / 4f,
                            cap = StrokeCap.Round,
                        ),
                    )
                }
                Canvas(Modifier.size(IllustrationSize.featureIcon)) {
                    val start = Offset(size.width * 0.16f, size.height * 0.52f)
                    val corner = Offset(size.width * 0.42f, size.height * 0.76f)
                    val end = Offset(size.width * 0.84f, size.height * 0.24f)
                    val firstLength = (corner - start).getDistance()
                    val secondLength = (end - corner).getDistance()
                    val drawnLength =
                        (firstLength + secondLength) * checkProgress.value.coerceIn(0f, 1f)
                    val check = Path().apply {
                        moveTo(start.x, start.y)
                        if (drawnLength <= firstLength) {
                            val fraction = (drawnLength / firstLength).coerceIn(0f, 1f)
                            val point = start + (corner - start) * fraction
                            lineTo(point.x, point.y)
                        } else {
                            lineTo(corner.x, corner.y)
                            val fraction =
                                ((drawnLength - firstLength) / secondLength).coerceIn(0f, 1f)
                            val point = corner + (end - corner) * fraction
                            lineTo(point.x, point.y)
                        }
                    }
                    drawPath(
                        check,
                        Safe,
                        style = Stroke(
                            Space.xs.toPx() + Space.xs.toPx() / 4f,
                            cap = StrokeCap.Round,
                        ),
                    )
                }
            }
        },
    )
}

@Composable
fun NotificationPermissionScreen(
    sample: Watched,
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
    sheetInitiallyOpen: Boolean = false,
) {
    val sheetState = rememberPermissionSheetState(sheetInitiallyOpen)
    Box(Modifier.fillMaxSize()) {
        HeroTemplate(
            title = stringResource(R.string.notification_onboarding_title),
            body = stringResource(R.string.notification_onboarding_body),
            stepLabel = stringResource(R.string.step_two_of_two),
            currentStep = 2,
            primaryText = stringResource(R.string.allow_notifications),
            onPrimary = onAllow,
            hasHeroCard = true,
            secondaryText = stringResource(R.string.not_now),
            onSecondary = onNotNow,
            illustration = {
                NotificationPreview(sample, Modifier.fillMaxSize())
            },
        )
        PermissionSheet(
            visible = sheetState.visible,
            icon = {
                Image(
                    painter = painterResource(R.drawable.illus_bell),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Fit,
                )
            },
            title = stringResource(R.string.notification_sheet_title),
            body = stringResource(R.string.notification_sheet_body),
            primaryText = stringResource(R.string.permission_allow),
            onPrimary = onAllow,
            onDismiss = sheetState.dismiss,
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
        modifier = modifier.fillMaxWidth().cardBorder(RoundedCornerShape(Radius.md)),
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
        modifier = modifier.fillMaxWidth().cardBorder(RoundedCornerShape(Radius.md)),
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
        PermissionSwitchTrack(switchProgress)
    }
}

@Composable
private fun PermissionSwitchTrack(progress: Float) {
    val value = progress.coerceIn(0f, 1f)
    Box(
        Modifier
            .width(IllustrationSize.switchTrackWidth)
            .height(IllustrationSize.switchTrackHeight)
            .clip(CircleShape)
            .background(lerp(Hairline, TextPrimary, value)),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset(x = (Space.xs / 2 + IllustrationSize.switchThumbTravel * value))
                .size(IllustrationSize.switchThumb)
                .clip(CircleShape)
                .background(Raised),
        )
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
    val shouldAnimate = !LocalReducedMotion.current && LocalAnimatedScope.current != null
    val dropProgress = remember { Animatable(if (shouldAnimate) 0f else 1f) }
    val iconRotation = remember { Animatable(0f) }
    LaunchedEffect(shouldAnimate) {
        if (!shouldAnimate) {
            dropProgress.snapTo(1f)
            iconRotation.snapTo(0f)
        } else {
            dropProgress.snapTo(0f)
            iconRotation.snapTo(0f)
            dropProgress.animateTo(1f, tween(Motion.NotificationDrop, easing = Motion.SmoothOut))
            while (true) {
                val segmentDuration = Motion.NotificationSwing / 4
                iconRotation.animateTo(-12f, tween(segmentDuration, easing = Motion.SmoothOut))
                iconRotation.animateTo(10f, tween(segmentDuration, easing = Motion.SmoothOut))
                iconRotation.animateTo(-6f, tween(segmentDuration, easing = Motion.SmoothOut))
                iconRotation.animateTo(0f, tween(segmentDuration, easing = Motion.SmoothOut))
                delay((Motion.NotificationSwingRepeat - Motion.NotificationSwing).toLong())
            }
        }
    }
    val density = LocalDensity.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(
                    width = IllustrationSize.notificationPhoneWidth,
                    height = IllustrationSize.demoPhoneHeight - Space.xl,
                )
                .offset(y = Space.lg),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(Ground)
                    .cardBorder(RoundedCornerShape(Radius.sm)),
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
                Box(Modifier.fillMaxWidth().padding(top = CardPadding.content)) {
                    Surface(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationY = with(density) {
                                    (Space.notificationDrop * -(1f - dropProgress.value)).toPx()
                                }
                                alpha = dropProgress.value
                            }
                            .cardBorder(RoundedCornerShape(Radius.md)),
                        color = Raised,
                        shape = RoundedCornerShape(Radius.md),
                        shadowElevation = Elevation.card,
                    ) {
                        Row(
                            Modifier.padding(CardPadding.content)
                                .padding(end = Space.xxl + Space.sm),
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
                    Image(
                        painter = painterResource(R.drawable.illus_bell),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = Space.notificationBellOutX, y = -Space.notificationBellOutY)
                            .size(IllustrationSize.bellArt)
                            .graphicsLayer(rotationZ = iconRotation.value),
                        contentScale = ContentScale.Fit,
                    )
                }
                Spacer(Modifier.height(Space.md))
                repeat(3) { index ->
                    androidx.compose.foundation.Canvas(
                        Modifier.fillMaxWidth(if (index == 0) 0.88f else 0.62f)
                            .height(IllustrationSize.skeletonMain),
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
            )
        }
    }
}

private fun Level.stringId(): Int = when (this) {
    Level.Safe -> R.string.level_safe
    Level.Caution -> R.string.level_caution
    Level.Danger -> R.string.level_danger
}
