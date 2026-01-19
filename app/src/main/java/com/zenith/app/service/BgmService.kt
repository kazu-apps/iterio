package com.zenith.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import androidx.annotation.RawRes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * BGM再生を管理するサービス
 * タイマーと連携してバックグラウンドでBGMを再生
 */
@AndroidEntryPoint
class BgmService : Service() {

    private val binder = BgmBinder()
    private var mediaPlayer: MediaPlayer? = null

    private val _bgmState = MutableStateFlow(BgmState())
    val bgmState: StateFlow<BgmState> = _bgmState.asStateFlow()

    inner class BgmBinder : Binder() {
        fun getService(): BgmService = this@BgmService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_NOT_STICKY
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_PLAY -> {
                val resourceId = intent.getIntExtra(EXTRA_RESOURCE_ID, 0)
                val trackId = intent.getStringExtra(EXTRA_TRACK_ID) ?: ""
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.5f)
                if (resourceId != 0) {
                    playBgm(resourceId, trackId, volume)
                }
            }
            ACTION_PAUSE -> pauseBgm()
            ACTION_RESUME -> resumeBgm()
            ACTION_STOP -> stopBgm()
            ACTION_SET_VOLUME -> {
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.5f)
                setVolume(volume)
            }
        }
    }

    private fun playBgm(@RawRes resourceId: Int, trackId: String, volume: Float) {
        // 既存のMediaPlayerを解放
        releaseMediaPlayer()

        try {
            mediaPlayer = MediaPlayer.create(this, resourceId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                isLooping = true
                setVolume(volume, volume)
                start()
            }

            _bgmState.value = BgmState(
                isPlaying = true,
                currentTrackId = trackId,
                volume = volume
            )
        } catch (e: Exception) {
            _bgmState.value = BgmState(error = e.message)
        }
    }

    private fun pauseBgm() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _bgmState.value = _bgmState.value.copy(isPlaying = false)
            }
        }
    }

    private fun resumeBgm() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _bgmState.value = _bgmState.value.copy(isPlaying = true)
            }
        }
    }

    private fun stopBgm() {
        releaseMediaPlayer()
        _bgmState.value = BgmState()
    }

    private fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(clampedVolume, clampedVolume)
        _bgmState.value = _bgmState.value.copy(volume = clampedVolume)
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            android.util.Log.e("BgmService", "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    override fun onDestroy() {
        releaseMediaPlayer()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.zenith.app.action.BGM_PLAY"
        const val ACTION_PAUSE = "com.zenith.app.action.BGM_PAUSE"
        const val ACTION_RESUME = "com.zenith.app.action.BGM_RESUME"
        const val ACTION_STOP = "com.zenith.app.action.BGM_STOP"
        const val ACTION_SET_VOLUME = "com.zenith.app.action.BGM_SET_VOLUME"

        const val EXTRA_RESOURCE_ID = "resource_id"
        const val EXTRA_TRACK_ID = "track_id"
        const val EXTRA_VOLUME = "volume"

        fun play(context: Context, @RawRes resourceId: Int, trackId: String, volume: Float = 0.5f) {
            val intent = Intent(context, BgmService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_RESOURCE_ID, resourceId)
                putExtra(EXTRA_TRACK_ID, trackId)
                putExtra(EXTRA_VOLUME, volume)
            }
            context.startService(intent)
        }

        fun pause(context: Context) {
            val intent = Intent(context, BgmService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, BgmService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BgmService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun setVolume(context: Context, volume: Float) {
            val intent = Intent(context, BgmService::class.java).apply {
                action = ACTION_SET_VOLUME
                putExtra(EXTRA_VOLUME, volume)
            }
            context.startService(intent)
        }
    }
}

/**
 * BGMの状態
 */
data class BgmState(
    val isPlaying: Boolean = false,
    val currentTrackId: String? = null,
    val volume: Float = 0.5f,
    val error: String? = null
)
