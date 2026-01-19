package com.zenith.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.zenith.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service that displays a fullscreen overlay during complete lock mode
 * to prevent users from switching to other apps.
 */
class LockOverlayService : Service() {

    companion object {
        private val _isOverlayShowing = MutableStateFlow(false)
        val isOverlayShowing: StateFlow<Boolean> = _isOverlayShowing.asStateFlow()

        @Volatile
        private var currentTimeText: String = ""
        @Volatile
        private var currentPhaseText: String = ""

        fun canDrawOverlays(context: Context): Boolean {
            return Settings.canDrawOverlays(context)
        }

        fun requestOverlayPermission(context: Context) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun showOverlay(context: Context) {
            if (!canDrawOverlays(context)) return
            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_SHOW
            }
            context.startService(intent)
        }

        fun hideOverlay(context: Context) {
            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_HIDE
            }
            context.startService(intent)
        }

        fun updateOverlayTime(context: Context, time: String, phase: String) {
            currentTimeText = time
            currentPhaseText = phase
            val intent = Intent(context, LockOverlayService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TIME, time)
                putExtra(EXTRA_PHASE, phase)
            }
            context.startService(intent)
        }

        private const val ACTION_SHOW = "com.zenith.app.action.SHOW_OVERLAY"
        private const val ACTION_HIDE = "com.zenith.app.action.HIDE_OVERLAY"
        private const val ACTION_UPDATE = "com.zenith.app.action.UPDATE_OVERLAY"
        private const val EXTRA_TIME = "extra_time"
        private const val EXTRA_PHASE = "extra_phase"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showOverlayView()
            ACTION_HIDE -> hideOverlayView()
            ACTION_UPDATE -> {
                val time = intent.getStringExtra(EXTRA_TIME) ?: currentTimeText
                val phase = intent.getStringExtra(EXTRA_PHASE) ?: currentPhaseText
                updateOverlayView(time, phase)
            }
        }
        return START_STICKY
    }

    private fun showOverlayView() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) return

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_lock_screen, null)

        overlayView?.setOnClickListener {
            // Clicking the overlay opens the app
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            launchIntent?.let { startActivity(it) }
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
            _isOverlayShowing.value = true
        } catch (e: Exception) {
            android.util.Log.e("LockOverlayService", "Failed to add overlay view", e)
        }
    }

    private fun hideOverlayView() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                android.util.Log.e("LockOverlayService", "Failed to remove overlay view", e)
            }
        }
        overlayView = null
        _isOverlayShowing.value = false
        stopSelf()
    }

    private fun updateOverlayView(time: String, phase: String) {
        overlayView?.let { view ->
            view.findViewById<TextView>(R.id.overlay_time_text)?.text = time
            view.findViewById<TextView>(R.id.overlay_phase_text)?.text = phase
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlayView()
    }
}
