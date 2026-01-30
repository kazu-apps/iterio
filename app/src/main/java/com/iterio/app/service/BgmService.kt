package com.iterio.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Binder
import android.os.IBinder
import com.iterio.app.domain.model.AudioGeneratorType
import com.iterio.app.service.audio.AudioGenerator
import com.iterio.app.service.audio.AudioGeneratorFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * BGM再生を管理するサービス
 * AudioTrack + プロシージャル音声生成でバックグラウンドBGMを再生
 */
@AndroidEntryPoint
class BgmService : Service() {

    private val binder = BgmBinder()
    private var audioTrack: AudioTrack? = null
    private var generatorThread: Thread? = null
    private var generator: AudioGenerator? = null
    @Volatile private var isGenerating = false

    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private var audioFocusRequest: AudioFocusRequest? = null

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
                val generatorTypeName = intent.getStringExtra(EXTRA_GENERATOR_TYPE) ?: return
                val trackId = intent.getStringExtra(EXTRA_TRACK_ID) ?: ""
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.5f)
                try {
                    val generatorType = AudioGeneratorType.valueOf(generatorTypeName)
                    playBgm(generatorType, trackId, volume)
                } catch (e: IllegalArgumentException) {
                    Timber.e(e, "Unknown generator type: $generatorTypeName")
                    _bgmState.value = BgmState(error = "不明なBGMタイプです")
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

    private fun playBgm(generatorType: AudioGeneratorType, trackId: String, volume: Float) {
        stopBgm()

        try {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (bufferSize <= 0) {
                Timber.e("Invalid buffer size: $bufferSize")
                _bgmState.value = BgmState(error = "オーディオの初期化に失敗しました")
                return
            }

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            generator = AudioGeneratorFactory.create(generatorType, sampleRate).apply {
                setVolume(volume)
            }

            requestAudioFocus()
            audioTrack?.play()
            isGenerating = true

            generatorThread = Thread({
                val buffer = ShortArray(bufferSize / 2)
                while (isGenerating) {
                    generator?.generate(buffer)
                    try {
                        audioTrack?.write(buffer, 0, buffer.size)
                    } catch (e: IllegalStateException) {
                        Timber.e(e, "AudioTrack write failed")
                        break
                    }
                }
            }, "BgmGeneratorThread").apply { start() }

            _bgmState.value = BgmState(
                isPlaying = true,
                currentTrackId = trackId,
                volume = volume
            )
        } catch (e: Exception) {
            Timber.e(e, "Error starting BGM")
            _bgmState.value = BgmState(error = e.message)
        }
    }

    private fun pauseBgm() {
        if (_bgmState.value.isPlaying) {
            isGenerating = false
            generatorThread?.join(1000)
            generatorThread = null
            try {
                audioTrack?.pause()
            } catch (e: IllegalStateException) {
                Timber.e(e, "Error pausing AudioTrack")
            }
            _bgmState.value = _bgmState.value.copy(isPlaying = false)
        }
    }

    private fun resumeBgm() {
        if (!_bgmState.value.isPlaying && audioTrack != null && generator != null) {
            try {
                audioTrack?.play()
                isGenerating = true

                val bufferSize = AudioTrack.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                generatorThread = Thread({
                    val buffer = ShortArray(bufferSize / 2)
                    while (isGenerating) {
                        generator?.generate(buffer)
                        try {
                            audioTrack?.write(buffer, 0, buffer.size)
                        } catch (e: IllegalStateException) {
                            Timber.e(e, "AudioTrack write failed on resume")
                            break
                        }
                    }
                }, "BgmGeneratorThread").apply { start() }

                _bgmState.value = _bgmState.value.copy(isPlaying = true)
            } catch (e: Exception) {
                Timber.e(e, "Error resuming BGM")
            }
        }
    }

    private fun stopBgm() {
        isGenerating = false
        generatorThread?.join(1000)
        generatorThread = null

        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.stop()
                }
                track.release()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error stopping AudioTrack")
        } finally {
            audioTrack = null
            generator = null
        }

        abandonAudioFocus()
        _bgmState.value = BgmState()
    }

    private fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        generator?.setVolume(clampedVolume)
        _bgmState.value = _bgmState.value.copy(volume = clampedVolume)
    }

    private fun requestAudioFocus() {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .setOnAudioFocusChangeListener { focusChange ->
                when (focusChange) {
                    AudioManager.AUDIOFOCUS_LOSS -> stopBgm()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pauseBgm()
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                        generator?.setVolume(_bgmState.value.volume * 0.3f)
                    AudioManager.AUDIOFOCUS_GAIN ->
                        generator?.setVolume(_bgmState.value.volume)
                }
            }
            .build()
        audioFocusRequest = focusRequest
        audioManager.requestAudioFocus(focusRequest)
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    override fun onDestroy() {
        stopBgm()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.iterio.app.action.BGM_PLAY"
        const val ACTION_PAUSE = "com.iterio.app.action.BGM_PAUSE"
        const val ACTION_RESUME = "com.iterio.app.action.BGM_RESUME"
        const val ACTION_STOP = "com.iterio.app.action.BGM_STOP"
        const val ACTION_SET_VOLUME = "com.iterio.app.action.BGM_SET_VOLUME"

        const val EXTRA_GENERATOR_TYPE = "generator_type"
        const val EXTRA_TRACK_ID = "track_id"
        const val EXTRA_VOLUME = "volume"

        fun play(context: Context, generatorType: String, trackId: String, volume: Float = 0.5f) {
            val intent = Intent(context, BgmService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_GENERATOR_TYPE, generatorType)
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
