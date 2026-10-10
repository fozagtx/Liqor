package com.liqor.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    fun homeLive() = captureScreen("home_live") {
        HomeScreen(
            reading = WorkerSnapshotData.walletPositions,
            sample = samplePosition.at(0),
            sampleMode = false,
            onRetry = {},
            onDisconnect = {},
        )
    }

    @Test
    fun noPositions() = captureScreen("no_positions") {
        NoPositionsScreen(samplePosition.at(0), onShowSample = {})
    }

    @Test
    fun overlayPermission() = captureScreen("overlay_permission") {
        OverlayPermissionScreen(onOpenSettings = {}, onNotNow = {}, demoTimeMs = 1_100L)
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
        NotificationPermissionScreen(onAllow = {}, onNotNow = {})
    }

    @Test
    fun firstLaunchAtLargeFontScale() = captureScreen("first_launch_fontscale_1_3", 1.3f) {
        FirstLaunchScreen(samplePosition.at(0), onConnect = {})
    }
}
