package com.liqor.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.liqor.ui.theme.Motion
import kotlinx.coroutines.delay

enum class AppScreen {
    Splash,
    Intro,
    FirstLaunch,
    Loading,
    PositionsFound,
    NoPositions,
    CouldNotCheck,
    OverlayPermission,
    BubbleOn,
    NotificationPermission,
    Home,
}

val LocalReducedMotion = staticCompositionLocalOf { false }
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ScreenHost(
    screen: AppScreen,
    forward: Boolean,
    content: @Composable (AppScreen) -> Unit,
) {
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val pageSlidePx = with(density) { Motion.PageSlide.roundToPx() }
    val pageBlurPx = with(density) { Motion.PageBlur.toPx() }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                val transition = when {
                    reducedMotion -> EnterTransition.None togetherWith ExitTransition.None
                    initialState == AppScreen.Splash -> {
                        fadeIn(tween(Motion.Fast, easing = Motion.SmoothOut)) togetherWith
                            fadeOut(tween(Motion.Quick))
                    }
                    forward -> {
                        (
                            slideInHorizontally(
                                tween(Motion.Fast, easing = Motion.SmoothOut),
                            ) { pageSlidePx } +
                                fadeIn(tween(Motion.Fast, easing = Motion.SmoothOut))
                            ) togetherWith (
                                slideOutHorizontally(
                                    tween(Motion.Fast, easing = Motion.SmoothOut),
                                ) { -pageSlidePx } +
                                    fadeOut(tween(Motion.Quick))
                                )
                    }
                    else -> {
                        (
                            slideInHorizontally(
                                tween(Motion.Fast, easing = Motion.SmoothOut),
                            ) { -pageSlidePx } +
                                fadeIn(tween(Motion.Fast, easing = Motion.SmoothOut))
                            ) togetherWith (
                                slideOutHorizontally(
                                    tween(Motion.Fast, easing = Motion.SmoothOut),
                                ) { pageSlidePx } +
                                    fadeOut(tween(Motion.Quick))
                        )
                    }
                }
                ContentTransform(
                    targetContentEnter = transition.targetContentEnter,
                    initialContentExit = transition.initialContentExit,
                    targetContentZIndex = 1f,
                ).using(SizeTransform(clip = false))
            },
        ) { target ->
            val pageBlur by transition.animateFloat(label = "page-blur") { state ->
                if (reducedMotion || state == EnterExitState.Visible) 0f else pageBlurPx
            }
            CompositionLocalProvider(
                LocalSharedScope provides this@SharedTransitionLayout,
                LocalAnimatedScope provides this,
            ) {
                Box(
                    Modifier.graphicsLayer {
                        renderEffect = if (pageBlur == 0f) {
                            null
                        } else {
                            BlurEffect(pageBlur, pageBlur, TileMode.Decal)
                        }
                    },
                ) {
                    content(target)
                }
            }
        }
    }
}

fun Modifier.stagger(index: Int): Modifier = composed {
    val reducedMotion = LocalReducedMotion.current
    val animatedScope = LocalAnimatedScope.current
    if (reducedMotion || animatedScope == null) {
        this
    } else {
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay((index * Motion.Stagger).toLong())
            progress.animateTo(1f, tween(Motion.VerySlow, easing = Motion.SmoothOut))
        }
        graphicsLayer {
            val value = progress.value
            alpha = value
            translationY = (1f - value) * Motion.TextReveal.toPx()
            val blurPx = (1f - value) * Motion.RevealBlur.toPx()
            renderEffect = if (blurPx == 0f) {
                null
            } else {
                BlurEffect(blurPx, blurPx, TileMode.Decal)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.sharedTicket(): Modifier = composed {
    val sharedScope = LocalSharedScope.current
    val animatedScope = LocalAnimatedScope.current
    if (LocalReducedMotion.current || sharedScope == null || animatedScope == null) {
        this
    } else {
        with(sharedScope) {
            sharedBounds(
                rememberSharedContentState("ticket"),
                animatedVisibilityScope = animatedScope,
                enter = fadeIn(tween(Motion.Fast)),
                exit = fadeOut(tween(Motion.Quick)),
                boundsTransform = { _, _ -> tween(Motion.Slow, easing = Motion.SmoothOut) },
            )
        }
    }
}
