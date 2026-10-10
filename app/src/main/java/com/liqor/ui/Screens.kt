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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
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
import com.liqor.ui.components.CoinLogo
import com.liqor.ui.components.LineIcon
import com.liqor.ui.components.PillButton
import com.liqor.ui.components.SampleChip
import com.liqor.ui.components.StatusChip
import com.liqor.ui.components.TextAction
import com.liqor.ui.components.TicketCard
import com.liqor.ui.theme.CautionTint
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Safe
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary
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
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(TextPrimary),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Canvas(Modifier.size(42.dp)) {
                    drawArc(
                        color = Raised,
                        startAngle = -30f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
            Text(
                stringResource(R.string.app_name),
                modifier = Modifier.padding(top = 12.dp).stagger(1),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.05).em,
                ),
            )
            Text(
                stringResource(R.string.splash_tagline),
                modifier = Modifier.padding(top = 4.dp).stagger(2),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            )
        }
        Text(
            stringResource(R.string.read_only_never_signs),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).stagger(3),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
        )
    }
}

@Composable
fun FirstLaunchScreen(sample: Watched, onConnect: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().stagger(0),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark()
            Text(
                stringResource(R.string.app_name),
                modifier = Modifier.padding(start = 7.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.weight(1f))
            SampleChip()
        }
        TicketCard(
            watched = sample,
            title = stringResource(R.string.ticket_distance_to_liquidation),
            modifier = Modifier.sharedTicket(),
        )
        Text(
            stringResource(R.string.first_launch_headline),
            modifier = Modifier.stagger(1),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.05).em,
                lineHeight = 27.sp,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureRow(0, stringResource(R.string.feature_bubble), Modifier.stagger(2))
            FeatureRow(1, stringResource(R.string.feature_nudge), Modifier.stagger(3))
            FeatureRow(2, stringResource(R.string.feature_read_only), Modifier.stagger(4))
        }
        Spacer(Modifier.weight(1f))
        PillButton(
            text = stringResource(R.string.connect_wallet),
            onClick = onConnect,
            modifier = Modifier.stagger(5),
            leadingGlyph = stringResource(R.string.wallet_glyph),
        )
    }
}

