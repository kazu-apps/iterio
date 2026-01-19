package com.zenith.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zenith.app.ui.components.NotificationPermissionDialog
import com.zenith.app.ui.navigation.BottomNavigationBar
import com.zenith.app.ui.navigation.Screen
import com.zenith.app.ui.navigation.ZenithNavHost
import com.zenith.app.ui.premium.PremiumManager
import com.zenith.app.ui.premium.TrialOfferDialog
import com.zenith.app.ui.theme.ZenithTheme
import kotlinx.coroutines.flow.collectLatest
import com.zenith.app.util.NotificationPermissionHelper
import com.zenith.app.util.NotificationPermissionPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var premiumManager: PremiumManager

    private lateinit var notificationPermissionPrefs: NotificationPermissionPrefs

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
        setContent {
            ZenithTheme {
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
                    ZenithNavHost(
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
}
