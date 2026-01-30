package com.iterio.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.iterio.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

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

        private const val ACTION_SHOW = "com.iterio.app.action.SHOW_OVERLAY"
        private const val ACTION_HIDE = "com.iterio.app.action.HIDE_OVERLAY"
        private const val ACTION_UPDATE = "com.iterio.app.action.UPDATE_OVERLAY"
        private const val EXTRA_TIME = "extra_time"
        private const val EXTRA_PHASE = "extra_phase"

        private const val TAP_TIMEOUT_MS = 300L
        private const val TAP_SLOP_PX = 24f
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentLayoutParams: WindowManager.LayoutParams? = null

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

    private fun createLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupTouchHandler(view: View) {
        var downTime = 0L
        var downX = 0f
        var downY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = event.eventTime
                    downX = event.x
                    downY = event.y
                }
                MotionEvent.ACTION_UP -> {
                    val elapsed = event.eventTime - downTime
                    val dx = abs(event.x - downX)
                    val dy = abs(event.y - downY)
                    if (elapsed < TAP_TIMEOUT_MS && dx < TAP_SLOP_PX && dy < TAP_SLOP_PX) {
                        launchApp()
                    }
                }
            }
            true
        }
    }

    private fun launchApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        launchIntent?.let { startActivity(it) }
    }

    private fun showOverlayView() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager
        if (windowManager == null) return

        val layoutParams = createLayoutParams()
        currentLayoutParams = layoutParams

        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_lock_screen, null)

        overlayView?.let { view ->
            setupTouchHandler(view)
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
            _isOverlayShowing.value = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to add overlay view")
            overlayView = null
            currentLayoutParams = null
            windowManager = null
            _isOverlayShowing.value = false
        }
    }

    private fun hideOverlayView() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove overlay view")
            }
        }
        overlayView = null
        currentLayoutParams = null
        _isOverlayShowing.value = false
        stopSelf()
    }

    private fun updateOverlayView(time: String, phase: String) {
        overlayView?.let { view ->
            view.findViewById<TextView>(R.id.overlay_time_text)?.text = time
            view.findViewById<TextView>(R.id.overlay_phase_text)?.text = phase
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val view = overlayView ?: return
        val wm = windowManager ?: return

        val savedTime = view.findViewById<TextView>(R.id.overlay_time_text)?.text?.toString() ?: currentTimeText
        val savedPhase = view.findViewById<TextView>(R.id.overlay_phase_text)?.text?.toString() ?: currentPhaseText

        val newView = LayoutInflater.from(this).inflate(R.layout.overlay_lock_screen, null)
        newView.findViewById<TextView>(R.id.overlay_time_text)?.text = savedTime
        newView.findViewById<TextView>(R.id.overlay_phase_text)?.text = savedPhase
        setupTouchHandler(newView)
        val newParams = createLayoutParams()

        try {
            wm.removeView(view)
            wm.addView(newView, newParams)
            currentLayoutParams = newParams
            overlayView = newView
        } catch (e: Exception) {
            Timber.e(e, "Failed to recreate overlay view on configuration change")
            overlayView = null
            currentLayoutParams = null
            windowManager = null
            _isOverlayShowing.value = false
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlayView()
    }
}
