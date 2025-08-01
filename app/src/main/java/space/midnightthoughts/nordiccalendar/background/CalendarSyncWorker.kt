// CalendarSyncWorker.kt
//
// This file defines the CalendarSyncWorker class, a background worker for synchronizing calendar events and reminders in the NordicCalendar app. It uses Hilt for dependency injection and interacts with the CalendarRepository to fetch calendars, retrieve events, and manage reminders.
//
// Classes:
//     CalendarSyncWorker: A Worker subclass that performs calendar synchronization tasks in the background. It fetches the current and next day's events from selected calendars and updates reminders accordingly.
//
// Interfaces:
//     RepositoryEntryPoint: Hilt entry point interface to provide CalendarRepository dependency.
//

package space.midnightthoughts.nordiccalendar.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import space.midnightthoughts.nordiccalendar.data.CalendarRepository

@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    /**
     * Hilt entry point interface to provide CalendarRepository dependency
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RepositoryEntryPoint {
        fun calendarRepository(): CalendarRepository
    }

    /**
     * Performs the background calendar synchronization work.
     *
     * Steps:
     * 1. Retrieves the CalendarRepository via Hilt entry point.
     * 2. Sets the time range for event retrieval (now to 24h later).
     * 3. Fetches all calendars and filters for selected ones.
     * 4. Retrieves events for selected calendars within the time range.
     * 5. Cancels existing reminders and schedules new ones for these events.
     *
     * @return Result of the work (success or failure)
     */
    override fun doWork(): Result {
        Log.d("CalendarSyncWorker", "Starting calendar sync work")
        val repo = EntryPointAccessors.fromApplication(
            applicationContext,
            RepositoryEntryPoint::class.java
        ).calendarRepository()
        val now = System.currentTimeMillis()
        val tomorrow = now + 24 * 60 * 60 * 1000
        repo.setTimeRange(now, tomorrow)
        val calendars = repo.getCalendars(applicationContext)
        val selectedIds = calendars.filter { it.selected }.map { it.id }
        val events = repo.getEventsForCalendars(applicationContext, selectedIds, now, tomorrow)
        repo.cancelRemindersForEvents(applicationContext, events)
        repo.scheduleRemindersForEvents(applicationContext, events)
        Log.d("CalendarSyncWorker", "Calendar sync work completed, ${events.size} events processed")
        return Result.success()
    }
}
