package com.iterio.app.ui.bgm

import android.content.Context
import com.iterio.app.domain.common.Result
import com.iterio.app.domain.model.BgmTrack
import com.iterio.app.domain.model.BgmTracks
import com.iterio.app.domain.model.PremiumFeature
import com.iterio.app.domain.repository.PremiumRepository
import com.iterio.app.domain.repository.SettingsRepository
import com.iterio.app.service.BgmService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BgmManager のユニットテスト
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BgmManagerTest {

    private lateinit var context: Context
    private lateinit var premiumRepository: PremiumRepository
    private lateinit var settingsRepository: SettingsRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        premiumRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        // Mock BgmService companion object static methods
        mockkObject(BgmService.Companion)
        every { BgmService.play(any(), any<String>(), any(), any()) } returns Unit
        every { BgmService.pause(any()) } returns Unit
        every { BgmService.resume(any()) } returns Unit
        every { BgmService.stop(any()) } returns Unit
        every { BgmService.setVolume(any(), any()) } returns Unit

        // Default mock responses
        coEvery { settingsRepository.getBgmTrackId() } returns Result.Success(null)
        coEvery { settingsRepository.getBgmVolume() } returns Result.Success(0.5f)
        coEvery { settingsRepository.getBgmAutoPlay() } returns Result.Success(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(BgmService.Companion)
    }

    private fun createBgmManager(): BgmManager {
        return BgmManager(context, premiumRepository, settingsRepository)
    }

    @Test
    fun `setVolume clamps value when below 0`() = runTest {
        // Arrange
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Act
        bgmManager.setVolume(-0.5f)
        advanceUntilIdle()

        // Assert
        assertEquals("音量が0にクランプされるべき", 0f, bgmManager.volume.value)
        coVerify { settingsRepository.setBgmVolume(0f) }
    }

    @Test
    fun `setVolume clamps value when above 1`() = runTest {
        // Arrange
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Act
        bgmManager.setVolume(1.5f)
        advanceUntilIdle()

        // Assert
        assertEquals("音量が1にクランプされるべき", 1f, bgmManager.volume.value)
        coVerify { settingsRepository.setBgmVolume(1f) }
    }

    @Test
    fun `setVolume accepts valid value`() = runTest {
        // Arrange
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Act
        bgmManager.setVolume(0.7f)
        advanceUntilIdle()

        // Assert
        assertEquals("音量が正しく設定されるべき", 0.7f, bgmManager.volume.value)
        coVerify { settingsRepository.setBgmVolume(0.7f) }
    }

    @Test
    fun `selectTrack updates selectedTrack and saves to settings`() = runTest {
        // Arrange
        val bgmManager = createBgmManager()
        advanceUntilIdle()
        val track = BgmTracks.all.first()

        // Act
        bgmManager.selectTrack(track)
        advanceUntilIdle()

        // Assert
        assertEquals("トラックが選択されるべき", track, bgmManager.selectedTrack.value)
        coVerify { settingsRepository.setBgmTrackId(track.id) }
    }

    @Test
    fun `selectTrack with null clears selection and saves null`() = runTest {
        // Arrange
        coEvery { settingsRepository.getBgmTrackId() } returns Result.Success("rain")
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Act
        bgmManager.selectTrack(null)
        advanceUntilIdle()

        // Assert
        assertNull("トラック選択がクリアされるべき", bgmManager.selectedTrack.value)
        coVerify { settingsRepository.setBgmTrackId(null) }
    }

    @Test
    fun `setAutoPlay updates state and saves to settings`() = runTest {
        // Arrange
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Act
        bgmManager.setAutoPlay(false)
        advanceUntilIdle()

        // Assert
        assertFalse("自動再生がOFFになるべき", bgmManager.autoPlayEnabled.value)
        coVerify { settingsRepository.setBgmAutoPlay(false) }
    }

    @Test
    fun `loadSettings restores track and volume on init`() = runTest {
        // Arrange
        coEvery { settingsRepository.getBgmTrackId() } returns Result.Success("forest")
        coEvery { settingsRepository.getBgmVolume() } returns Result.Success(0.8f)
        coEvery { settingsRepository.getBgmAutoPlay() } returns Result.Success(false)

        // Act
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Assert
        assertEquals("トラックが復元されるべき", BgmTracks.getById("forest"), bgmManager.selectedTrack.value)
        assertEquals("音量が復元されるべき", 0.8f, bgmManager.volume.value)
        assertFalse("自動再生設定が復元されるべき", bgmManager.autoPlayEnabled.value)
    }

    @Test
    fun `canUseBgm returns true when premium user`() = runTest {
        // Arrange
        coEvery { premiumRepository.canAccessFeature(PremiumFeature.BGM) } returns Result.Success(true)
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Act
        val canUse = bgmManager.canUseBgm()

        // Assert
        assertTrue("Premium ユーザーはBGMを使用可能", canUse)
    }

    @Test
    fun `canUseBgm returns false when free user`() = runTest {
        // Arrange
        coEvery { premiumRepository.canAccessFeature(PremiumFeature.BGM) } returns Result.Success(false)
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Act
        val canUse = bgmManager.canUseBgm()

        // Assert
        assertFalse("無料ユーザーはBGMを使用不可", canUse)
    }

    @Test
    fun `getAvailableTracks returns all tracks`() = runTest {
        // Arrange
        val bgmManager = createBgmManager()
        advanceUntilIdle()

        // Act
        val tracks = bgmManager.getAvailableTracks()

        // Assert
        assertEquals("全てのトラックが返されるべき", BgmTracks.all, tracks)
    }
}
