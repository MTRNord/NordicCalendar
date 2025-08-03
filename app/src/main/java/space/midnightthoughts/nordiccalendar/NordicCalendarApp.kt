package space.midnightthoughts.nordiccalendar

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import space.midnightthoughts.nordiccalendar.background.CalendarSyncWorker
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.OnboardingPrefs
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 *  NordicCalendarApp is the main application class for the Nordic Calendar app.
 * It initializes Hilt for dependency injection, sets up WorkManager for periodic calendar synchronization,
 * and registers a content observer for calendar changes.
 *
 * @property workerFactory The HiltWorkerFactory for creating Hilt-enabled workers.
 * @property calendarRepository The repository for managing calendar data and operations.
 */
@HiltAndroidApp
class NordicCalendarApp : Application(), Configuration.Provider {
    /**
     * HiltWorkerFactory for creating workers with Hilt dependencies.
     * Injected by Hilt.
     */
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * CalendarRepository for managing calendar data and operations.
     * Injected by Hilt.
     */
    @Inject
    lateinit var calendarRepository: CalendarRepository

    /**
     * Tracks whether calendar services have been initialized.
     */
    private var calendarServicesInitialized = false

    /**
     * Provides the WorkManager configuration with the HiltWorkerFactory.
     * This is used to create workers that can inject dependencies via Hilt.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Called when the application is created.
     * This method only initializes calendar services if onboarding is complete and permissions are granted.
     */
    override fun onCreate() {
        super.onCreate()
        Log.d("NordicCalendarApp", "App created")

        // Only initialize calendar services if onboarding is complete and permissions are granted
        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "0.1.0-exp"
        } catch (_: PackageManager.NameNotFoundException) {
            "0.1.0-exp"
        }

        val onboardingNeeded = OnboardingPrefs.isOnboardingNeeded(this, currentVersion)
        val hasCalendarPermissions = hasCalendarPermissions()

        if (!onboardingNeeded && hasCalendarPermissions) {
            Log.d(
                "NordicCalendarApp",
                "Onboarding complete and permissions granted, initializing calendar services"
            )
            initializeCalendarServices()
        } else {
            Log.d(
                "NordicCalendarApp",
                "Onboarding needed: $onboardingNeeded, Has permissions: $hasCalendarPermissions - skipping calendar services initialization"
            )
        }
    }

    /**
     * Checks if the app has calendar permissions.
     * @return True if both READ_CALENDAR and WRITE_CALENDAR permissions are granted.
     */
    private fun hasCalendarPermissions(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        val writePermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        return readPermission && writePermission
    }

    /**
     * Initializes calendar services including WorkManager and content observer.
     * This should only be called after onboarding is complete and permissions are granted.
     */
    fun initializeCalendarServices() {
        if (calendarServicesInitialized) {
            Log.d("NordicCalendarApp", "Calendar services already initialized")
            return
        }

        if (!hasCalendarPermissions()) {
            Log.w("NordicCalendarApp", "Cannot initialize calendar services without permissions")
            return
        }

        try {
            // Initialize the CalendarRepository first
            Log.d("NordicCalendarApp", "Initializing CalendarRepository")
            calendarRepository.initialize()

            // WorkManager für periodische Kalender-Synchronisation einrichten
            Log.d("NordicCalendarApp", "Setting up periodic calendar sync work")
            val workRequest =
                PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                    15,
                    TimeUnit.MINUTES
                )
                    .build()
            Log.d(
                "NordicCalendarApp",
                "Enqueuing periodic work request for calendar sync"
            )
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "calendar_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            // CalendarContentObserver registrieren
            Log.d("NordicCalendarApp", "Registering calendar content observer")
            calendarRepository.registerCalendarContentObserver(this)

            calendarServicesInitialized = true
            Log.d("NordicCalendarApp", "Calendar services initialized successfully")
        } catch (e: SecurityException) {
            Log.e("NordicCalendarApp", "SecurityException while initializing calendar services", e)
        } catch (e: Exception) {
            Log.e("NordicCalendarApp", "Error initializing calendar services", e)
        }
    }

    /**
     * Called when the application is terminated.
     * This method unregisters the calendar content observer to avoid memory leaks.
     */
    override fun onTerminate() {
        super.onTerminate()
        if (calendarServicesInitialized) {
            // CalendarContentObserver abmelden, um Speicherlecks zu vermeiden
            Log.d("NordicCalendarApp", "Unregistering calendar content observer")
            calendarRepository.unregisterCalendarContentObserver(this)
        }
    }
}

/**
 * Returns the current application locale based on the Android version.
 * For Android Tiramisu (API 33) and above, it uses LocaleManager to get the application locales.
 * For earlier versions, it falls back to Locale.getDefault().
 *
 * @param context The application context.
 * @return The current application locale.
 */
fun getCurrentAppLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val appLocales: LocaleList = localeManager.applicationLocales
        if (!appLocales.isEmpty) appLocales[0] else Locale.getDefault()
    } else {
        Locale.getDefault()
    }
}
