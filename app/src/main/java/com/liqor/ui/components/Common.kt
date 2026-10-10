package com.liqor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liqor.R
import com.liqor.domain.Level
import com.liqor.ui.theme.CautionText
import com.liqor.ui.theme.CautionTint
import com.liqor.ui.theme.DangerText
import com.liqor.ui.theme.DangerTint
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.SafeText
import com.liqor.ui.theme.SafeTint
import com.liqor.ui.theme.StaleText
import com.liqor.ui.theme.StaleTint
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.CardPadding
import com.liqor.ui.theme.ScreenSize
import com.liqor.ui.theme.IllustrationSize
import com.liqor.ui.theme.Radius
import com.liqor.ui.theme.Space
import com.liqor.ui.theme.TypeRamp

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = ScreenSize.primaryButtonHeight,
    leadingGlyph: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        shape = RoundedCornerShape(Radius.pill),
        color = TextPrimary,
        contentColor = Raised,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = CardPadding.hero),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingGlyph != null) {
                Text(
                    leadingGlyph,
                    style = TypeRamp.action,
                )
                Spacer(Modifier.size(Space.sm))
            }
            Text(
                text,
                style = TypeRamp.button,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.lg, vertical = Space.md),
        color = TextPrimary,
        style = TypeRamp.action,
    )
}

@Composable
fun StatusChip(
    level: Level,
    modifier: Modifier = Modifier,
    label: String? = null,
    stale: Boolean = false,
) {
    val (background, foreground, resource) = if (stale) {
        Triple(StaleTint, StaleText, R.string.stale_label)
    } else {
        when (level) {
            Level.Safe -> Triple(SafeTint, SafeText, R.string.level_safe)
            Level.Caution -> Triple(CautionTint, CautionText, R.string.level_caution)
            Level.Danger -> Triple(DangerTint, DangerText, R.string.level_danger)
        }
    }
    Text(
        text = label ?: stringResource(resource),
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = Space.sm, vertical = Space.xs),
        color = foreground,
        style = TypeRamp.label,
    )
}

@Composable
fun SampleChip(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.sample_tag),
        modifier = modifier
            .clip(CircleShape)
            .background(TextPrimary)
            .padding(horizontal = Space.sm, vertical = Space.chipVertical),
        color = Raised,
        style = TypeRamp.label,
    )
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(IllustrationSize.brandMark)
            .clip(CircleShape)
            .background(TextPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(Space.lg.minus(Space.xs))) {
            drawArc(
                Color.White,
                -30f,
                300f,
                false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(Space.xs.toPx() / 2, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun LineIcon(kind: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.size(IllustrationSize.smallIcon)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(Space.xs.toPx() * 0.375f, cap = StrokeCap.Round)
        val c = Color(0xFF6B6862)
        val w = size.width
        val h = size.height
        when (kind) {
            0 -> {
                drawArc(c, 205f, 130f, false, style = stroke)
                drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.52f),
                    androidx.compose.ui.geometry.Offset(w * 0.72f, h * 0.32f), 1.5.dp.toPx(), StrokeCap.Round)
                drawCircle(c, 1.2.dp.toPx(), androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.52f))
            }
            1 -> {
                drawArc(c, 190f, 160f, false, style = stroke)
                drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.78f),
                    androidx.compose.ui.geometry.Offset(w * 0.68f, h * 0.78f), 1.5.dp.toPx(), StrokeCap.Round)
                drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.9f),
                    androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.9f), 1.5.dp.toPx(), StrokeCap.Round)
            }
            else -> {
                drawRoundRect(
                    color = c,
                    topLeft = androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.14f),
                    size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.72f),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                    style = stroke,
                )
                drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.37f, h * 0.5f),
                    androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.6f), 1.5.dp.toPx(), StrokeCap.Round)
                drawLine(c, androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.6f),
                    androidx.compose.ui.geometry.Offset(w * 0.66f, h * 0.37f), 1.5.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}
