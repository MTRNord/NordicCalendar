package space.midnightthoughts.nordiccalendar.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.notifications.NotificationReceiver
import space.midnightthoughts.nordiccalendar.util.Calendar
import space.midnightthoughts.nordiccalendar.util.CalendarData
import space.midnightthoughts.nordiccalendar.util.Event
import space.midnightthoughts.nordiccalendar.util.Reminder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(@ApplicationContext context: Context) {
    private val calendarData = CalendarData()

    private val _calendarsFlow = MutableStateFlow<List<Calendar>>(emptyList())
    val calendarsFlow: StateFlow<List<Calendar>> = _calendarsFlow.asStateFlow()
    private val repoScope = CoroutineScope(Dispatchers.IO)

    private val _startMillis = MutableStateFlow(System.currentTimeMillis())
    val startMillis: StateFlow<Long> = _startMillis.asStateFlow()
    private val _endMillis = MutableStateFlow(System.currentTimeMillis())
    val endMillis: StateFlow<Long> = _endMillis.asStateFlow()
    private val _eventsFlow = MutableStateFlow<List<Event>>(emptyList())
    val eventsFlow: StateFlow<List<Event>> = _eventsFlow.asStateFlow()

    private var calendarContentObserver: ContentObserver? = null

    init {
        loadCalendarsToFlow(context)
        repoScope.launch {
            combine(_calendarsFlow, _startMillis, _endMillis) { calendars, start, end ->
                val selectedIds = calendars.filter { it.selected }.map { it.id }
                getEventsForCalendars(context, selectedIds, start, end)
            }.collect { events ->
                _eventsFlow.value = events
            }
        }
    }

    private fun loadCalendarsToFlow(context: Context) {
        repoScope.launch {
            val calendars = getCalendars(context)
            _calendarsFlow.value = calendars
        }
    }

    fun getCalendars(context: Context): List<Calendar> {
        val contentResolver = context.contentResolver
        val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
        val calendars = calendarData.getCalendars(contentResolver)
        val selectedIds = prefs.getStringSet("selected_calendar_ids", null)
        return if (selectedIds != null) {
            calendars.map { it.copy(selected = selectedIds.contains(it.id.toString())) }
        } else {
            calendars
        }
    }

    fun setCalendarSelected(context: Context, calendarId: Long, selected: Boolean) {
        val prefs = context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
        val selectedIds =
            prefs.getStringSet("selected_calendar_ids", null)?.toMutableSet() ?: mutableSetOf()
        if (selected) {
            selectedIds.add(calendarId.toString())
        } else {
            selectedIds.remove(calendarId.toString())
        }
        prefs.edit { putStringSet("selected_calendar_ids", selectedIds) }
        loadCalendarsToFlow(context)
    }

    fun setTimeRange(startMillis: Long, endMillis: Long) {
        _startMillis.value = startMillis
        _endMillis.value = endMillis
    }

    fun getEventsForCalendars(
        context: Context,
        calendarIds: List<Long>,
        startMillis: Long,
        endMillis: Long
    ): List<Event> =
        calendarData.getEventsForCalendars(
            context.contentResolver,
            calendarIds,
            startMillis,
            endMillis
        )

    fun getEventById(context: Context, eventId: Long): Event? {
        return calendarData.getEventById(context.contentResolver, eventId)
    }

    fun refreshEvents(context: Context) {
        repoScope.launch {
            val calendars = getCalendars(context)
            val selectedIds = calendars.filter { it.selected }.map { it.id }
            _eventsFlow.value =
                getEventsForCalendars(context, selectedIds, _startMillis.value, _endMillis.value)
        }
    }

    // Liefert für eine Liste von Events alle Reminder (Vorlaufzeiten) als Map<EventId, List<Reminder>>
    fun getRemindersForEvents(
        context: Context,
        events: List<Event>
    ): Map<Long, List<Reminder>> {
        Log.d(
            "CalendarRepository",
            "Getting reminders for ${events.size} events"
        )
        val contentResolver = context.contentResolver
        return events.associate { event ->
            event.eventId to calendarData.getRemindersForEvent(contentResolver, event.eventId)
        }
    }

    /**
     * Löscht alle Reminder-Alarme für die übergebenen Events.
     */
    fun cancelRemindersForEvents(context: Context, events: List<Event>) {
        Log.d(
            "CalendarRepository",
            "Cancelling reminders for ${events.size} events"
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val remindersMap = getRemindersForEvents(context, events)
        for (event in events) {
            val reminders = remindersMap[event.eventId] ?: continue
            for (reminder in reminders) {
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("eventId", event.eventId)
                    putExtra("eventTitle", event.title)
                    putExtra("eventTime", event.startTime)
                    putExtra("eventEndTime", event.endTime)
                    putExtra("eventLocation", event.location)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (event.eventId + reminder.minutes).toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    /**
     * Setzt für alle Events mit Reminder einen Alarm, der eine Benachrichtigung auslöst.
     * Sollte nach jedem Laden der Events aufgerufen werden.
     * Vorher werden alle bestehenden Reminder-Alarme für diese Events entfernt.
     */
    fun scheduleRemindersForEvents(context: Context, events: List<Event>) {
        Log.d(
            "CalendarRepository",
            "Scheduling reminders for ${events.size} events"
        )
        cancelRemindersForEvents(context, events)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val remindersMap = getRemindersForEvents(context, events)
        val hasScheduleExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
        for (event in events) {
            val reminders = remindersMap[event.eventId] ?: continue
            for (reminder in reminders) {
                val triggerAtMillis = event.startTime - (reminder.minutes * 60 * 1000)

                if (triggerAtMillis < System.currentTimeMillis()) {
                    Log.w(
                        "CalendarRepository",
                        "Skipping reminder for event ${event.eventId} (${event.title}) " +
                                "with reminder ${reminder.minutes} minutes, " +
                                "trigger time $triggerAtMillis is in the past"
                    )
                    continue
                }
                Log.d(
                    "CalendarRepository",
                    "Setting reminder for event ${event.eventId} (${event.title}) with reminder ${reminder.minutes} minutes"
                )
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("eventId", event.eventId)
                    putExtra("eventTitle", event.title)
                    putExtra("eventTime", event.startTime)
                    putExtra("eventEndTime", event.endTime)
                    putExtra("eventLocation", event.location)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    (event.eventId + reminder.minutes).toInt(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                try {
                    if (hasScheduleExactAlarm) {
                        Log.d(
                            "CalendarRepository",
                            "Scheduling exact alarm for event ${event.eventId} at $triggerAtMillis"
                        )
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        Log.d(
                            "CalendarRepository",
                            "Scheduling non-exact alarm for event ${event.eventId} at $triggerAtMillis"
                        )
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun registerCalendarContentObserver(context: Context) {
        if (calendarContentObserver != null) return // Nur einmal registrieren
        val handler = Handler(Looper.getMainLooper())
        calendarContentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                refreshEvents(context)
            }
        }
        val cr = context.contentResolver
        cr.registerContentObserver(
            CalendarContract.Events.CONTENT_URI,
            true,
            calendarContentObserver!!
        )
        cr.registerContentObserver(
            CalendarContract.Instances.CONTENT_URI,
            true,
            calendarContentObserver!!
        )
        cr.registerContentObserver(
            CalendarContract.Reminders.CONTENT_URI,
            true,
            calendarContentObserver!!
        )
    }

    fun unregisterCalendarContentObserver(context: Context) {
        calendarContentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            calendarContentObserver = null
        }
    }
}