@Composable
fun PositionsFoundScreen(
    reading: Reading.Live,
    onTurnOnBubble: () -> Unit,
    wallet: String = stringResource(R.string.wallet_address),
) {
    val closest = reading.watched.minByOrNull { it.distance } ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().stagger(0),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark()
            Text(
                stringResource(R.string.app_name),
                modifier = Modifier.padding(start = 7.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.weight(1f))
            Text(
                wallet,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Raised)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            )
        }
        TicketCard(
            closest,
            stringResource(R.string.ticket_closest_to_liquidation),
            closest = true,
            modifier = Modifier.sharedTicket(),
        )
        Text(
            stringResource(R.string.positions_open_count, reading.watched.size),
            modifier = Modifier.stagger(1),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        )
        Column(
            modifier = Modifier
                .stagger(2)
                .clip(RoundedCornerShape(18.dp))
                .background(Raised)
                .padding(horizontal = 14.dp),
        ) {
            reading.watched.take(2).forEachIndexed { index, watched ->
                if (index > 0) androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                    drawLine(Hairline, androidx.compose.ui.geometry.Offset.Zero,
                        androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx())
                }
                PositionRow(watched)
            }
        }
        Spacer(Modifier.weight(1f))
        PillButton(
            text = stringResource(R.string.turn_on_bubble),
            onClick = onTurnOnBubble,
            modifier = Modifier.stagger(3),
        )
        Text(
            stringResource(R.string.permissions_caption),
            modifier = Modifier.fillMaxWidth().stagger(4),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun NoPositionsScreen(sample: Watched, onShowSample: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BrandHeader(Modifier.stagger(0))
        Text(
            stringResource(R.string.no_positions_headline),
            modifier = Modifier.stagger(1),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.04).em,
            ),
        )
        TicketCard(
            watched = sample,
            title = stringResource(R.string.ticket_distance_to_liquidation),
            isSample = true,
            modifier = Modifier.sharedTicket(),
        )
        Spacer(Modifier.weight(1f))
        PillButton(
            stringResource(R.string.show_sample_bubble),
            onClick = onShowSample,
            modifier = Modifier.stagger(2),
        )
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandHeader(Modifier.stagger(0))
        Text(
            stringResource(R.string.looking_for_positions),
            modifier = Modifier.padding(top = 24.dp).stagger(1),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun CouldNotCheckScreen(onTryAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BrandHeader(Modifier.stagger(0))
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.could_not_check_headline),
            modifier = Modifier.stagger(1),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.weight(1f))
        PillButton(
            stringResource(R.string.try_again),
            onTryAgain,
            modifier = Modifier.stagger(2),
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
    val actionIndex = when {
        watched != null -> 1
        reading == Reading.NoPositions -> 2
        else -> 3
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BrandHeader(Modifier.stagger(0))
        when {
            watched != null -> TicketCard(
                watched = watched,
                title = stringResource(
                    if (sampleMode) R.string.ticket_distance_to_liquidation
                    else R.string.ticket_closest_to_liquidation,
                ),
                isSample = sampleMode,
                modifier = Modifier.sharedTicket(),
            )
            reading == Reading.NoPositions -> Text(
                stringResource(R.string.panel_no_positions),
                modifier = Modifier.stagger(1),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> Text(
                stringResource(R.string.could_not_check_headline),
                modifier = Modifier.stagger(1),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.weight(1f))
        if (canRetry) {
            TextAction(
                stringResource(R.string.try_again),
                onRetry,
                Modifier.stagger(if (watched != null) 1 else 2),
            )
        }
        TextAction(
            stringResource(R.string.disconnect),
            onDisconnect,
            Modifier.stagger(actionIndex),
        )
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StepHeader(stringResource(R.string.step_one_of_two), 1, Modifier.stagger(0))
        Text(
            stringResource(if (stillOff) R.string.overlay_still_off_title else R.string.overlay_setup_title),
            modifier = Modifier.stagger(1),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.04).em,
            ),
        )
        if (stillOff) {
            Text(
                stringResource(R.string.overlay_still_off_body),
                modifier = Modifier.stagger(2),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OverlayStepsCard(Modifier.stagger(3))
        PermissionSettingsCard(
            Modifier.stagger(4),
            demoTimeMs,
        )
        Text(
            stringResource(R.string.overlay_settings_helper),
            modifier = Modifier.stagger(5),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        )
        Row(
            modifier = Modifier
                .stagger(6)
                .clip(RoundedCornerShape(14.dp))
                .background(Raised)
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                stringResource(R.string.info_glyph),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.overlay_restricted_tip),
                modifier = Modifier.padding(start = 8.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            )
        }
        Spacer(Modifier.weight(1f))
        PillButton(
            stringResource(if (stillOff) R.string.try_again else R.string.open_settings),
            onClick = onOpenSettings,
            modifier = Modifier.stagger(7),
            leadingGlyph = if (stillOff) null else stringResource(R.string.external_glyph),
        )
        TextAction(
            stringResource(R.string.open_app_info),
            onOpenAppInfo,
            Modifier.align(Alignment.CenterHorizontally).stagger(8),
        )
        TextAction(
            stringResource(if (stillOff) R.string.overlay_continue_without_bubble else R.string.not_now),
            onNotNow,
            Modifier.align(Alignment.CenterHorizontally).stagger(9),
        )
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StepHeader(stringResource(R.string.step_one_of_two), 1, Modifier.stagger(0))
        Spacer(Modifier.weight(1f))
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        Safe,
                        startAngle = -90f,
                        sweepAngle = sweep.value,
                        useCenter = false,
                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                        size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                        style = Stroke(5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Canvas(Modifier.size(34.dp)) {
                    val checkCorner = Offset(size.width * 0.42f, size.height * 0.76f)
                    drawLine(
                        Safe,
                        Offset(size.width * 0.16f, size.height * 0.52f),
                        checkCorner,
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        Safe,
                        checkCorner,
                        Offset(size.width * 0.84f, size.height * 0.24f),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
            Text(
                stringResource(R.string.bubble_on_title),
                modifier = Modifier.padding(top = 22.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.04).em,
                ),
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.bubble_on_body),
                modifier = Modifier.padding(top = 8.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        PillButton(stringResource(R.string.bubble_on_next), onNext, Modifier.stagger(1))
    }
}

@Composable
fun NotificationPermissionScreen(onAllow: () -> Unit, onNotNow: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        StepHeader(stringResource(R.string.step_two_of_two), 2, Modifier.stagger(0))
        Text(
            stringResource(R.string.notification_onboarding_title),
            modifier = Modifier.stagger(1),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.04).em,
            ),
        )
        Text(
            stringResource(R.string.notification_onboarding_body),
            modifier = Modifier.stagger(2),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        NotificationPreview(Modifier.stagger(3))
        Spacer(Modifier.weight(1f))
        PillButton(
            stringResource(R.string.allow_notifications),
            onAllow,
            modifier = Modifier.stagger(4),
        )
        TextAction(
            stringResource(R.string.not_now),
            onNotNow,
            Modifier.align(Alignment.CenterHorizontally).stagger(5),
        )
        Text(
            stringResource(R.string.notification_service_footnote),
            modifier = Modifier.fillMaxWidth().stagger(6),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FeatureRow(icon: Int, text: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Raised),
            contentAlignment = Alignment.Center,
        ) {
            LineIcon(icon)
        }
        Text(
            text,
            modifier = Modifier.padding(start = 10.dp),
            color = TextPrimary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
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
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinLogo(position.asset, 28.dp)
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(
                stringResource(R.string.position_name, position.asset, position.side),
                color = TextPrimary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                details,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            )
        }
        StatusChip(watched.level, label = "${distanceText(watched.distance)}%", stale = stale)
        Text(
            stringResource(R.string.chevron_glyph),
            modifier = Modifier.padding(start = 8.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun BrandHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Text(
            stringResource(R.string.app_name),
            modifier = Modifier.padding(start = 7.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun StepHeader(step: String, currentStep: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandHeader()
            Text(
                step,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(2) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (index < currentStep) TextPrimary else Hairline),
                )
            }
        }
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
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            steps.forEachIndexed { index, resource ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).background(TextPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${index + 1}",
                            color = Raised,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        )
                    }
                    Text(
                        stringResource(resource),
                        Modifier.weight(1f).padding(start = 10.dp),
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                    )
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
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                stringResource(R.string.overlay_what_you_see),
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            )
            Text(
                stringResource(R.string.overlay_display_over_apps),
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
                            .offset(x = (-42f + 37f * fingerProgress).dp)
                            .size(24.dp)
                            .graphicsLayer(alpha = fingerAlpha)
                            .clip(CircleShape)
                            .background(TextPrimary.copy(alpha = 0.25f))
                            .border(1.5.dp, TextPrimary.copy(alpha = 0.5f), CircleShape),
                    )
                }
            }
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(Hairline, androidx.compose.ui.geometry.Offset.Zero,
                    androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx())
            }
            PermissionRow(stringResource(R.string.overlay_any_other_app), "", 0f)
        }
    }
}

@Composable
private fun PermissionRow(name: String, status: String, switchProgress: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark()
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(name, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
            if (status.isNotEmpty()) {
                Text(status, color = TextSecondary, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp))
            }
        }
        Box(
            Modifier
                .width(34.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(lerp(Hairline, TextPrimary, switchProgress.coerceIn(0f, 1f))),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (2.dp + 14.dp * switchProgress.coerceIn(0f, 1f)))
                    .size(16.dp)
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
private fun NotificationPreview(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Raised,
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 4.dp,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Column(Modifier.padding(start = 10.dp)) {
                Text(
                    stringResource(R.string.notification_preview_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                )
                Text(
                    stringResource(R.string.notification_preview_body),
                    modifier = Modifier.padding(top = 4.dp),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
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
