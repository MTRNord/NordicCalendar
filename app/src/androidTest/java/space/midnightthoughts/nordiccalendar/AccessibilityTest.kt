package space.midnightthoughts.nordiccalendar

import android.Manifest
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    var mRuntimePermissionRule: GrantPermissionRule? =
        GrantPermissionRule.grant(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.INTERNET,
        )

    @Test
    fun calendarScreen_accessibilityCheck() {
        composeTestRule.enableAccessibilityChecks()

        // Navigate to the day screen
        composeTestRule.onNodeWithTag("tab_2")
            .performClick()


        // Any action (such as performClick) will perform accessibility checks too:
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}

