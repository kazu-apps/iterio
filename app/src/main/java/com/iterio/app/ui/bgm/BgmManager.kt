package com.iterio.app.ui.bgm

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.iterio.app.domain.model.BgmTrack
import com.iterio.app.domain.model.BgmTracks
import com.iterio.app.domain.model.PremiumFeature
import com.iterio.app.domain.repository.PremiumRepository
import com.iterio.app.domain.repository.SettingsRepository
import com.iterio.app.service.BgmService
import com.iterio.app.service.BgmState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BGM機能を管理するファサードクラス
 * Premium機能のチェックとBgmServiceとの連携を行う
 */
@Singleton
class BgmManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val premiumRepository: PremiumRepository,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var bgmService: BgmService? = null
    private var isBound = false

    private val _bgmState = MutableStateFlow(BgmState())
    val bgmState: StateFlow<BgmState> = _bgmState.asStateFlow()

    private val _selectedTrack = MutableStateFlow<BgmTrack?>(null)
    val selectedTrack: StateFlow<BgmTrack?> = _selectedTrack.asStateFlow()

    private val _volume = MutableStateFlow(0.5f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _autoPlayEnabled = MutableStateFlow(true)
    val autoPlayEnabled: StateFlow<Boolean> = _autoPlayEnabled.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? BgmService.BgmBinder ?: return
            bgmService = binder.getService()
            isBound = true

            scope.launch {
                bgmService?.bgmState?.collect { state ->
                    _bgmState.value = state
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            bgmService = null
            isBound = false
        }
    }

    init {
        bindService()
        loadSettings()
    }

    private fun loadSettings() {
        scope.launch {
            val trackId = settingsRepository.getBgmTrackId().getOrNull()
            val volume = settingsRepository.getBgmVolume().getOrDefault(0.5f)
            val autoPlay = settingsRepository.getBgmAutoPlay().getOrDefault(true)

            trackId?.let { id ->
                _selectedTrack.value = BgmTracks.getById(id)
            }
            _volume.value = volume
            _autoPlayEnabled.value = autoPlay
        }
    }

    private fun bindService() {
        val intent = Intent(context, BgmService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * BGM機能が利用可能かどうか（Premium機能）
     */
    suspend fun canUseBgm(): Boolean {
        return premiumRepository.canAccessFeature(PremiumFeature.BGM).getOrDefault(false)
    }

    /**
     * 利用可能なBGMトラック一覧を取得
     */
    fun getAvailableTracks(): List<BgmTrack> = BgmTracks.all

    /**
     * トラックを選択（設定に保存）
     */
    fun selectTrack(track: BgmTrack?) {
        _selectedTrack.value = track
        scope.launch {
            settingsRepository.setBgmTrackId(track?.id)
        }
    }

    /**
     * BGMを再生開始
     */
    fun play(track: BgmTrack) {
        _selectedTrack.value = track
        BgmService.play(context, track.generatorType.name, track.id, _volume.value)
    }

    /**
     * BGMを一時停止
     */
    fun pause() {
        BgmService.pause(context)
    }

    /**
     * BGMを再開
     */
    fun resume() {
        BgmService.resume(context)
    }

    /**
     * BGMを停止
     */
    fun stop() {
        BgmService.stop(context)
        _selectedTrack.value = null
    }

    /**
     * 音量を設定（0.0f〜1.0f）
     */
    fun setVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        _volume.value = clampedVolume
        BgmService.setVolume(context, clampedVolume)
        scope.launch {
            settingsRepository.setBgmVolume(clampedVolume)
        }
    }

    /**
     * 自動再生を設定
     */
    fun setAutoPlay(enabled: Boolean) {
        _autoPlayEnabled.value = enabled
        scope.launch {
            settingsRepository.setBgmAutoPlay(enabled)
        }
    }

    /**
     * 現在再生中かどうか
     */
    fun isPlaying(): Boolean = _bgmState.value.isPlaying

    /**
     * タイマー開始時にBGMも開始（選択されていて自動再生がONの場合）
     */
    fun onTimerStart() {
        if (_autoPlayEnabled.value) {
            _selectedTrack.value?.let { track ->
                play(track)
            }
        }
    }

    /**
     * タイマー一時停止時にBGMも一時停止
     */
    fun onTimerPause() {
        if (isPlaying()) {
            pause()
        }
    }

    /**
     * タイマー再開時にBGMも再開
     */
    fun onTimerResume() {
        if (_selectedTrack.value != null && !isPlaying()) {
            resume()
        }
    }

    /**
     * タイマー終了時にBGMも停止
     */
    fun onTimerStop() {
        stop()
    }

    fun unbind() {
        if (isBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                // Already unbound
            }
            isBound = false
        }
    }
}
