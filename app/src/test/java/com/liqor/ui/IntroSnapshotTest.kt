package com.liqor.ui

import androidx.compose.runtime.CompositionLocalProvider
import com.liqor.domain.samplePosition
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h890dp-xxhdpi")
class IntroSnapshotTest {
    @Test
    fun beforeAfter() = captureScreen("intro_before_after") {
        IntroScreen(samplePosition.at(0), onFinish = {})
    }

    @Test
    fun watches() = captureScreen("intro_watches") {
        IntroScreen(samplePosition.at(0), onFinish = {}, initialPage = 1)
    }

    @Test
    fun bubbleDemoPanelOpen() = captureScreen("intro_bubble_demo") {
        IntroScreen(samplePosition.at(0), onFinish = {}, initialPage = 2, demoTimeMs = 2_100L)
    }

    @Test
    fun bubbleDemoDragged() = captureScreen("intro_bubble_demo_drag") {
        IntroScreen(samplePosition.at(0), onFinish = {}, initialPage = 2, demoTimeMs = 3_900L)
    }

    @Test
    fun bubbleDemoHide() = captureScreen("intro_bubble_demo_hide") {
        IntroScreen(samplePosition.at(0), onFinish = {}, initialPage = 2, demoTimeMs = 5_600L)
    }

    @Test
    fun bubbleDemoReducedMotion() = captureScreen("intro_bubble_demo_reduced_motion") {
        CompositionLocalProvider(LocalReducedMotion provides true) {
            IntroScreen(samplePosition.at(0), onFinish = {}, initialPage = 2, demoTimeMs = 5_600L)
        }
    }

    @Test
    fun beforeAfterAtLargeFontScale() = captureScreen("intro_before_after_fontscale_1_3", 1.3f) {
        IntroScreen(samplePosition.at(0), onFinish = {})
    }
}
