package com.iterio.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.iterio.app.R
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
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"

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
                SystemPackages.SOFT_MODE_ALLOWED
            }
            _allowedPackages.value = basePackages + additionalAllowedPackages
            Timber.d("Focus mode started: strict=$strictMode, allowedPackages=${_allowedPackages.value}")
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

                // Layer 2: collapse system UI panel immediately in strict mode
                if (_isStrictMode.value && packageName == SYSTEM_UI_PACKAGE) {
                    Timber.d("Strict mode: collapsing system panel")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    return
                }

                // Check if the app is allowed
                val allowed = isPackageAllowed(packageName)
                Timber.d("Focus mode event: package=$packageName, allowed=$allowed, strict=${_isStrictMode.value}")
                if (!allowed) {
                    if (!_isStrictMode.value) {
                        showBlockedWarning()
                    }
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

    private fun showBlockedWarning() {
        try {
            android.widget.Toast.makeText(
                this,
                getString(R.string.focus_mode_blocked_message),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show blocked warning toast")
        }
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
