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

/**
 * CalendarRepository is a singleton class responsible for managing calendar data, events, and reminders.
 * It provides flows for calendars and events, manages the selected time range, and handles synchronization
 * with the Android calendar provider. It also manages scheduling and canceling reminders for events.
 *
 * @constructor Injects the application context for accessing system services and content providers.
 */
@Singleton
class CalendarRepository @Inject constructor(@ApplicationContext context: Context) {
    /**
     * Holds the calendar data utility instance.
     */
    private val calendarData = CalendarData()

    /**
     * StateFlow holding the list of all calendars.
     */
    private val _calendarsFlow = MutableStateFlow<List<Calendar>>(emptyList())

    /**
     * Public read-only StateFlow for observing calendar list changes.
     */
    val calendarsFlow: StateFlow<List<Calendar>> = _calendarsFlow.asStateFlow()

    /**
     * Coroutine scope for repository background operations.
     */
    private val repoScope = CoroutineScope(Dispatchers.IO)

    /**
     * StateFlow holding the start time in milliseconds for the current event range.
     */
    private val _startMillis = MutableStateFlow(System.currentTimeMillis())

    /**
     * Public read-only StateFlow for observing the start time.
     */
    val startMillis: StateFlow<Long> = _startMillis.asStateFlow()

    /**
     * StateFlow holding the end time in milliseconds for the current event range.
     */
    private val _endMillis = MutableStateFlow(System.currentTimeMillis())

    /**
     * Public read-only StateFlow for observing the end time.
     */
    val endMillis: StateFlow<Long> = _endMillis.asStateFlow()

    /**
     * StateFlow holding the list of events for the selected calendars and time range.
     */
    private val _eventsFlow = MutableStateFlow<List<Event>>(emptyList())

    /**
     * Public read-only StateFlow for observing event list changes.
     */
    val eventsFlow: StateFlow<List<Event>> = _eventsFlow.asStateFlow()

    /**
     * ContentObserver for monitoring changes in the calendar provider.
     */
    private var calendarContentObserver: ContentObserver? = null

    /**
     * Initializes the repository by loading calendars and setting up event synchronization.
     */
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

    /**
     * Loads all calendars from the system and updates the calendars flow.
     * @param context The application context.
     */
    private fun loadCalendarsToFlow(context: Context) {
        repoScope.launch {
            val calendars = getCalendars(context)
            _calendarsFlow.value = calendars
        }
    }

    /**
     * Retrieves all calendars from the system and marks them as selected based on preferences.
     * @param context The application context.
     * @return List of Calendar objects.
     */
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

    /**
     * Sets the selected state of a calendar and updates the preferences.
     * @param context The application context.
     * @param calendarId The ID of the calendar to update.
     * @param selected The new selected state.
     */
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

    /**
     * Sets the time range for event retrieval.
     * @param startMillis The start time in milliseconds.
     * @param endMillis The end time in milliseconds.
     */
    fun setTimeRange(startMillis: Long, endMillis: Long) {
        _startMillis.value = startMillis
        _endMillis.value = endMillis
    }

    /**
     * Retrieves events for the specified calendars and time range.
     * @param context The application context.
     * @param calendarIds The list of calendar IDs.
     * @param startMillis The start time in milliseconds.
     * @param endMillis The end time in milliseconds.
     * @return List of Event objects.
     */
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

    /**
     * Retrieves an event by its ID.
     * @param context The application context.
     * @param eventId The ID of the event to retrieve.
     * @return The Event object, or null if not found.
     */
    fun getEventById(context: Context, eventId: Long): Event? {
        return calendarData.getEventById(context.contentResolver, eventId)
    }

    /**
     * Refreshes the events flow by reloading events for the selected calendars.
     * @param context The application context.
     */
    fun refreshEvents(context: Context) {
        repoScope.launch {
            val calendars = getCalendars(context)
            val selectedIds = calendars.filter { it.selected }.map { it.id }
            _eventsFlow.value =
                getEventsForCalendars(context, selectedIds, _startMillis.value, _endMillis.value)
        }
    }

    /**
     * Returns a map of event IDs to their corresponding reminders (lead times).
     * @param context The application context.
     * @param events The list of events to retrieve reminders for.
     * @return Map<EventId, List<Reminder>> containing the reminders for each event.
     */
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
     * Cancels all reminder alarms for the given events.
     * @param context The application context.
     * @param events The list of events to cancel reminders for.
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
                    putExtra("eventDescription", event.description)
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
     * Schedules reminder alarms for events with reminders.
     * Should be called after loading events.
     * Existing reminder alarms for these events are removed first.
     * @param context The application context.
     * @param events The list of events to schedule reminders for.
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
                    putExtra("eventDescription", event.description)
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

    /**
     * Registers a ContentObserver to listen for calendar provider changes.
     * @param context The application context.
     */
    fun registerCalendarContentObserver(context: Context) {
        if (calendarContentObserver != null) return // Only register once
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

    /**
     * Unregisters the ContentObserver for calendar provider changes.
     * @param context The application context.
     */
    fun unregisterCalendarContentObserver(context: Context) {
        calendarContentObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            calendarContentObserver = null
        }
    }
}
