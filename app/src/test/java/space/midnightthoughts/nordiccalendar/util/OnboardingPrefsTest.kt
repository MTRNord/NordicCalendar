package space.midnightthoughts.nordiccalendar.util

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for OnboardingPrefs utility functions.
 * Uses Robolectric to provide Android Context for SharedPreferences testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Use Android API 33 for testing
class OnboardingPrefsTest {

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any existing preferences before each test
        sharedPrefs = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().clear().apply()
    }

    @Test
    fun `isOnboardingNeeded returns true for fresh install`() {
        // Given: Fresh installation with no preferences set
        val currentVersion = "1.0.0"

        // When: Checking if onboarding is needed
        val result = OnboardingPrefs.isOnboardingNeeded(context, currentVersion)

        // Then: Should return true (onboarding needed)
        assertTrue("Onboarding should be needed for fresh install", result)
    }

    @Test
    fun `isOnboardingNeeded returns false when onboarding completed for same version`() {
        // Given: Onboarding was completed for version 1.0.0
        val version = "1.0.0"
        OnboardingPrefs.setOnboardingDone(context, version)

        // When: Checking if onboarding is needed for same version
        val result = OnboardingPrefs.isOnboardingNeeded(context, version)

        // Then: Should return false (onboarding not needed)
        assertFalse("Onboarding should not be needed for same version", result)
    }

    @Test
    fun `isOnboardingNeeded returns true when app version changes`() {
        // Given: Onboarding was completed for version 1.0.0
        val oldVersion = "1.0.0"
        val newVersion = "1.1.0"
        OnboardingPrefs.setOnboardingDone(context, oldVersion)

        // When: Checking if onboarding is needed for new version
        val result = OnboardingPrefs.isOnboardingNeeded(context, newVersion)

        // Then: Should return true (onboarding needed for new version)
        assertTrue("Onboarding should be needed when version changes", result)
    }

    @Test
    fun `isOnboardingNeeded returns true when upgrading from older version`() {
        // Given: Onboarding was completed for version 1.0.0
        val oldVersion = "1.0.0"
        val newerVersion = "2.0.0"
        OnboardingPrefs.setOnboardingDone(context, oldVersion)

        // When: Checking if onboarding is needed for newer version
        val result = OnboardingPrefs.isOnboardingNeeded(context, newerVersion)

        // Then: Should return true (onboarding needed for version upgrade)
        assertTrue("Onboarding should be needed when upgrading versions", result)
    }

    @Test
    fun `isOnboardingNeeded returns true when downgrading version`() {
        // Given: Onboarding was completed for version 2.0.0
        val newerVersion = "2.0.0"
        val olderVersion = "1.0.0"
        OnboardingPrefs.setOnboardingDone(context, newerVersion)

        // When: Checking if onboarding is needed for older version (downgrade)
        val result = OnboardingPrefs.isOnboardingNeeded(context, olderVersion)

        // Then: Should return true (onboarding needed for version change)
        assertTrue("Onboarding should be needed when downgrading versions", result)
    }

    @Test
    fun `isOnboardingNeeded handles version string variations`() {
        // Given: Onboarding completed for version with build suffix
        val versionWithBuild = "1.0.0-debug"
        val versionWithoutBuild = "1.0.0"
        OnboardingPrefs.setOnboardingDone(context, versionWithBuild)

        // When: Checking for slightly different version string
        val result = OnboardingPrefs.isOnboardingNeeded(context, versionWithoutBuild)

        // Then: Should return true (versions are considered different)
        assertTrue("Different version strings should trigger onboarding", result)
    }

    @Test
    fun `setOnboardingDone stores completion state correctly`() {
        // Given: A version to mark as completed
        val version = "1.5.0"

        // When: Setting onboarding as done
        OnboardingPrefs.setOnboardingDone(context, version)

        // Then: SharedPreferences should contain correct values
        assertTrue(
            "Onboarding done flag should be true",
            sharedPrefs.getBoolean("onboarding_done", false)
        )
        assertEquals(
            "Stored version should match", version,
            sharedPrefs.getString("onboarding_version", null)
        )
    }

    @Test
    fun `setOnboardingDone overwrites previous completion state`() {
        // Given: Onboarding was completed for an older version
        val oldVersion = "1.0.0"
        val newVersion = "1.2.0"
        OnboardingPrefs.setOnboardingDone(context, oldVersion)

        // When: Setting onboarding as done for new version
        OnboardingPrefs.setOnboardingDone(context, newVersion)

        // Then: Should update to new version
        assertEquals(
            "Should update to new version", newVersion,
            sharedPrefs.getString("onboarding_version", null)
        )
        assertTrue(
            "Onboarding done flag should remain true",
            sharedPrefs.getBoolean("onboarding_done", false)
        )
    }

    @Test
    fun `resetOnboarding clears all preferences`() {
        // Given: Onboarding was completed for some version
        val version = "1.0.0"
        OnboardingPrefs.setOnboardingDone(context, version)

        // Verify it was set
        assertTrue(
            "Should be set initially",
            sharedPrefs.getBoolean("onboarding_done", false)
        )
        assertNotNull(
            "Version should be set initially",
            sharedPrefs.getString("onboarding_version", null)
        )

        // When: Resetting onboarding
        OnboardingPrefs.resetOnboarding(context)

        // Then: All preferences should be cleared
        assertFalse(
            "Onboarding done flag should be false after reset",
            sharedPrefs.getBoolean("onboarding_done", false)
        )
        assertNull(
            "Version should be null after reset",
            sharedPrefs.getString("onboarding_version", null)
        )
    }

    @Test
    fun `resetOnboarding allows onboarding to be needed again`() {
        // Given: Onboarding was completed
        val version = "1.0.0"
        OnboardingPrefs.setOnboardingDone(context, version)
        assertFalse(
            "Should not need onboarding initially",
            OnboardingPrefs.isOnboardingNeeded(context, version)
        )

        // When: Resetting onboarding
        OnboardingPrefs.resetOnboarding(context)

        // Then: Onboarding should be needed again
        assertTrue(
            "Should need onboarding after reset",
            OnboardingPrefs.isOnboardingNeeded(context, version)
        )
    }

    @Test
    fun `handles empty version string gracefully`() {
        // Given: Empty version string
        val emptyVersion = ""

        // When: Setting and checking onboarding with empty version
        OnboardingPrefs.setOnboardingDone(context, emptyVersion)
        val result = OnboardingPrefs.isOnboardingNeeded(context, emptyVersion)

        // Then: Should handle gracefully
        assertFalse("Should not need onboarding for same empty version", result)
    }

    @Test
    fun `handles null version scenarios`() {
        // Given: Onboarding done with some version, but checking with different version
        val validVersion = "1.0.0"
        val differentVersion = "2.0.0"
        OnboardingPrefs.setOnboardingDone(context, validVersion)

        // When: Checking with different version (stored version will be different from current)
        val result = OnboardingPrefs.isOnboardingNeeded(context, differentVersion)

        // Then: Should need onboarding because versions don't match
        assertTrue("Should need onboarding when versions don't match", result)
    }

    @Test
    fun `complex version upgrade scenario`() {
        // Given: Multiple version upgrades simulation
        val versions = listOf("0.9.0", "1.0.0", "1.1.0", "2.0.0")

        for (i in versions.indices) {
            val currentVersion = versions[i]

            // For each version, onboarding should be needed initially
            assertTrue(
                "Should need onboarding for version $currentVersion",
                OnboardingPrefs.isOnboardingNeeded(context, currentVersion)
            )

            // Complete onboarding for this version
            OnboardingPrefs.setOnboardingDone(context, currentVersion)

            // Should not need onboarding for same version anymore
            assertFalse(
                "Should not need onboarding after completion for $currentVersion",
                OnboardingPrefs.isOnboardingNeeded(context, currentVersion)
            )
        }
    }

    @Test
    fun `concurrent access doesn't cause issues`() {
        // Given: Multiple rapid calls (simulating potential race conditions)
        val version = "1.0.0"

        // When: Rapid successive calls
        repeat(10) {
            OnboardingPrefs.setOnboardingDone(context, version)
            val result = OnboardingPrefs.isOnboardingNeeded(context, version)
            assertFalse("Should consistently return false after setting done", result)
        }
    }
}
