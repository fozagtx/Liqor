package com.liqor.service

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Insets
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.Display
import android.view.Surface
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleService
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.liqor.Config
import com.liqor.MainActivity
import com.liqor.R
import com.liqor.data.DirectChainReader
import com.liqor.data.Prefs
import com.liqor.domain.AlertGate
import com.liqor.domain.Freshness
import com.liqor.domain.Level
import com.liqor.domain.Reading
import com.liqor.domain.Watched
import com.liqor.domain.sentence
import com.liqor.ui.BubbleUi
import com.liqor.ui.bubbleUi
import com.liqor.ui.closestPosition
import com.liqor.ui.components.Bubble
import com.liqor.ui.components.Panel
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class WatchService : LifecycleService() {
    private lateinit var prefs: Prefs
    private lateinit var overlayContext: Context
    private lateinit var windowManager: WindowManager
    private lateinit var displayManager: DisplayManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastDisplayRotation = Surface.ROTATION_0
    private val reader = DirectChainReader()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val screenOnRequests = Channel<Unit>(Channel.CONFLATED)
    private val alertGate = AlertGate(System::currentTimeMillis)
    private val overlayOwners = mutableMapOf<ComposeView, OverlayOwner>()
    private var reading: Reading = Reading.CouldNotCheck
    private var freshness = Freshness.Unknown
    private var lastSuccessfulServerTime: Instant? = null
    private var bubbleUi by mutableStateOf(BubbleUi("–", com.liqor.ui.BubbleState.Unknown, 0f))
    private var panelReading by mutableStateOf<Reading>(Reading.CouldNotCheck)
    private var panelFreshness by mutableStateOf(Freshness.Unknown)
    private var panelAgeSeconds by mutableStateOf<Long?>(null)
    private var screenOn = true
    private var panelOpen = false
    private var expandedAddress by mutableStateOf<String?>(null)
    private var bubbleView: ComposeView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelView: ComposeView? = null
    private var hideView: ComposeView? = null
    private var hideParams: WindowManager.LayoutParams? = null
    private var panelVisible by mutableStateOf(false)
    private val overlayComponentCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            onOverlayGeometryChanged()
        }

        override fun onLowMemory() = Unit
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            val rotation = displayManager.getDisplay(displayId)?.rotation ?: return
            if (rotation != lastDisplayRotation) {
                lastDisplayRotation = rotation
                onOverlayGeometryChanged()
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                screenOn = true
                if (prefs.mode == Prefs.MODE_LIVE) screenOnRequests.trySend(Unit)
            } else if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                screenOn = false
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        displayManager = getSystemService(DisplayManager::class.java)
        val display = checkNotNull(displayManager.getDisplay(Display.DEFAULT_DISPLAY))
        overlayContext = createDisplayContext(display).createWindowContext(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            null,
        )
        windowManager = overlayContext.getSystemService(WindowManager::class.java)
        lastDisplayRotation = display.rotation
        overlayContext.registerComponentCallbacks(overlayComponentCallbacks)
        displayManager.registerDisplayListener(displayListener, mainHandler)
        createNotificationChannels()
        startStatusForeground()
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            Context.RECEIVER_NOT_EXPORTED,
        )
        if (prefs.mode == Prefs.MODE_NONE || !prefs.watchingEnabled) {
            stopSelf()
            return
        }
        serviceScope.launch { pollingLoop() }
        serviceScope.launch { freshnessLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startStatusForeground()
        when (intent?.action) {
            ACTION_DETAILS -> {
                prefs.hidden = false
                showBubble()
                openPanel()
            }
            ACTION_OPEN_JUPITER -> openJupiter()
        }
        return Service.START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    private fun onOverlayGeometryChanged() {
        closePanel()
        bubbleParams?.let { params ->
            setBubbleStartPosition(params)
            bubbleView?.let { view ->
                runCatching { windowManager.updateViewLayout(view, params) }
            }
        }
        hideParams?.let { params ->
            params.y = dpToPixels(24) + safeInsets().bottom
            hideView?.let { view ->
                runCatching { windowManager.updateViewLayout(view, params) }
            }
        }
    }

    override fun onDestroy() {
        overlayContext.unregisterComponentCallbacks(overlayComponentCallbacks)
        displayManager.unregisterDisplayListener(displayListener)
        runCatching { unregisterReceiver(screenReceiver) }
        removePanel()
        removeBubble()
        removeHideTarget()
        screenOnRequests.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun pollingLoop() {
        while (serviceScope.isActive) {
            fetchLive()
            val danger = (reading as? Reading.Live)?.watched?.any { it.level == Level.Danger } == true
            val interval = when {
                reading == Reading.NoPositions || !screenOn -> 60_000L
                danger || panelOpen -> 5_000L
                else -> 15_000L
            }
            withTimeoutOrNull(interval) { screenOnRequests.receive() }
        }
    }

    private suspend fun freshnessLoop() {
        while (serviceScope.isActive) {
            if (prefs.mode == Prefs.MODE_LIVE && panelOpen) {
                panelAgeSeconds = ageSeconds()
            }
            val live = reading as? Reading.Live
            if (prefs.mode == Prefs.MODE_LIVE && live != null) {
                val age = Duration.between(live.serverTime, Instant.now()).seconds.coerceAtLeast(0)
                val next = com.liqor.domain.freshness(age)
                if (next != freshness) {
                    freshness = next
                    updateSurfaces()
                }
                val level = live.watched.maxByOrNull { it.level.ordinal }?.level
                if (
                    prefs.hidden &&
                    level == Level.Danger &&
                    System.currentTimeMillis() - prefs.hiddenAt >= 600_000L
                ) {
                    prefs.hidden = false
                    showBubble()
                }
            }
            delay(1_000)
        }
    }

    private suspend fun fetchLive() {
        if (prefs.mode != Prefs.MODE_LIVE) return
        val wallet = prefs.walletAddress
        reading = if (wallet == null) Reading.CouldNotCheck else reader.positions(wallet)
        (reading as? Reading.Live)?.let { lastSuccessfulServerTime = it.serverTime }
        freshness = if (reading is Reading.Live) freshness else Freshness.Unknown

        when (val current = reading) {
            is Reading.Live -> {
                val overall = current.watched.maxByOrNull { it.level.ordinal }?.level
                if (overall != null) {
                    val transition = alertGate.observe(overall)
                    if (transition != null) {
                        if (prefs.hidden) prefs.hidden = false
                        if (transition.alert) {
                            val watched = current.watched
                                .filter { it.level == transition.current }
                                .minByOrNull { it.distance }
                            if (watched != null) {
                                vibrate(transition.current)
                                postAlert(watched)
                            }
                        }
                    }
                }
            }
            Reading.NoPositions -> {
                removeBubble()
                alertGate.observe(Level.Safe)
            }
            Reading.CouldNotCheck -> Unit
        }
        updateSurfaces()
        updateStatusNotification()
    }

    private fun updateSurfaces() {
        if (reading == Reading.NoPositions) {
            removeBubble()
            return
        }
        if (prefs.hidden) {
            removeBubble()
        } else {
            showBubble()
        }
        if (panelOpen) updatePanelContent()
    }

    private fun currentBubbleUi(): BubbleUi = bubbleUi(reading, freshness)

    private fun showBubble() {
        if (!Settings.canDrawOverlays(this) || reading == Reading.NoPositions || prefs.hidden) return
        bubbleUi = currentBubbleUi()
        if (bubbleView != null) return
        val view = createComposeView()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            layoutEdgeToEdge()
            setBubbleStartPosition(this)
        }
        view.addOnLayoutChangeListener { changed, _, _, _, _, _, _, _, _ ->
            updateBubbleGestureExclusion(changed)
            val padding = bubbleShadowPaddingPx()
            val diameter = bubbleDiameterPx()
            val contentWidth = changed.width - 2 * padding
            val left = padding + ((contentWidth - diameter).coerceAtLeast(0) / 2)
            val params = bubbleParams
            if (bubbleView === changed && params != null && changed.height > 0) {
                val contentHeight = (changed.height - 2 * padding).coerceAtLeast(diameter)
                val bubbleBounds = bubbleBounds(contentHeight, left)
                val clamped = bubbleBounds.clampResting(params.x, params.y)
                if (clamped.x != params.x || clamped.y != params.y) {
                    params.x = clamped.x
                    params.y = clamped.y
                    runCatching { windowManager.updateViewLayout(changed, params) }
                }
            }
        }
        view.setContent {
            com.liqor.ui.theme.LiqorTheme {
                Bubble(bubbleUi, Modifier.padding(8.dp).bubbleGestures())
            }
        }
        runCatching { windowManager.addView(view, params) }
            .onSuccess {
                bubbleView = view
                bubbleParams = params
            }
    }

    private fun Modifier.bubbleGestures(): Modifier = this
        .pointerInput(Unit) {
            detectTapGestures(onTap = {
                if (panelOpen) closePanel() else openPanel()
            })
        }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    showHideTarget()
                },
                onDrag = { change, amount ->
                    change.consume()
                    val view = bubbleView ?: return@detectDragGestures
                    val params = bubbleParams ?: return@detectDragGestures
                    val clamped = bubbleBounds(
                        contentHeight = bubbleContentHeightPx(view),
                        circleLeft = bubbleCircleLeft(view),
                    ).clampDrag(
                        x = params.x + amount.x.toInt(),
                        y = params.y + amount.y.toInt(),
                    )
                    params.x = clamped.x
                    params.y = clamped.y
                    runCatching { windowManager.updateViewLayout(view, params) }
                },
                onDragEnd = { finishDrag() },
                onDragCancel = { finishDrag() },
            )
        }

    private fun finishDrag() {
        val view = bubbleView
        val params = bubbleParams
        if (view == null || params == null) {
            removeHideTarget()
            return
        }
        val screenBounds = windowManager.currentWindowMetrics.bounds
        val density = overlayContext.resources.displayMetrics.density
        val diameter = bubbleDiameterPx()
        val insets = safeInsets()
        val padding = bubbleShadowPaddingPx()
        val contentHeight = bubbleContentHeightPx(view)
        val bubbleBounds = bubbleBounds(contentHeight, bubbleCircleLeft(view))
        val dragPosition = bubbleBounds.clampDrag(params.x, params.y)
        params.x = dragPosition.x
        params.y = dragPosition.y
        val centerX = params.x + bubbleCircleLeft(view) + diameter / 2
        val centerY = params.y + padding + diameter / 2
        val hideCenterY = screenBounds.height() -
            dpToPixels(24) - insets.bottom - dpToPixels(28)
        if (
            kotlin.math.abs(centerX - screenBounds.width() / 2) < 64 * density &&
            centerY > hideCenterY - 64 * density
        ) {
            prefs.hidden = true
            removeBubble()
            removeHideTarget()
            return
        }
        val right = centerX >= screenBounds.width() / 2
        val restingY = params.y.coerceIn(bubbleBounds.minY, bubbleBounds.maxY)
        val targetX = if (right) bubbleBounds.maxX else bubbleBounds.minX
        animateToEdge(params, targetX, restingY)
        prefs.bubbleSide = if (right) Prefs.SIDE_RIGHT else Prefs.SIDE_LEFT
        prefs.bubbleYFraction = (
            (restingY - bubbleBounds.minY).toFloat() /
                (bubbleBounds.maxY - bubbleBounds.minY).coerceAtLeast(1)
            ).coerceIn(0f, 1f)
        removeHideTarget()
    }

    private fun animateToEdge(
        params: WindowManager.LayoutParams,
        targetX: Int,
        targetY: Int,
    ) {
        val startX = params.x
        val startY = params.y
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 220
            addUpdateListener { animator ->
                val progress = animator.animatedValue as Float
                params.x = (startX + (targetX - startX) * progress).toInt()
                params.y = (startY + (targetY - startY) * progress).toInt()
                bubbleView?.let { runCatching { windowManager.updateViewLayout(it, params) } }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bubbleView?.let(::updateBubbleGestureExclusion)
                }
            })
            start()
        }
    }

    private fun setBubbleStartPosition(params: WindowManager.LayoutParams) {
        val padding = bubbleShadowPaddingPx()
        val diameter = bubbleDiameterPx()
        val contentHeight = diameter + bubbleTagHeightPx()
        val bounds = bubbleBounds(contentHeight, padding)
        params.x = if (prefs.bubbleSide == Prefs.SIDE_LEFT) bounds.minX else bounds.maxX
        params.y = bounds.minY + ((bounds.maxY - bounds.minY) * prefs.bubbleYFraction).toInt()
    }

    private fun bubbleTagHeightPx(): Int =
        if (bubbleUi.asset != null && bubbleUi.side != null) dpToPixels(20) else 0

    private fun bubbleContentHeightPx(view: View? = bubbleView): Int {
        val padding = bubbleShadowPaddingPx()
        val measuredHeight = (view?.height ?: 0) - 2 * padding
        return measuredHeight.takeIf { it > 0 } ?: (bubbleDiameterPx() + bubbleTagHeightPx())
    }

    private fun bubbleCircleLeft(view: View?): Int {
        val padding = bubbleShadowPaddingPx()
        val diameter = bubbleDiameterPx()
        val contentWidth = ((view?.width ?: 0) - 2 * padding).coerceAtLeast(0)
        return padding + ((contentWidth - diameter).coerceAtLeast(0) / 2)
    }

    private fun bubbleBounds(contentHeight: Int, circleLeft: Int): OverlayBounds {
        val screen = windowManager.currentWindowMetrics.bounds
        val insets = safeInsets()
        return OverlayBounds.calculate(
            screenWidth = screen.width(),
            screenHeight = screen.height(),
            leftInset = insets.left,
            topInset = insets.top,
            rightInset = insets.right,
            bottomInset = insets.bottom,
            margin = dpToPixels(12),
            shadowPadding = bubbleShadowPaddingPx(),
            circleLeft = circleLeft,
            diameter = bubbleDiameterPx(),
            contentHeight = contentHeight,
        )
    }

    private fun safeInsets(): Insets {
        val insets = windowManager.currentWindowMetrics.windowInsets
        return Insets.max(
            insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()),
            insets.getInsets(WindowInsets.Type.displayCutout()),
        )
    }

    private fun WindowManager.LayoutParams.layoutEdgeToEdge() {
        setFitInsetsTypes(0)
        layoutInDisplayCutoutMode =
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }

    private fun updateBubbleGestureExclusion(view: View) {
        if (view.width <= 0 || view.height <= 0) return
        view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
    }

    private fun dpToPixels(dp: Int) =
        (dp * overlayContext.resources.displayMetrics.density).roundToInt()

    private fun bubbleDiameterPx() = dpToPixels(56)

    private fun bubbleShadowPaddingPx() = dpToPixels(8)

    private fun showHideTarget() {
        if (hideView != null) return
        val view = createComposeView()
        val params = WindowManager.LayoutParams(
            (56 * overlayContext.resources.displayMetrics.density).toInt(),
            (56 * overlayContext.resources.displayMetrics.density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dpToPixels(24) + safeInsets().bottom
            layoutEdgeToEdge()
        }
        view.setContent {
            com.liqor.ui.theme.LiqorTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF141414), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        stringResource(R.string.hide_glyph),
                        color = Color.White,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        runCatching { windowManager.addView(view, params) }.onSuccess {
            hideView = view
            hideParams = params
        }
    }

    private fun removeHideTarget() {
        hideView?.let(::disposeView)
        hideView = null
        hideParams = null
    }

    private fun removeBubble() {
        bubbleView?.let(::disposeView)
        bubbleView = null
        bubbleParams = null
    }

    private fun openPanel() {
        if (panelView != null || !Settings.canDrawOverlays(this)) return
        panelOpen = true
        panelVisible = false
        panelReading = reading
        panelFreshness = freshness
        panelAgeSeconds = ageSeconds()
        val owner = OverlayOwner()
        val view = createComposeView(owner)
        val insets = safeInsets()
        val density = overlayContext.resources.displayMetrics.density
        val padding = bubbleShadowPaddingPx()
        val bubbleBottomPx = (bubbleParams?.y ?: 0) + padding + bubbleContentHeightPx()
        view.setContent {
            com.liqor.ui.theme.LiqorTheme {
                PanelOverlay(
                    reading = panelReading,
                    freshness = panelFreshness,
                    ageSeconds = panelAgeSeconds,
                    visible = panelVisible,
                    side = prefs.bubbleSide,
                    safeLeft = (insets.left / density).dp,
                    safeTop = (insets.top / density).dp,
                    safeRight = (insets.right / density).dp,
                    safeBottom = (insets.bottom / density).dp,
                    bubbleBottom = (bubbleBottomPx / density).dp,
                    onClose = ::closePanel,
                    onMore = {
                        closePanel()
                        startActivity(
                            Intent(this, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    onOpenJupiter = {
                        closePanel()
                        openJupiter()
                    },
                    expandedAddress = expandedAddress,
                    onTogglePosition = { address ->
                        expandedAddress = if (expandedAddress == address) null else address
                    },
                )
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { layoutEdgeToEdge() }
        runCatching { windowManager.addView(view, params) }.onSuccess {
            panelView = view
            serviceScope.launch {
                delay(16)
                panelVisible = true
            }
        }.onFailure {
            panelOpen = false
            disposeView(view)
        }
    }

    @Composable
    private fun PanelOverlay(
        reading: Reading,
        freshness: Freshness,
        ageSeconds: Long?,
        visible: Boolean,
        side: String,
        safeLeft: Dp,
        safeTop: Dp,
        safeRight: Dp,
        safeBottom: Dp,
        bubbleBottom: Dp,
        onClose: () -> Unit,
        onMore: () -> Unit,
        onOpenJupiter: () -> Unit,
        expandedAddress: String?,
        onTogglePosition: (String) -> Unit,
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().background(Color(0x30000000)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClose),
            )
            val available = (maxHeight - safeTop - safeBottom - 24.dp).coerceAtLeast(0.dp)
            val minPanelTop = safeTop + 12.dp
            val maxPanelTop = (
                maxHeight - safeBottom - 12.dp - minOf(390.dp, available)
            ).coerceAtLeast(minPanelTop)
            val panelTop = (bubbleBottom + 8.dp).coerceIn(minPanelTop, maxPanelTop)
            val availablePanelHeight =
                (maxHeight - panelTop - safeBottom - 12.dp).coerceAtLeast(0.dp)
            AnimatedVisibility(
                visible = visible,
                modifier = Modifier
                    .align(if (side == Prefs.SIDE_RIGHT) Alignment.TopEnd else Alignment.TopStart)
                    .padding(
                        top = panelTop,
                        start = if (side == Prefs.SIDE_LEFT) 12.dp + safeLeft else 0.dp,
                        end = if (side == Prefs.SIDE_RIGHT) 12.dp + safeRight else 0.dp,
                    ),
                enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 10 },
                exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 14 },
            ) {
                Box(Modifier.clickable(onClick = {})) {
                    Panel(
                        reading = reading,
                        freshness = freshness,
                        ageSeconds = ageSeconds,
                        onOpenJupiter = onOpenJupiter,
                        onMore = onMore,
                        expandedPosition = expandedAddress,
                        onTogglePosition = onTogglePosition,
                        maxHeight = availablePanelHeight,
                    )
                }
            }
        }
        LaunchedEffect(Unit) { panelVisible = true }
    }

    private fun updatePanelContent() {
        if (!panelOpen) return
        panelReading = reading
        panelFreshness = freshness
        panelAgeSeconds = ageSeconds()
    }

    private fun closePanel() {
        if (!panelOpen) return
        panelOpen = false
        panelVisible = false
        serviceScope.launch {
            delay(200)
            removePanel()
        }
    }

    private fun removePanel() {
        panelView?.let(::disposeView)
        panelView = null
        panelVisible = false
    }

    private fun openJupiter() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(Config.JUPITER_LINK))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun createComposeView(owner: OverlayOwner? = null): ComposeView =
        ComposeView(overlayContext).also { view ->
            val viewOwner = owner ?: OverlayOwner()
            viewOwner.create()
            overlayOwners[view] = viewOwner
            val lifecycleTag = overlayContext.resources.getIdentifier(
                "view_tree_lifecycle_owner",
                "id",
                packageName,
            )
            val savedStateTag = overlayContext.resources.getIdentifier(
                "view_tree_saved_state_registry_owner",
                "id",
                packageName,
            )
            if (lifecycleTag != 0) view.setTag(lifecycleTag, viewOwner)
            if (savedStateTag != 0) view.setTag(savedStateTag, viewOwner)
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) = viewOwner.resume()
                override fun onViewDetachedFromWindow(view: View) = viewOwner.pause()
            })
        }

    private fun disposeView(view: ComposeView) {
        runCatching { windowManager.removeView(view) }
        overlayOwners.remove(view)?.destroy()
    }

    private fun updateStatusNotification() {
        val text = statusNotificationText()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(STATUS_NOTIFICATION_ID, buildStatusNotification(text))
    }

    private fun statusNotificationText(): String = when {
            reading == Reading.NoPositions -> getString(R.string.status_all_clear)
            reading == Reading.CouldNotCheck -> getString(R.string.status_could_not_check)
            reading is Reading.Live -> closestPosition((reading as Reading.Live))
                ?.let(::sentence) ?: getString(R.string.status_could_not_check)
            else -> getString(R.string.status_could_not_check)
        }

    private fun startStatusForeground() {
        val notification = buildStatusNotification(statusNotificationText())
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                STATUS_NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(STATUS_NOTIFICATION_ID, notification)
        }
    }

    private fun buildStatusNotification(text: String): Notification {
        val details = PendingIntent.getService(
            this,
            1,
            Intent(this, WatchService::class.java).setAction(ACTION_DETAILS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getService(
            this,
            2,
            Intent(this, WatchService::class.java).setAction(ACTION_OPEN_JUPITER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, STATUS_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, getString(R.string.notification_details), details).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.open_jupiter), open).build())
            .build()
    }

    private fun postAlert(watched: Watched) {
        if (prefs.mode != Prefs.MODE_LIVE) return
        val position = watched.position
        val leverage = (position.sizeUsd / position.collateralUsd).toInt().coerceAtLeast(1)
        val title = getString(
            if (watched.level == Level.Danger) R.string.notification_danger_title
            else R.string.notification_caution_title,
            position.asset,
            com.liqor.domain.distanceText(watched.distance),
        )
        val body = getString(
            R.string.notification_alert_body,
            com.liqor.domain.usd(position.liquidationPrice),
            position.asset,
            position.side,
            leverage,
        )
        val details = PendingIntent.getService(
            this,
            3,
            Intent(this, WatchService::class.java).setAction(ACTION_DETAILS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val open = PendingIntent.getService(
            this,
            4,
            Intent(this, WatchService::class.java).setAction(ACTION_OPEN_JUPITER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, ALERTS_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .addAction(Notification.Action.Builder(null, getString(R.string.notification_details), details).build())
            .addAction(Notification.Action.Builder(null, getString(R.string.open_jupiter), open).build())
            .build()
        getSystemService(NotificationManager::class.java)
            .notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun vibrate(level: Level) {
        val pattern = when (level) {
            Level.Caution -> longArrayOf(0, 40, 80, 40)
            Level.Danger -> longArrayOf(0, 250, 120, 250)
            Level.Safe -> return
        }
        val vibrator = getSystemService(Vibrator::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(STATUS_CHANNEL, getString(R.string.notification_channel_status), NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(ALERTS_CHANNEL, getString(R.string.notification_channel_alerts), NotificationManager.IMPORTANCE_HIGH),
        )
    }

    private fun ageSeconds(): Long? {
        val serverTime = lastSuccessfulServerTime ?: return null
        return Duration.between(serverTime, Instant.now()).seconds.coerceAtLeast(0)
    }

    companion object {
        const val ACTION_DETAILS = "com.liqor.action.DETAILS"
        const val ACTION_OPEN_JUPITER = "com.liqor.action.OPEN_JUPITER"
        private const val STATUS_CHANNEL = "status"
        private const val ALERTS_CHANNEL = "alerts"
        private const val STATUS_NOTIFICATION_ID = 41
        private const val ALERT_NOTIFICATION_ID = 42
    }
}

private class OverlayOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

    fun create() {
        controller.performAttach()
        controller.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun resume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun pause() {
        if (lifecycleRegistry.currentState != Lifecycle.State.DESTROYED) {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }
}
