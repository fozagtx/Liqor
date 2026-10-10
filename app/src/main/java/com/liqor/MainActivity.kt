package com.liqor

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.liqor.data.DirectChainReader
import com.liqor.data.Prefs
import com.liqor.domain.Reading
import com.liqor.domain.samplePosition
import com.liqor.ui.CouldNotCheckScreen
import com.liqor.ui.AppScreen
import com.liqor.ui.BubbleOnScreen
import com.liqor.ui.FirstLaunchScreen
import com.liqor.ui.IntroScreen
import com.liqor.ui.LocalReducedMotion
import com.liqor.ui.HomeScreen
import com.liqor.ui.LoadingScreen
import com.liqor.ui.NoPositionsScreen
import com.liqor.ui.NotificationPermissionScreen
import com.liqor.ui.OverlayPermissionScreen
import com.liqor.ui.PositionsFoundScreen
import com.liqor.ui.ScreenHost
import com.liqor.ui.SplashScreen
import com.liqor.ui.theme.LiqorTheme
import com.liqor.wallet.Base58
import com.liqor.service.WatchService
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var resultSender: ActivityResultSender
    private lateinit var prefs: Prefs
    private val reader = DirectChainReader()
    private val adapter by lazy {
        MobileWalletAdapter(
            ConnectionIdentity(
                identityUri = Uri.parse("https://github.com/fozagtx/Liqor"),
                iconUri = Uri.parse("favicon.ico"),
                identityName = "Liqor",
            ),
        )
    }
    private var screen by mutableStateOf(AppScreen.Splash)
    private var forward by mutableStateOf(true)
    private val backStack = mutableStateListOf<AppScreen>()
    private var reducedMotion by mutableStateOf(false)
    private var reading by mutableStateOf<Reading>(Reading.CouldNotCheck)
    private var appMode by mutableStateOf(Prefs.MODE_NONE)
    private var walletAddress by mutableStateOf<String?>(null)
    private var elapsedMs by mutableLongStateOf(0L)
    private var overlayStillOff by mutableStateOf(false)
    private var waitingForOverlay = false
    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            startServiceAndShowHome()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        resultSender = ActivityResultSender(this)
        prefs = Prefs(this)
        appMode = prefs.mode
        walletAddress = prefs.walletAddress
        refreshReducedMotion()
        setContent {
            LiqorTheme {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalReducedMotion provides reducedMotion,
                ) {
                    BackHandler(
                        enabled = backStack.isNotEmpty() &&
                            screen != AppScreen.Loading &&
                            screen != AppScreen.Home &&
                            screen != AppScreen.BubbleOn,
                    ) {
                        forward = false
                        screen = backStack.removeAt(backStack.lastIndex)
                    }
                    LaunchedEffect(Unit) {
                        delay(800)
                        if (appMode == Prefs.MODE_NONE) {
                            navigateRoot(AppScreen.Intro, true)
                        } else {
                            navigateRoot(AppScreen.Home, true)
                            if (appMode == Prefs.MODE_LIVE) refreshPositions(showLoading = false)
                            if (Settings.canDrawOverlays(this@MainActivity)) startWatching()
                        }
                    }
                    LaunchedEffect(screen, appMode) {
                        if (screen == AppScreen.Intro ||
                            screen == AppScreen.FirstLaunch ||
                            appMode == Prefs.MODE_SAMPLE
                        ) {
                            val start = System.currentTimeMillis()
                            while (true) {
                                elapsedMs = System.currentTimeMillis() - start
                                delay(200)
                            }
                        }
                    }
                    ScreenHost(screen, forward) { current ->
                        when (current) {
                            AppScreen.Splash -> SplashScreen()
                            AppScreen.Intro -> IntroScreen(
                                sample = samplePosition.at(elapsedMs),
                                onFinish = { navigate(AppScreen.FirstLaunch) },
                            )
                            AppScreen.FirstLaunch -> FirstLaunchScreen(
                                sample = samplePosition.at(elapsedMs),
                                onConnect = ::connectWallet,
                            )
                            AppScreen.Loading -> LoadingScreen()
                            AppScreen.PositionsFound -> {
                                val live = reading as? Reading.Live
                                if (live != null) {
                                    PositionsFoundScreen(live, onTurnOnBubble = ::beginOverlaySetup)
                                } else {
                                    LoadingScreen()
                                }
                            }
                            AppScreen.NoPositions -> NoPositionsScreen(
                                sample = samplePosition.at(elapsedMs),
                                onShowSample = ::startSampleMode,
                            )
                            AppScreen.CouldNotCheck -> CouldNotCheckScreen {
                                if (walletAddress == null) connectWallet() else refreshPositions()
                            }
                            AppScreen.OverlayPermission -> OverlayPermissionScreen(
                                onOpenSettings = ::openOverlaySettings,
                                onOpenAppInfo = ::openAppInfo,
                                onNotNow = { navigateRoot(AppScreen.Home, true) },
                                stillOff = overlayStillOff,
                            )
                            AppScreen.BubbleOn -> BubbleOnScreen(
                                onNext = { navigate(AppScreen.NotificationPermission) },
                            )
                            AppScreen.NotificationPermission -> NotificationPermissionScreen(
                                onAllow = ::requestNotificationPermission,
                                onNotNow = ::startServiceAndShowHome,
                            )
                            AppScreen.Home -> HomeScreen(
                                reading = reading,
                                sample = samplePosition.at(elapsedMs),
                                sampleMode = appMode == Prefs.MODE_SAMPLE,
                                onRetry = ::refreshPositions,
                                onDisconnect = ::disconnect,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshReducedMotion()
        if (waitingForOverlay) {
            waitingForOverlay = false
            if (Settings.canDrawOverlays(this)) {
                overlayStillOff = false
                startWatching()
                navigate(AppScreen.BubbleOn)
            } else if (screen == AppScreen.OverlayPermission) {
                overlayStillOff = true
            }
        }
    }

    private fun connectWallet() {
        navigate(AppScreen.Loading)
        lifecycleScope.launch {
            val result = runCatching { adapter.connect(resultSender) }.getOrNull()
            val success = result as? TransactionResult.Success<*>
            val publicKey = success?.authResult?.publicKey
            if (publicKey == null || publicKey.isEmpty()) {
                navigate(AppScreen.CouldNotCheck)
                return@launch
            }
            walletAddress = Base58.encode(publicKey)
            prefs.walletAddress = walletAddress
            prefs.mode = Prefs.MODE_LIVE
            appMode = Prefs.MODE_LIVE
            refreshPositions()
        }
    }

    private fun refreshPositions(showLoading: Boolean = true) {
        val address = walletAddress ?: return navigateRoot(AppScreen.FirstLaunch, false)
        if (showLoading && screen != AppScreen.Loading) navigate(AppScreen.Loading)
        lifecycleScope.launch {
            reading = reader.positions(address)
            if (showLoading) {
                navigate(when (reading) {
                    is Reading.Live -> AppScreen.PositionsFound
                    Reading.NoPositions -> AppScreen.NoPositions
                    Reading.CouldNotCheck -> AppScreen.CouldNotCheck
                })
            }
        }
    }

    private fun startSampleMode() {
        prefs.mode = Prefs.MODE_SAMPLE
        prefs.walletAddress = null
        appMode = Prefs.MODE_SAMPLE
        beginOverlaySetup()
    }

    private fun beginOverlaySetup() {
        overlayStillOff = false
        navigate(if (Settings.canDrawOverlays(this)) {
            AppScreen.BubbleOn
        } else {
            AppScreen.OverlayPermission
        })
    }

    private fun openOverlaySettings() {
        waitingForOverlay = true
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun openAppInfo() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName"),
            ),
        )
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startServiceAndShowHome()
        }
    }

    private fun startServiceAndShowHome() {
        if (Settings.canDrawOverlays(this) && appMode != Prefs.MODE_NONE) {
            startForegroundService(Intent(this, WatchService::class.java))
        }
        navigateRoot(AppScreen.Home, true)
    }

    private fun startWatching() {
        if (appMode != Prefs.MODE_NONE) {
            startForegroundService(Intent(this, WatchService::class.java))
        }
    }

    private fun disconnect() {
        stopService(Intent(this, WatchService::class.java))
        prefs.clear()
        appMode = Prefs.MODE_NONE
        walletAddress = null
        reading = Reading.CouldNotCheck
        navigateRoot(AppScreen.FirstLaunch, false)
    }

    private fun navigate(to: AppScreen) {
        if (screen !in setOf(AppScreen.Splash, AppScreen.Loading, AppScreen.CouldNotCheck)) {
            backStack.add(screen)
        }
        forward = true
        screen = to
    }

    private fun navigateRoot(to: AppScreen, isForward: Boolean) {
        backStack.clear()
        forward = isForward
        screen = to
    }

    private fun refreshReducedMotion() {
        reducedMotion =
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
