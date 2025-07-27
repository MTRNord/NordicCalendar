package space.midnightthoughts.nordiccalendar.util

import android.content.Context
import android.util.Log
import androidx.core.content.edit

object OnboardingPrefs {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_ONBOARDING_VERSION = "onboarding_version"

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

    fun setOnboardingDone(context: Context, currentVersion: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putBoolean(KEY_ONBOARDING_DONE, true)
                .putString(KEY_ONBOARDING_VERSION, currentVersion)
        }
    }

    fun resetOnboarding(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }
}

