package space.midnightthoughts.nordiccalendar

import android.Manifest
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
        Manifest.permission.INTERNET,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.SCHEDULE_EXACT_ALARM
    )

    private lateinit var device: UiDevice

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Give extra time for permissions to be granted and app to initialize
        composeTestRule.waitForIdle()
        Thread.sleep(5000)

        // Ensure we're in a stable state
        composeTestRule.waitForIdle()
    }

    @Test
    fun calendarScreen_accessibilityCheck() {
        composeTestRule.enableAccessibilityChecks()

        // Wait for the initial screen to load completely
        composeTestRule.waitForIdle()
        Thread.sleep(2000)

        try {
            // Navigate to the day screen if possible
            composeTestRule.onNodeWithTag("tab_2")
                .performClick()

            // Wait for navigation to complete
            composeTestRule.waitForIdle()
            Thread.sleep(1000)
        } catch (e: Exception) {
            // If navigation fails, just test the current screen
        }

        // Perform accessibility checks
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
