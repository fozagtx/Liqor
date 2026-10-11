package com.liqor.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liqor.domain.Reading
import com.liqor.domain.samplePosition
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h890dp-xxhdpi")
class ScreensSnapshotTest {
    @Test
    fun splash() = captureScreen("splash") { SplashScreen() }

    @Test
    fun firstLaunchSampleAtZero() = captureScreen("first_launch") {
        FirstLaunchScreen(samplePosition.at(0), onConnect = {})
    }

    @Test
    fun loading() = captureScreen("loading") { LoadingScreen() }

    @Test
    fun positionsFound() = captureScreen("positions_found") {
        PositionsFoundScreen(
            WorkerSnapshotData.walletPositions,
            onTurnOnBubble = {},
            wallet = WorkerSnapshotData.walletAddress,
        )
    }

    @Test
    fun couldNotCheckScreen() = captureScreen("could_not_check_screen") {
        CouldNotCheckScreen(onTryAgain = {})
    }

    @Test
    fun walletFailedScreen() = captureScreen("wallet_failed_screen") {
        CouldNotCheckScreen(walletFailed = true, onTryAgain = {})
    }

    @Test
    fun homeLive() = captureScreen("home_live") {
        HomeScreen(
            reading = WorkerSnapshotData.walletPositions,
            wallet = WorkerSnapshotData.walletAddress,
            bubbleOn = true,
            onBubbleChange = {},
            onRetry = {},
            onDisconnect = {},
        )
    }

    @Test
    fun homeFivePositions() = captureScreen("home_five_positions") {
        HomeScreen(
            reading = WorkerSnapshotData.all,
            wallet = WorkerSnapshotData.walletAddress,
            bubbleOn = true,
            onBubbleChange = {},
            onRetry = {},
            onDisconnect = {},
        )
    }

    @Test
    fun homeBubbleOff() = captureScreen("home_bubble_off") {
        HomeScreen(
            reading = WorkerSnapshotData.walletPositions,
            wallet = WorkerSnapshotData.walletAddress,
            bubbleOn = false,
            onBubbleChange = {},
            onRetry = {},
            onDisconnect = {},
        )
    }

    @Test
    fun homeNoPositions() = captureScreen("home_no_positions") {
        HomeScreen(
            reading = Reading.NoPositions,
            wallet = WorkerSnapshotData.walletAddress,
            bubbleOn = false,
            onBubbleChange = {},
            onRetry = {},
            onDisconnect = {},
        )
    }

    @Test
    fun noPositions() = captureScreen("no_positions") {
        NoPositionsScreen(onTurnOnBubble = {})
    }

    @Test
    fun overlayPermission() = captureScreen("overlay_permission") {
        OverlayPermissionScreen(onOpenSettings = {}, onNotNow = {}, demoTimeMs = 1_100L)
    }

    @Test
    fun overlayPermissionSheet() = captureScreen("overlay_permission_sheet") {
        OverlayPermissionScreen(
            onOpenSettings = {},
            onNotNow = {},
            demoTimeMs = 1_100L,
            sheetInitiallyOpen = true,
        )
    }

    @Test
    fun overlayStillOff() = captureScreen("overlay_still_off") {
        OverlayPermissionScreen(
            onOpenSettings = {},
            onNotNow = {},
            stillOff = true,
            demoTimeMs = 1_100L,
        )
    }

    @Test
    fun bubbleOn() = captureScreen("bubble_on") {
        BubbleOnScreen(onNext = {})
    }

    @Test
    fun notificationPermission() = captureScreen("notification_permission") {
        NotificationPermissionScreen(samplePosition.at(0), onAllow = {}, onNotNow = {})
    }

    @Test
    fun notificationPermissionSheet() = captureScreen("notification_permission_sheet") {
        NotificationPermissionScreen(
            samplePosition.at(0),
            onAllow = {},
            onNotNow = {},
            sheetInitiallyOpen = true,
        )
    }

    @Test
    fun homeOverlaySheet() = captureScreen("home_overlay_sheet") {
        HomeScreen(
            reading = WorkerSnapshotData.walletPositions,
            wallet = WorkerSnapshotData.walletAddress,
            bubbleOn = false,
            onBubbleChange = {},
            onRetry = {},
            onDisconnect = {},
            overlaySheetVisible = true,
            onOpenOverlaySettings = {},
            onDismissOverlaySheet = {},
        )
    }

    @Test
    fun firstLaunchAtLargeFontScale() = captureScreen("first_launch_fontscale_1_3", 1.3f) {
        FirstLaunchScreen(samplePosition.at(0), onConnect = {})
    }
}
