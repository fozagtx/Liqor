package com.liqor.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liqor.R
import com.liqor.ui.LocalAnimatedScope
import com.liqor.ui.LocalReducedMotion
import com.liqor.ui.theme.CardBorder
import com.liqor.ui.theme.Elevation
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.Hairline
import com.liqor.ui.theme.IllustrationSize
import com.liqor.ui.theme.Motion
import com.liqor.ui.theme.Radius
import com.liqor.ui.theme.Raised
import com.liqor.ui.theme.Space
import com.liqor.ui.theme.TextPrimary
import com.liqor.ui.theme.TextSecondary
import com.liqor.ui.theme.TypeRamp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private const val PERMISSION_SHEET_DELAY_MS = 600L

class PermissionSheetState internal constructor(
    val visible: Boolean,
    val dismiss: () -> Unit,
)

@Composable
fun rememberPermissionSheetState(
    sheetInitiallyOpen: Boolean = false,
    reopenKey: Int = 0,
): PermissionSheetState {
    var visible by remember { mutableStateOf(sheetInitiallyOpen) }
    var dismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!sheetInitiallyOpen) {
            delay(PERMISSION_SHEET_DELAY_MS)
            if (!dismissed) visible = true
        }
    }
    LaunchedEffect(sheetInitiallyOpen) {
        if (sheetInitiallyOpen) {
            dismissed = false
            visible = true
        }
    }
    LaunchedEffect(reopenKey) {
        if (reopenKey > 0) {
            dismissed = false
            visible = false
            delay(PERMISSION_SHEET_DELAY_MS)
            if (!dismissed) visible = true
        }
    }

    return PermissionSheetState(visible) {
        visible = false
        dismissed = true
    }
}

@Composable
fun PermissionSheet(
    visible: Boolean,
    icon: @Composable BoxScope.() -> Unit,
    title: String,
    body: String,
    primaryText: String,
    primaryGlyph: String? = null,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shouldAnimate = !LocalReducedMotion.current && LocalAnimatedScope.current != null
    val transition = updateTransition(targetState = visible, label = "permission-sheet")
    val scrimAlpha by transition.animateFloat(
        transitionSpec = {
            tween(
                if (shouldAnimate) Motion.Fast else 0,
                easing = Motion.SmoothOut,
            )
        },
        label = "permission-sheet-scrim",
    ) { if (it) 0.4f else 0f }
    val slideFraction by transition.animateFloat(
        transitionSpec = {
            tween(
                if (shouldAnimate) Motion.Medium else 0,
                easing = Motion.SmoothOut,
            )
        },
        label = "permission-sheet-slide",
    ) { if (it) 0f else 1f }

    BackHandler(enabled = visible, onBack = onDismiss)

    if (transition.currentState || transition.targetState) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(TextPrimary.copy(alpha = scrimAlpha))
                    .clickable(onClick = onDismiss),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = size.height * slideFraction
                    }
                    .drawWithContent {
                        drawContent()
                        val inset = Radius.lg.toPx()
                        drawLine(
                            color = CardBorder,
                            start = Offset(inset, IllustrationSize.cardBorder.toPx() / 2f),
                            end = Offset(
                                size.width - inset,
                                IllustrationSize.cardBorder.toPx() / 2f,
                            ),
                            strokeWidth = IllustrationSize.cardBorder.toPx(),
                        )
                    },
                shape = RoundedCornerShape(
                    topStart = Radius.lg,
                    topEnd = Radius.lg,
                ),
                color = Raised,
                shadowElevation = Elevation.card,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                        )
                        .padding(
                            horizontal = Space.screenHorizontal,
                            vertical = Space.xl,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Hairline),
                    )
                    Spacer(Modifier.height(Space.xl))
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Ground),
                        contentAlignment = Alignment.Center,
                        content = icon,
                    )
                    Spacer(Modifier.height(Space.lg))
                    Text(
                        text = title,
                        color = TextPrimary,
                        style = TypeRamp.title.copy(fontSize = 22.sp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = body,
                        modifier = Modifier.fillMaxWidth(),
                        color = TextSecondary,
                        style = TypeRamp.body,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(Space.xl))
                    PillButton(
                        text = primaryText,
                        onClick = onPrimary,
                        leadingGlyph = primaryGlyph,
                    )
                    Spacer(Modifier.height(Space.sm))
                    SecondaryPillButton(
                        text = androidx.compose.ui.res.stringResource(R.string.not_now),
                        onClick = onDismiss,
                        height = 56.dp,
                    )
                }
            }
        }
    }
}
