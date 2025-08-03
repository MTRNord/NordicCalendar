package space.midnightthoughts.nordiccalendar.util

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for ColorUtils utility functions.
 * Tests color contrast calculation and color conversion functions.
 */
class ColorUtilsTest {

    @Test
    fun `getContrastingTextColor returns black for light backgrounds`() {
        // Test with pure white (highest luminance)
        val whiteBackground = Color.White
        val result = ColorUtils.getContrastingTextColor(whiteBackground)
        assertEquals(Color.Black, result)
    }

    @Test
    fun `getContrastingTextColor returns white for dark backgrounds`() {
        // Test with pure black (lowest luminance)
        val blackBackground = Color.Black
        val result = ColorUtils.getContrastingTextColor(blackBackground)
        assertEquals(Color.White, result)
    }

    @Test
    fun `getContrastingTextColor returns black for light gray background`() {
        // Light gray should have luminance > 0.5
        val lightGrayBackground = Color(0.8f, 0.8f, 0.8f)
        val result = ColorUtils.getContrastingTextColor(lightGrayBackground)
        assertEquals(Color.Black, result)
    }

    @Test
    fun `getContrastingTextColor returns white for dark gray background`() {
        // Dark gray should have luminance < 0.5
        val darkGrayBackground = Color(0.3f, 0.3f, 0.3f)
        val result = ColorUtils.getContrastingTextColor(darkGrayBackground)
        assertEquals(Color.White, result)
    }

    @Test
    fun `getContrastingTextColor returns white for bright red background`() {
        // Pure red has relatively low luminance despite being "bright"
        val redBackground = Color.Red
        val result = ColorUtils.getContrastingTextColor(redBackground)
        assertEquals(Color.White, result)
    }

    @Test
    fun `getContrastingTextColor returns black for bright yellow background`() {
        // Yellow has high luminance
        val yellowBackground = Color.Yellow
        val result = ColorUtils.getContrastingTextColor(yellowBackground)
        assertEquals(Color.Black, result)
    }

    @Test
    fun `getContrastingTextColor returns white for blue background`() {
        // Blue has low luminance
        val blueBackground = Color.Blue
        val result = ColorUtils.getContrastingTextColor(blueBackground)
        assertEquals(Color.White, result)
    }

    @Test
    fun `getContrastingTextColor handles edge case at luminance boundary`() {
        // Test color with luminance exactly at 0.5 threshold
        // RGB(128, 128, 128) should have luminance very close to 0.5
        val midGrayBackground = Color(0.5f, 0.5f, 0.5f)
        val result = ColorUtils.getContrastingTextColor(midGrayBackground)
        // Should return white because luminance is exactly 0.5, not > 0.5
        assertEquals(Color.White, result)
    }

    @Test
    fun `longToColor converts long to Color with full alpha`() {
        // Test with a color that has no alpha channel (RGB only)
        val redLong = 0xFF0000L // Pure red without alpha
        val result = ColorUtils.longToColor(redLong)

        // Should result in fully opaque red
        val expected = Color(0xFFFF0000.toInt())
        assertEquals(expected, result)
    }

    @Test
    fun `longToColor preserves existing alpha channel`() {
        // Test with a color that already has full alpha
        val redWithAlpha = 0xFFFF0000L // Pure red with full alpha
        val result = ColorUtils.longToColor(redWithAlpha)

        val expected = Color(0xFFFF0000.toInt())
        assertEquals(expected, result)
    }

    @Test
    fun `longToColor forces alpha to full opacity when partial alpha present`() {
        // Test with a color that has partial alpha
        val semiTransparentRed = 0x80FF0000L // 50% transparent red
        val result = ColorUtils.longToColor(semiTransparentRed)

        // Should force to full opacity
        val expected = Color(0xFFFF0000.toInt())
        assertEquals(expected, result)
    }

    @Test
    fun `longToColor handles zero value`() {
        val zeroLong = 0x000000L
        val result = ColorUtils.longToColor(zeroLong)

        // Should result in fully opaque black
        val expected = Color(0xFF000000.toInt())
        assertEquals(expected, result)
    }

    @Test
    fun `longToColor handles white color`() {
        val whiteLong = 0xFFFFFFL // White without alpha
        val result = ColorUtils.longToColor(whiteLong)

        // Should result in fully opaque white
        val expected = Color(0xFFFFFFFF.toInt())
        assertEquals(expected, result)
    }

    @Test
    fun `longToColor handles various color combinations`() {
        // Test with green
        val greenLong = 0x00FF00L
        val greenResult = ColorUtils.longToColor(greenLong)
        assertEquals(Color(0xFF00FF00.toInt()), greenResult)

        // Test with blue
        val blueLong = 0x0000FFL
        val blueResult = ColorUtils.longToColor(blueLong)
        assertEquals(Color(0xFF0000FF.toInt()), blueResult)

        // Test with purple
        val purpleLong = 0xFF00FFL
        val purpleResult = ColorUtils.longToColor(purpleLong)
        assertEquals(Color(0xFFFF00FF.toInt()), purpleResult)
    }

    @Test
    fun `longToColor handles maximum long value`() {
        // Test with maximum possible color value
        val maxLong = 0xFFFFFFFFL
        val result = ColorUtils.longToColor(maxLong)

        // Should result in white (all bits set)
        val expected = Color(0xFFFFFFFF.toInt())
        assertEquals(expected, result)
    }
}
