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
import androidx.core.app.NotificationManagerCompat
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
import com.solana.mobilewalletadapter.clientlib.Solana
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
        ).apply {
            blockchain = Solana.Mainnet
        }
    }
    private var screen by mutableStateOf(AppScreen.Splash)
    private var forward by mutableStateOf(true)
    private val backStack = mutableStateListOf<AppScreen>()
    private var reducedMotion by mutableStateOf(false)
    private var reading by mutableStateOf<Reading>(Reading.CouldNotCheck)
    private var appMode by mutableStateOf(Prefs.MODE_NONE)
    private var bubbleOn by mutableStateOf(false)
    private var walletAddress by mutableStateOf<String?>(null)
    private var walletFailed by mutableStateOf(false)
    private var elapsedMs by mutableLongStateOf(0L)
    private var overlayStillOff by mutableStateOf(false)
    private var waitingForOverlay = false
    private var waitingForOverlayFromHome = false
    private var homeOverlaySheetVisible by mutableStateOf(false)
    private var overlaySheetReopenKey by mutableStateOf(0)
    private var waitingForNotificationSettings = false
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
        bubbleOn = Settings.canDrawOverlays(this) &&
            appMode != Prefs.MODE_NONE &&
            prefs.watchingEnabled
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
                            AppScreen.CouldNotCheck -> CouldNotCheckScreen(
                                walletFailed = walletFailed,
                                onTryAgain = {
                                    if (walletFailed) connectWallet() else refreshPositions()
                                },
                            )
                            AppScreen.OverlayPermission -> OverlayPermissionScreen(
                                onOpenSettings = { openOverlaySettings() },
                                onOpenAppInfo = ::openAppInfo,
                                onNotNow = { navigateRoot(AppScreen.Home, true) },
                                stillOff = overlayStillOff,
                                sheetReopenKey = overlaySheetReopenKey,
                            )
                            AppScreen.BubbleOn -> BubbleOnScreen(
                                onNext = { navigate(AppScreen.NotificationPermission) },
                            )
                            AppScreen.NotificationPermission -> NotificationPermissionScreen(
                                sample = samplePosition.at(elapsedMs),
                                onAllow = ::requestNotificationPermission,
                                onNotNow = ::startServiceAndShowHome,
                            )
                            AppScreen.Home -> HomeScreen(
                                reading = reading,
                                sample = samplePosition.at(elapsedMs),
                                sampleMode = appMode == Prefs.MODE_SAMPLE,
                                bubbleOn = bubbleOn,
                                onBubbleChange = ::changeBubbleEnabled,
                                onRetry = ::refreshPositions,
                                onDisconnect = ::disconnect,
                                overlaySheetVisible = homeOverlaySheetVisible,
                                onOpenOverlaySettings = { openOverlaySettings(fromHome = true) },
                                onDismissOverlaySheet = { homeOverlaySheetVisible = false },
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
        val overlaysAllowed = Settings.canDrawOverlays(this)
        if (waitingForNotificationSettings) {
            waitingForNotificationSettings = false
            startServiceAndShowHome()
        } else if (waitingForOverlay) {
            val returningToHome = waitingForOverlayFromHome
            waitingForOverlay = false
            waitingForOverlayFromHome = false
            if (overlaysAllowed) {
                overlayStillOff = false
                startWatching()
                if (returningToHome) {
                    homeOverlaySheetVisible = false
                } else {
                    navigate(AppScreen.BubbleOn)
                }
            } else if (returningToHome) {
                prefs.watchingEnabled = false
                bubbleOn = false
                homeOverlaySheetVisible = false
            } else if (screen == AppScreen.OverlayPermission) {
                overlayStillOff = true
                overlaySheetReopenKey += 1
            }
        } else if (overlaysAllowed && appMode != Prefs.MODE_NONE && prefs.watchingEnabled) {
            startWatching()
        } else if (
            !overlaysAllowed &&
            appMode != Prefs.MODE_NONE &&
            prefs.watchingEnabled &&
            screen == AppScreen.Home
        ) {
            bubbleOn = false
            homeOverlaySheetVisible = true
        } else {
            bubbleOn = false
        }
    }

    private fun connectWallet() {
        walletFailed = false
        navigate(AppScreen.Loading)
        lifecycleScope.launch {
            val result = withTimeoutOrNull(WALLET_CONNECT_TIMEOUT_MS) {
                runCatching { adapter.connect(resultSender) }.getOrNull()
            }
            val success = result as? TransactionResult.Success<*>
            val publicKey = success?.authResult?.publicKey
            if (publicKey == null || publicKey.isEmpty()) {
                walletFailed = true
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
        walletFailed = false
        val address = walletAddress ?: return navigateRoot(AppScreen.FirstLaunch, false)
        if (showLoading && screen != AppScreen.Loading) navigate(AppScreen.Loading)
        lifecycleScope.launch {
            var result = reader.positions(address)
            repeat(READ_RETRIES) { attempt ->
                if (result != Reading.CouldNotCheck) return@repeat
                delay(READ_RETRY_DELAY_MS * (attempt + 1))
                result = reader.positions(address)
            }
            reading = result
            if (showLoading) {
                navigate(when (result) {
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
        prefs.watchingEnabled = true
        if (Settings.canDrawOverlays(this)) {
            startWatching()
            navigate(AppScreen.BubbleOn)
        } else {
            navigate(AppScreen.OverlayPermission)
        }
    }

    private fun openOverlaySettings(fromHome: Boolean = false) {
        waitingForOverlay = true
        waitingForOverlayFromHome = fromHome
        if (fromHome) {
            prefs.watchingEnabled = true
            homeOverlaySheetVisible = false
        }
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
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val permissionGranted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (permissionGranted) {
                startServiceAndShowHome()
            } else if (
                !prefs.notificationAsked ||
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                prefs.notificationAsked = true
                notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings()
            }
        } else if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            openNotificationSettings()
        } else {
            startServiceAndShowHome()
        }
    }

    private fun openNotificationSettings() {
        waitingForNotificationSettings = true
        startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName),
        )
    }

    private fun startServiceAndShowHome() {
        startWatching()
        navigateRoot(AppScreen.Home, true)
    }

    private fun startWatching() {
        if (Settings.canDrawOverlays(this) &&
            appMode != Prefs.MODE_NONE &&
            prefs.watchingEnabled
        ) {
            startForegroundService(Intent(this, WatchService::class.java))
            bubbleOn = true
        } else {
            bubbleOn = false
        }
    }

    private fun changeBubbleEnabled(enabled: Boolean) {
        if (!enabled) {
            prefs.watchingEnabled = false
            stopWatching()
        } else if (Settings.canDrawOverlays(this)) {
            prefs.watchingEnabled = true
            startWatching()
        } else {
            prefs.watchingEnabled = false
            bubbleOn = false
            homeOverlaySheetVisible = true
        }
    }

    private fun stopWatching() {
        stopService(Intent(this, WatchService::class.java))
        bubbleOn = false
    }

    private fun disconnect() {
        stopWatching()
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

    private companion object {
        const val WALLET_CONNECT_TIMEOUT_MS = 60_000L
        const val READ_RETRIES = 2
        const val READ_RETRY_DELAY_MS = 1_500L
    }
}
