package com.liqor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liqor.R
import com.liqor.domain.Freshness
import com.liqor.domain.Level
import com.liqor.domain.Reading
import com.liqor.domain.Watched
import com.liqor.ui.PositionRow
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Stale
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary

@Composable
fun Panel(
    reading: Reading,
    freshness: Freshness,
    ageSeconds: Long?,
    sample: Boolean,
    onOpenJupiter: () -> Unit,
    onMore: () -> Unit,
    expandedPosition: String? = null,
    onTogglePosition: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(344.dp),
        color = Raised,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 14.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            when (reading) {
                is Reading.Live -> LivePanelContent(
                    reading = reading,
                    stale = freshness == Freshness.Stale,
                    sample = sample,
                    expandedPosition = expandedPosition,
                    onTogglePosition = onTogglePosition,
                    onMore = onMore,
                )
                Reading.CouldNotCheck -> UnknownPanelContent()
                Reading.NoPositions -> Text(
                    stringResource(R.string.panel_no_positions),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(12.dp))
            PanelDivider()
            Spacer(Modifier.height(10.dp))
            PanelFooter(
                ageSeconds = ageSeconds,
                lastChecked = reading == Reading.CouldNotCheck,
                unknown = freshness == Freshness.Unknown || reading == Reading.CouldNotCheck,
                onOpenJupiter = onOpenJupiter,
            )
        }
    }
}

@Composable
private fun LivePanelContent(
    reading: Reading.Live,
    stale: Boolean,
    sample: Boolean,
    expandedPosition: String?,
    onTogglePosition: (String) -> Unit,
    onMore: () -> Unit,
) {
    val primary = reading.watched.firstOrNull() ?: return
    TicketCard(
        watched = primary,
        title = stringResource(R.string.ticket_closest_to_liquidation),
        isSample = sample,
        stale = stale,
        closest = true,
        panelContent = true,
        perforated = false,
    )
    reading.watched.drop(1).take(2).forEach { watched ->
        val expanded = expandedPosition == watched.position.address
        PanelDivider()
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PositionRow(
                watched = watched,
                modifier = Modifier
                    .clickable { onTogglePosition(watched.position.address) }
                    .padding(horizontal = 10.dp),
                stale = stale,
            )
            if (expanded) {
                com.liqor.ui.components.TrainLine(
                    entry = watched.position.entryPrice,
                    now = watched.position.oraclePrice,
                    liq = watched.position.liquidationPrice,
                    travelled = watched.travelled.toFloat(),
                    level = watched.level,
                    stale = stale,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
        }
    }
    val more = (reading.watched.size - 3).coerceAtLeast(0)
    if (more > 0) {
        PanelDivider()
        Text(
            text = stringResource(R.string.more_positions, more),
            modifier = Modifier
                .clickable(onClick = onMore)
                .padding(horizontal = 8.dp, vertical = 12.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun UnknownPanelContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            stringResource(R.string.unknown_value),
            color = Stale,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
        )
        Text(
            stringResource(R.string.panel_unknown_copy),
            modifier = Modifier.padding(top = 4.dp),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        )
    }
}

@Composable
private fun PanelDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Hairline),
    )
}

@Composable
private fun PanelFooter(
    ageSeconds: Long?,
    lastChecked: Boolean,
    unknown: Boolean,
    onOpenJupiter: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (ageSeconds != null) {
            Text(
                stringResource(R.string.clock_glyph),
                color = if (unknown) Stale else TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            val ageText = when {
                lastChecked && ageSeconds < 60 ->
                    stringResource(R.string.panel_last_checked_seconds, ageSeconds)
                lastChecked ->
                    stringResource(R.string.panel_last_checked_minutes, ageSeconds / 60)
                ageSeconds < 60 -> stringResource(R.string.panel_updated_seconds, ageSeconds)
                else -> stringResource(R.string.panel_updated_minutes, ageSeconds / 60)
            }
            Text(
                ageText,
                modifier = Modifier.padding(start = 4.dp).weight(1f),
                color = if (unknown) Stale else TextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        Surface(
            onClick = onOpenJupiter,
            shape = RoundedCornerShape(50),
            color = TextPrimary,
            contentColor = Raised,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.open_jupiter),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.external_glyph), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
