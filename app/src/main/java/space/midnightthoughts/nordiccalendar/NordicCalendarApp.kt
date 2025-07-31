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

@HiltAndroidApp
class NordicCalendarApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var calendarRepository: CalendarRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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

    override fun onTerminate() {
        super.onTerminate()
        // CalendarContentObserver abmelden, um Speicherlecks zu vermeiden
        Log.d("NordicCalendarApp", "Unregistering calendar content observer")
        calendarRepository.unregisterCalendarContentObserver(this)
    }
}

fun getCurrentAppLocale(context: Context): Locale {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        val appLocales: LocaleList = localeManager.applicationLocales
        if (!appLocales.isEmpty) appLocales[0] else Locale.getDefault()
    } else {
        Locale.getDefault()
    }
}
