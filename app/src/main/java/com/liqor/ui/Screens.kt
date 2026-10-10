package com.liqor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
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
        StepHeader(stringResource(R.string.step_one_of_two), Modifier.stagger(0))
        Text(
            stringResource(R.string.overlay_title),
            modifier = Modifier.stagger(1),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.04).em,
            ),
        )
        Text(
            stringResource(R.string.overlay_body),
            modifier = Modifier.stagger(2),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        PermissionSettingsCard(Modifier.stagger(3))
        Row(
            modifier = Modifier
                .stagger(4)
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
            stringResource(R.string.open_settings),
            onClick = onOpenSettings,
            modifier = Modifier.stagger(5),
            leadingGlyph = stringResource(R.string.external_glyph),
        )
        TextAction(
            stringResource(R.string.open_app_info),
            onOpenAppInfo,
            Modifier.align(Alignment.CenterHorizontally).stagger(6),
        )
        TextAction(
            stringResource(R.string.not_now),
            onNotNow,
            Modifier.align(Alignment.CenterHorizontally).stagger(7),
        )
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
        StepHeader(stringResource(R.string.step_two_of_two), Modifier.stagger(0))
        Text(
            stringResource(R.string.notification_title),
            modifier = Modifier.stagger(1),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.04).em,
            ),
        )
        Text(
            stringResource(R.string.notification_body),
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
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(Ground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                position.asset.take(1),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            )
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(
                stringResource(R.string.position_name, position.asset, position.side),
                color = TextPrimary,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            )
            Text(
                stringResource(R.string.position_details, leverage, stringResource(watched.level.stringId())),
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
private fun StepHeader(step: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
}

@Composable
private fun PermissionSettingsCard(modifier: Modifier = Modifier) {
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
            PermissionRow(stringResource(R.string.app_name), stringResource(R.string.overlay_allowed), true)
            androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawLine(Hairline, androidx.compose.ui.geometry.Offset.Zero,
                    androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx())
            }
            PermissionRow(stringResource(R.string.overlay_any_other_app), "", false)
        }
    }
}

@Composable
private fun PermissionRow(name: String, status: String, checked: Boolean) {
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
                .background(if (checked) TextPrimary else Ground)
                .padding(2.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(Raised))
        }
    }
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
