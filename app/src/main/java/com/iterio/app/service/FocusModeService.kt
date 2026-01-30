package com.iterio.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.iterio.app.ui.MainActivity
import com.iterio.app.util.SystemPackages
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Accessibility Service for Focus Mode (Phone Lock during study)
 *
 * This service monitors app switches and blocks access to other apps
 * when focus mode is active.
 */
class FocusModeService : AccessibilityService() {

    companion object {
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _isFocusModeActive = MutableStateFlow(false)
        val isFocusModeActive: StateFlow<Boolean> = _isFocusModeActive.asStateFlow()

        private val _isStrictMode = MutableStateFlow(false)
        val isStrictMode: StateFlow<Boolean> = _isStrictMode.asStateFlow()

        private val _allowedPackages = MutableStateFlow<Set<String>>(emptySet())

        @androidx.annotation.VisibleForTesting
        internal fun getAllowedPackages(): Set<String> = _allowedPackages.value

        @Volatile
        var instance: FocusModeService? = null
            private set

        fun startFocusMode(strictMode: Boolean = false, additionalAllowedPackages: Set<String> = emptySet()) {
            _isFocusModeActive.value = true
            _isStrictMode.value = strictMode
            val basePackages = if (strictMode) {
                SystemPackages.STRICT_MODE_ALLOWED
            } else {
                SystemPackages.ALWAYS_ALLOWED
            }
            _allowedPackages.value = basePackages + additionalAllowedPackages
        }

        fun stopFocusMode() {
            _isFocusModeActive.value = false
            _isStrictMode.value = false
            _allowedPackages.value = emptySet()
        }

        fun isActive(): Boolean = _isFocusModeActive.value
    }

    private var ownPackageName: String = ""

    override fun onServiceConnected() {
        super.onServiceConnected()

        instance = this
        ownPackageName = packageName
        _isServiceRunning.value = true

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!_isFocusModeActive.value) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return

                // Check if the app is allowed
                if (!isPackageAllowed(packageName)) {
                    // Block by returning to our app
                    returnToApp()
                }
            }
        }
    }

    private fun isPackageAllowed(packageName: String): Boolean {
        // Our app is always allowed
        if (packageName == ownPackageName) return true

        // Check allowed packages
        return _allowedPackages.value.contains(packageName)
    }

    private fun returnToApp() {
        try {
            // Launch our main activity to bring user back
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch MainActivity")
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceRunning.value = false
        _isFocusModeActive.value = false
    }
}
