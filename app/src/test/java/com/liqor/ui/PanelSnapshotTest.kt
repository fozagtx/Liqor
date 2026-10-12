package com.liqor.ui

import com.liqor.domain.Freshness
import com.liqor.domain.Reading
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h890dp-xxhdpi")
class PanelSnapshotTest {
    private val pair = WorkerSnapshotData.walletPositions

    @Test
    fun twoRealPositions() =
        capturePanel("panel_two", pair, Freshness.Fresh, 12)

    @Test
    fun betaPositionShowsVenue() =
        capturePanel("panel_beta_position", WorkerSnapshotData.beta, Freshness.Fresh, 12)

    @Test
    fun fiveRealPositionsShowsTwoMore() =
        capturePanel("panel_five_positions", WorkerSnapshotData.all, Freshness.Fresh, 12)

    @Test
    fun expandedRowShowsTrain() = capturePanel(
        "panel_row_expanded",
        pair,
        Freshness.Fresh,
        12,
        expandedPosition = pair.watched.last().position.address,
    )

    @Test
    fun stalePanelUsesAgeAndGreyState() =
        capturePanel("panel_stale", pair, Freshness.Stale, 47)

    @Test
    fun couldNotCheckPanelShowsLastSuccessfulCheck() =
        capturePanel("panel_could_not_check", Reading.CouldNotCheck, Freshness.Unknown, 121)

    @Test
    fun noPositionsPanelShowsLastCheckedTime() =
        capturePanel("panel_no_positions", Reading.NoPositions, Freshness.Fresh, 12)
}
