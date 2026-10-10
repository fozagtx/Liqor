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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun SplashScreen() {
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
                        sweepAngle = 300f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
            Text(
                stringResource(R.string.app_name),
                modifier = Modifier.padding(top = 12.dp),
                color = TextPrimary,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.05).em,
                ),
            )
            Text(
                stringResource(R.string.splash_tagline),
                modifier = Modifier.padding(top = 4.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            )
        }
        Text(
            stringResource(R.string.read_only_never_signs),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
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
            modifier = Modifier.fillMaxWidth(),
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
        )
        Text(
            stringResource(R.string.first_launch_headline),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.05).em,
                lineHeight = 27.sp,
            ),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureRow(0, stringResource(R.string.feature_bubble))
            FeatureRow(1, stringResource(R.string.feature_nudge))
            FeatureRow(2, stringResource(R.string.feature_read_only))
        }
        Spacer(Modifier.weight(1f))
        PillButton(
            text = stringResource(R.string.connect_wallet),
            onClick = onConnect,
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
            modifier = Modifier.fillMaxWidth(),
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
        TicketCard(closest, stringResource(R.string.ticket_closest_to_liquidation), closest = true)
        Text(
            stringResource(R.string.positions_open_count, reading.watched.size),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        )
        Column(
            modifier = Modifier
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
        )
        Text(
            stringResource(R.string.permissions_caption),
            modifier = Modifier.fillMaxWidth(),
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
        BrandHeader()
        Text(
            stringResource(R.string.no_positions_headline),
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
        )
        Spacer(Modifier.weight(1f))
        PillButton(
            stringResource(R.string.show_sample_bubble),
            onClick = onShowSample,
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
        BrandHeader()
        Text(
            stringResource(R.string.looking_for_positions),
            modifier = Modifier.padding(top = 24.dp),
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
        BrandHeader()
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.could_not_check_headline),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )
        Spacer(Modifier.weight(1f))
        PillButton(stringResource(R.string.try_again), onTryAgain)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BrandHeader()
        when {
            watched != null -> TicketCard(
                watched = watched,
                title = stringResource(
                    if (sampleMode) R.string.ticket_distance_to_liquidation
                    else R.string.ticket_closest_to_liquidation,
                ),
                isSample = sampleMode,
            )
            reading == Reading.NoPositions -> Text(
                stringResource(R.string.panel_no_positions),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> Text(
                stringResource(R.string.could_not_check_headline),
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.weight(1f))
        if (!sampleMode && reading !is Reading.Live && reading != Reading.NoPositions) {
            TextAction(stringResource(R.string.try_again), onRetry)
        }
        TextAction(stringResource(R.string.disconnect), onDisconnect)
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
        StepHeader(stringResource(R.string.step_one_of_two))
        Text(
            stringResource(R.string.overlay_title),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.04).em,
            ),
        )
        Text(
            stringResource(R.string.overlay_body),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        PermissionSettingsCard()
        Row(
            modifier = Modifier
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
            leadingGlyph = stringResource(R.string.external_glyph),
        )
        TextAction(
            stringResource(R.string.open_app_info),
            onOpenAppInfo,
            Modifier.align(Alignment.CenterHorizontally),
        )
        TextAction(stringResource(R.string.not_now), onNotNow, Modifier.align(Alignment.CenterHorizontally))
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
        StepHeader(stringResource(R.string.step_two_of_two))
        Text(
            stringResource(R.string.notification_title),
            color = TextPrimary,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.04).em,
            ),
        )
        Text(
            stringResource(R.string.notification_body),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        NotificationPreview()
        Spacer(Modifier.weight(1f))
        PillButton(stringResource(R.string.allow_notifications), onAllow)
        TextAction(stringResource(R.string.not_now), onNotNow, Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun FeatureRow(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun BrandHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BrandMark()
        Text(
            stringResource(R.string.app_name),
            modifier = Modifier.padding(start = 7.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun StepHeader(step: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
private fun PermissionSettingsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
private fun NotificationPreview() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
