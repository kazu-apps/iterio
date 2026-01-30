package com.iterio.app.service

import com.iterio.app.util.SystemPackages
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * FocusModeService のユニットテスト
 *
 * FocusModeService は AccessibilityService を継承しているため、
 * ここでは主に companion object の static メソッドと SystemPackages の定数をテストする。
 */
class FocusModeServiceTest {

    @Before
    fun setUp() {
        FocusModeService.stopFocusMode()
    }

    @After
    fun tearDown() {
        FocusModeService.stopFocusMode()
    }

    @Test
    fun `STRICT_MODE_ALLOWED excludes all launchers`() {
        for (launcher in SystemPackages.LAUNCHERS) {
            assertFalse(
                "STRICT_MODE_ALLOWED should not contain launcher: $launcher",
                SystemPackages.STRICT_MODE_ALLOWED.contains(launcher)
            )
        }
    }

    @Test
    fun `ALWAYS_ALLOWED includes launchers`() {
        for (launcher in SystemPackages.LAUNCHERS) {
            assertTrue(
                "ALWAYS_ALLOWED should contain launcher: $launcher",
                SystemPackages.ALWAYS_ALLOWED.contains(launcher)
            )
        }
    }

    @Test
    fun `STRICT_MODE_ALLOWED contains SYSTEM_UI and EMERGENCY`() {
        for (pkg in SystemPackages.SYSTEM_UI) {
            assertTrue(
                "STRICT_MODE_ALLOWED should contain SYSTEM_UI: $pkg",
                SystemPackages.STRICT_MODE_ALLOWED.contains(pkg)
            )
        }
        for (pkg in SystemPackages.EMERGENCY) {
            assertTrue(
                "STRICT_MODE_ALLOWED should contain EMERGENCY: $pkg",
                SystemPackages.STRICT_MODE_ALLOWED.contains(pkg)
            )
        }
    }

    @Test
    fun `startFocusMode with strictMode uses STRICT_MODE_ALLOWED`() {
        FocusModeService.startFocusMode(strictMode = true)

        assertTrue(FocusModeService.isFocusModeActive.value)
        assertTrue(FocusModeService.isStrictMode.value)

        val allowedPackages = FocusModeService.getAllowedPackages()
        assertEquals(SystemPackages.STRICT_MODE_ALLOWED, allowedPackages)

        for (launcher in SystemPackages.LAUNCHERS) {
            assertFalse(
                "Strict mode allowed packages should not contain launcher: $launcher",
                allowedPackages.contains(launcher)
            )
        }
    }

    @Test
    fun `startFocusMode without strictMode uses ALWAYS_ALLOWED`() {
        FocusModeService.startFocusMode(strictMode = false)

        assertTrue(FocusModeService.isFocusModeActive.value)
        assertFalse(FocusModeService.isStrictMode.value)

        val allowedPackages = FocusModeService.getAllowedPackages()
        assertEquals(SystemPackages.ALWAYS_ALLOWED, allowedPackages)

        for (launcher in SystemPackages.LAUNCHERS) {
            assertTrue(
                "Non-strict mode allowed packages should contain launcher: $launcher",
                allowedPackages.contains(launcher)
            )
        }
    }

    @Test
    fun `additional allowed packages are merged in strict mode`() {
        val additionalApps = setOf("com.example.allowed")
        FocusModeService.startFocusMode(strictMode = true, additionalAllowedPackages = additionalApps)

        assertTrue(FocusModeService.isFocusModeActive.value)
        assertTrue(FocusModeService.isStrictMode.value)

        val allowedPackages = FocusModeService.getAllowedPackages()
        val expected = SystemPackages.STRICT_MODE_ALLOWED + additionalApps
        assertEquals(expected, allowedPackages)
        assertTrue(allowedPackages.contains("com.example.allowed"))
    }

    @Test
    fun `stopFocusMode clears active and strict mode and allowed packages`() {
        FocusModeService.startFocusMode(strictMode = true)
        assertTrue(FocusModeService.isFocusModeActive.value)
        assertTrue(FocusModeService.isStrictMode.value)
        assertTrue(FocusModeService.getAllowedPackages().isNotEmpty())

        FocusModeService.stopFocusMode()
        assertFalse(FocusModeService.isFocusModeActive.value)
        assertFalse(FocusModeService.isStrictMode.value)
        assertTrue(FocusModeService.getAllowedPackages().isEmpty())
    }

    @Test
    fun `isActive returns correct value`() {
        assertFalse(FocusModeService.isActive())

        FocusModeService.startFocusMode()
        assertTrue(FocusModeService.isActive())

        FocusModeService.stopFocusMode()
        assertFalse(FocusModeService.isActive())
    }
}
