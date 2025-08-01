package space.midnightthoughts.nordiccalendar

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import space.midnightthoughts.nordiccalendar.background.CalendarSyncWorker
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
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
     * Provides the WorkManager configuration with the HiltWorkerFactory.
     * This is used to create workers that can inject dependencies via Hilt.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Called when the application is created.
     * This method sets up the periodic calendar synchronization work and registers a content observer
     * for calendar changes.
     */
    override fun onCreate() {
        super.onCreate()
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
    }

    /**
     * Called when the application is terminated.
     * This method unregisters the calendar content observer to avoid memory leaks.
     */
    override fun onTerminate() {
        super.onTerminate()
        // CalendarContentObserver abmelden, um Speicherlecks zu vermeiden
        Log.d("NordicCalendarApp", "Unregistering calendar content observer")
        calendarRepository.unregisterCalendarContentObserver(this)
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
