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
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface RepositoryEntryPoint {
        fun calendarRepository(): CalendarRepository
    }

    override fun doWork(): Result {
        Log.d("CalendarSyncWorker", "Starting calendar sync work")
        val repo = EntryPointAccessors.fromApplication(
            applicationContext,
            RepositoryEntryPoint::class.java
        ).calendarRepository()
        // Zeitraum: jetzt bis 24h später
        val now = System.currentTimeMillis()
        val tomorrow = now + 24 * 60 * 60 * 1000
        repo.setTimeRange(now, tomorrow)
        // Events für diesen Zeitraum holen
        val calendars = repo.getCalendars(applicationContext)
        val selectedIds = calendars.filter { it.selected }.map { it.id }
        val events = repo.getEventsForCalendars(applicationContext, selectedIds, now, tomorrow)
        // Reminder-Benachrichtigungen vorher löschen und neu setzen
        repo.cancelRemindersForEvents(applicationContext, events)
        repo.scheduleRemindersForEvents(applicationContext, events)
        Log.d("CalendarSyncWorker", "Calendar sync work completed, ${events.size} events processed")
        return Result.success()
    }
}
