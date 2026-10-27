package com.example.applock.monitoring

import com.example.applock.data.models.ReLockPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LockStateManagerTest {

    private val targetApp = "com.example.targetapp"
    private val selfApp = "com.example.applock"
    private val otherApp = "com.example.otherapp"

    @Before
    fun setUp() {
        LockStateManager.resetSessions()
    }

    @Test
    fun testAppInitiallyLocked() {
        assertTrue(LockStateManager.shouldLock(targetApp, ReLockPolicy.AFTER_LEAVING))
    }

    @Test
    fun testSelfLockingRequiresLockWhenNotAuthenticated() {
        assertTrue(LockStateManager.shouldLock(selfApp, ReLockPolicy.AFTER_LEAVING))
    }

    @Test
    fun testSelfLockingUnlockedWhenAuthenticated() {
        LockStateManager.onAppUnlocked(selfApp)
        assertFalse(LockStateManager.shouldLock(selfApp, ReLockPolicy.AFTER_LEAVING))
    }

    @Test
    fun testSamsungSecureFolderExclusion() {
        val samsungSecureFolder = "com.samsung.knox.securefolder"
        val samsungSecProtect = "com.sec.android.app.secprotect"
        val samsungKnoxContainer = "com.samsung.android.knox.containercore"

        assertTrue(AppLockService.isExcludedSystemContainer(samsungSecureFolder))
        assertTrue(AppLockService.isExcludedSystemContainer(samsungSecProtect))
        assertTrue(AppLockService.isExcludedSystemContainer(samsungKnoxContainer))
        assertFalse(AppLockService.isExcludedSystemContainer(targetApp))
    }

    @Test
    fun testAppUnlockedWhenAuthenticated() {
        LockStateManager.onAppUnlocked(targetApp)
        assertFalse(LockStateManager.shouldLock(targetApp, ReLockPolicy.AFTER_LEAVING))
    }

    @Test
    fun testAfterLeavingPolicyRelocksWhenSwitched() {
        LockStateManager.onAppUnlocked(targetApp)
        LockStateManager.onForegroundPackageChanged(targetApp, ReLockPolicy.AFTER_LEAVING)
        assertFalse(LockStateManager.shouldLock(targetApp, ReLockPolicy.AFTER_LEAVING))

        // Switch to another app
        LockStateManager.onForegroundPackageChanged(otherApp, ReLockPolicy.AFTER_LEAVING)

        // Now targetApp should require lock again
        assertTrue(LockStateManager.shouldLock(targetApp, ReLockPolicy.AFTER_LEAVING))
    }

    @Test
    fun testTimeoutPolicyRemainsUnlockedWithinWindow() {
        LockStateManager.onAppUnlocked(targetApp)
        LockStateManager.onForegroundPackageChanged(targetApp, ReLockPolicy.SECONDS_30)

        // Switch away from targetApp
        LockStateManager.onForegroundPackageChanged(otherApp, ReLockPolicy.SECONDS_30)

        // Immediately checking targetApp should still be unlocked
        assertFalse(LockStateManager.shouldLock(targetApp, ReLockPolicy.SECONDS_30))
    }
}
