package space.midnightthoughts.nordiccalendar.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit

/**
 * OnboardingPrefs is a utility object for managing onboarding state in shared preferences.
 * It tracks whether onboarding has been completed and for which app version.
 */
object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_ONBOARDING_VERSION = "onboarding_version"

    /**
     * Checks if onboarding is needed for the current app version.
     *
     * @param context The application context.
     * @param currentVersion The current app version string.
     * @return True if onboarding should be shown, false otherwise.
     */
    fun isOnboardingNeeded(context: Context, currentVersion: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val done = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        val savedVersion = prefs.getString(KEY_ONBOARDING_VERSION, null)
        Log.d(
            "OnboardingPrefs",
            "Onboarding done: $done, saved version: $savedVersion, current version: $currentVersion"
        )
        return !done || savedVersion != currentVersion
    }

    /**
     * Marks onboarding as completed for the given app version.
     *
     * @param context The application context.
     * @param currentVersion The current app version string.
     */
    fun setOnboardingDone(context: Context, currentVersion: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_ONBOARDING_DONE, true)
                .putString(KEY_ONBOARDING_VERSION, currentVersion)
        }
    }

    /**
     * Resets onboarding state (for testing or re-showing onboarding).
     *
     * @param context The application context.
     */
    fun resetOnboarding(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }
}
