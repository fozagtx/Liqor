package com.liqor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liqor.R
import com.liqor.ui.components.Bubble
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.LiqorTheme
import com.liqor.ui.theme.TextPrimary
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h890dp-xxhdpi")
class BubbleSnapshotTest {
    @Test
    fun bubbleStates() = captureScreen("bubble_states") {
        val positions = WorkerSnapshotData.all.watched
        val btcShort = positions.first {
            it.position.address == "BTeM8V1TLgZwEe7ENCxCQtQmvMoxcseipryLawCKyRPd"
        }
        val fourPercent = positions.first {
            com.liqor.domain.distanceText(it.distance).startsWith("4.")
        }
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxSize().background(Ground).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BubbleCaption(positions[4], BubbleState.Safe, R.string.level_safe, Modifier.weight(1f))
                BubbleCaption(fourPercent, BubbleState.Caution, R.string.level_caution, Modifier.weight(1f))
                BubbleCaption(btcShort, BubbleState.Danger, R.string.level_danger, Modifier.weight(1f))
                BubbleCaption(positions[1], BubbleState.Stale, R.string.stale_label, Modifier.weight(1f))
                BubbleCaption("–", BubbleState.Unknown, R.string.bubble_state_unknown, Modifier.weight(1f))
                BubbleCaption(
                    com.liqor.domain.distanceText(com.liqor.domain.samplePosition.at(0).distance),
                    BubbleState.Sample,
                    R.string.sample_caption,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BubbleCaption(
    watched: com.liqor.domain.Watched,
    state: BubbleState,
    captionId: Int,
    modifier: Modifier,
) = BubbleCaption(
    value = com.liqor.domain.distanceText(watched.distance),
    state = state,
    captionId = captionId,
    cushion = 1f - watched.travelled.toFloat(),
    modifier = modifier,
    asset = watched.position.asset,
    side = watched.position.side,
)

@Composable
private fun BubbleCaption(
    value: String,
    state: BubbleState,
    captionId: Int,
    modifier: Modifier,
) = BubbleCaption(value, state, captionId, 0.58f, modifier)

@Composable
private fun BubbleCaption(
    value: String,
    state: BubbleState,
    captionId: Int,
    cushion: Float,
    modifier: Modifier,
    asset: String? = null,
    side: String? = null,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Bubble(
            BubbleUi(
                value,
                state,
                cushion = cushion,
                animateDanger = false,
                asset = asset,
                side = side,
            ),
        )
        Text(
            text = stringResource(captionId),
            modifier = Modifier.fillMaxWidth(),
            color = TextPrimary,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
