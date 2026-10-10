package com.liqor.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.liqor.domain.Freshness
import com.liqor.domain.Reading
import com.liqor.ui.components.Bubble
import com.liqor.ui.components.Panel
import com.liqor.ui.theme.Ground
import com.liqor.ui.theme.LiqorTheme
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowLooper

fun captureScreen(
    name: String,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
    val activity = controller.get()
    activity.setContent {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, fontScale),
        ) {
            LiqorTheme(content = content)
        }
    }
    ShadowLooper.idleMainLooper()
    activity.window.decorView.captureRoboImage("$name.png")
    controller.pause().stop().destroy()
}

fun capturePanel(
    name: String,
    reading: Reading,
    freshness: Freshness,
    ageSeconds: Long,
    expandedPosition: String? = null,
) {
    captureScreen(name) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Ground)) {
            Bubble(
                bubbleUi(reading, freshness),
                Modifier.align(Alignment.TopEnd).padding(top = 356.dp, end = 12.dp),
            )
            Panel(
                reading = reading,
                freshness = freshness,
                ageSeconds = ageSeconds,
                sample = false,
                onOpenJupiter = {},
                onMore = {},
                expandedPosition = expandedPosition,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 436.dp, end = 12.dp),
                maxHeight = (maxHeight - 436.dp - 16.dp).coerceAtLeast(0.dp),
            )
        }
    }
}
