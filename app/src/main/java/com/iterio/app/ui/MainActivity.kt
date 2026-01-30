package com.iterio.app.ui

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.iterio.app.service.FocusModeService
import com.iterio.app.ui.components.NotificationPermissionDialog
import com.iterio.app.ui.navigation.BottomNavigationBar
import com.iterio.app.ui.navigation.Screen
import com.iterio.app.ui.navigation.IterioNavHost
import com.iterio.app.ui.premium.PremiumManager
import com.iterio.app.ui.premium.TrialOfferDialog
import com.iterio.app.ui.theme.IterioTheme
import com.iterio.app.util.LocaleManager
import kotlinx.coroutines.flow.collectLatest
import com.iterio.app.util.NotificationPermissionHelper
import com.iterio.app.util.NotificationPermissionPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var premiumManager: PremiumManager

    private lateinit var notificationPermissionPrefs: NotificationPermissionPrefs

    private var isImmersiveModeActive = false

    /**
     * Apply language settings synchronously before Activity is created.
     * This ensures the correct locale is set before any UI is inflated.
     */
    override fun attachBaseContext(newBase: Context) {
        val language = LocaleManager.Sync.getLanguage(newBase)
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        lifecycleScope.launch {
            notificationPermissionPrefs.setPermissionRequested(true)
            if (!isGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val shouldShowRationale = shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (!shouldShowRationale) {
                        notificationPermissionPrefs.setPermissionDeniedPermanently(true)
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        notificationPermissionPrefs = NotificationPermissionPrefs(this)

        enableEdgeToEdge()
        observeStrictMode()
        setContent {
            IterioTheme {
                var showPermissionDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val hasRequested = notificationPermissionPrefs.hasRequestedPermission.first()
                        val isGranted = NotificationPermissionHelper.isNotificationPermissionGranted(
                            this@MainActivity
                        )

                        if (!hasRequested && !isGranted) {
                            showPermissionDialog = true
                        }
                    }
                }

                if (showPermissionDialog) {
                    NotificationPermissionDialog(
                        onConfirm = {
                            showPermissionDialog = false
                            requestNotificationPermission()
                        },
                        onDismiss = {
                            showPermissionDialog = false
                            lifecycleScope.launch {
                                notificationPermissionPrefs.setPermissionRequested(true)
                            }
                        }
                    )
                }

                // Trial offer dialog
                var showTrialOfferDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    premiumManager.subscriptionStatus.collectLatest { status ->
                        if (!status.hasSeenTrialOffer && !status.isTrialUsed && !status.isPremium) {
                            showTrialOfferDialog = true
                        }
                    }
                }

                if (showTrialOfferDialog) {
                    TrialOfferDialog(
                        onStartTrial = {
                            showTrialOfferDialog = false
                            lifecycleScope.launch {
                                premiumManager.startTrial()
                            }
                        },
                        onDismiss = {
                            showTrialOfferDialog = false
                            lifecycleScope.launch {
                                premiumManager.markTrialOfferSeen()
                            }
                        }
                    )
                }

                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavigationBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    IterioNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun observeStrictMode() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                FocusModeService.isStrictMode.collectLatest { strictMode ->
                    if (strictMode) {
                        isImmersiveModeActive = true
                        enterImmersiveMode()
                    } else if (isImmersiveModeActive) {
                        isImmersiveModeActive = false
                        exitImmersiveMode()
                    }
                }
            }
        }
    }

    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    private fun exitImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        enableEdgeToEdge()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && isImmersiveModeActive) {
            enterImmersiveMode()
        }
    }
}
