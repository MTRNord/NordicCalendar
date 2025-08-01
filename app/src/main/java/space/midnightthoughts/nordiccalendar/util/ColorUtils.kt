package space.midnightthoughts.nordiccalendar.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Utility functions for color manipulation and contrast calculation.
 */
object ColorUtils {

    /**
     * Determines whether to use light or dark text based on the background color's luminance.
     * Uses WCAG guidelines for accessibility.
     *
     * @param backgroundColor The background color to check
     * @return Color.White for dark backgrounds, Color.Black for light backgrounds
     */
    fun getContrastingTextColor(backgroundColor: Color): Color {
        return if (backgroundColor.luminance() > 0.5f) {
            Color.Black
        } else {
            Color.White
        }
    }


    /**
     * Converts a long color value to a Compose Color with guaranteed alpha.
     *
     * @param colorLong The long color value (may or may not include alpha)
     * @return Color with full opacity
     */
    fun longToColor(colorLong: Long): Color {
        return Color((colorLong or 0xFF000000L).toInt())
    }
}
