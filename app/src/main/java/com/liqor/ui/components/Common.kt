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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 58.dp,
    leadingGlyph: String? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(height),
        shape = CircleShape,
        color = TextPrimary,
        contentColor = Raised,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingGlyph != null) {
                Text(
                    leadingGlyph,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                )
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.06.em,
                ),
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
            .padding(horizontal = 18.dp, vertical = 12.dp),
        color = TextPrimary,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
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
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = foreground,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.sp,
            letterSpacing = 0.03.em,
        ),
    )
}

@Composable
fun SampleChip(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.sample_tag),
        modifier = modifier
            .clip(CircleShape)
            .background(TextPrimary)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        color = Raised,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
    )
}

@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(TextPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(14.dp)) {
            drawArc(
                Color.White,
                -30f,
                300f,
                false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun LineIcon(kind: Int, modifier: Modifier = Modifier) {
    Canvas(modifier.size(20.dp)) {
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), cap = StrokeCap.Round)
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
