package space.midnightthoughts.nordiccalendar.util

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log

/**
 * Data class representing a calendar.
 *
 * @property id The unique ID of the calendar.
 * @property name The display name of the calendar.
 * @property color The color of the calendar.
 * @property accountName The account name associated with the calendar.
 * @property accountType The account type associated with the calendar.
 * @property syncEvents Whether the calendar is set to sync events.
 * @property visible Whether the calendar is visible.
 * @property selected Whether the calendar is selected (default: true).
 */
data class Calendar(
    val id: Long,
    val name: String,
    val displayName: String,
    val color: Long,
    val accountName: String,
    val accountType: String,
    val syncEvents: Boolean,
    val visible: Boolean,
    val selected: Boolean = true
)

/**
 * Data class representing an event in the calendar.
 *
 * @property title The title of the event.
 * @property description A description of the event.
 * @property location The location of the event.
 * @property eventColor The color of the event.
 * @property status The status of the event (e.g., confirmed, tentative, canceled).
 * @property selfAttendeeStatus The attendee status of the self (e.g., accepted, declined).
 * @property duration The duration of the event.
 * @property eventTimezone The timezone of the event.
 * @property eventEndTimezone The timezone of the event's end time.
 * @property allDay Whether the event lasts all day.
 * @property accessLevel The access level for the event.
 * @property availability The availability status of the event.
 * @property hasAlarm Whether the event has an alarm.
 * @property eventId The unique ID of the event.
 * @property startTime The start time of the event in milliseconds.
 * @property endTime The end time of the event in milliseconds.
 * @property calendarId The ID of the calendar that the event belongs to.
 * @property organizer The organizer of the event.
 * @property attendees A list of attendees for the event.
 * @property calendar A reference to the calendar this event belongs to.
 */
data class Event(
    val title: String,
    val description: String?,
    val location: String?,
    val eventColor: Long,
    /**
     * One of STATUS_TENTATIVE, STATUS_CONFIRMED, STATUS_CANCELED
     */
    val status: Int,
    val selfAttendeeStatus: Int,
    val duration: String?,
    val eventTimezone: String?,
    val eventEndTimezone: String?,
    val allDay: Boolean,
    val accessLevel: Int,
    val availability: Int,
    val hasAlarm: Boolean,
    val eventId: Long,
    val startTime: Long,
    val endTime: Long,
    val calendarId: Long,
    val organizer: String?,
    val attendees: List<String> = emptyList(),
    /**
     * Reference to the calendar this event belongs to
     */
    val calendar: Calendar
)

/**
 * Data class representing a reminder for an event.
 *
 * @property minutes The number of minutes before the event when the reminder should trigger.
 * @property method The method used for the reminder (e.g., alert, email).
 */
data class Reminder(val minutes: Int, val method: Int)

/**
 * Helper class for interacting with the Android calendar provider/CalendarContract.
 * Provides methods for querying calendars, events, and reminders.
 */
class CalendarData {

    /**
     * Projection array for querying event instances from the calendar provider.
     */
    val INSTANCE_PROJECTION = arrayOf(
        CalendarContract.Instances.CALENDAR_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.DESCRIPTION,
        CalendarContract.Instances.EVENT_LOCATION,
        CalendarContract.Instances.EVENT_COLOR,
        CalendarContract.Instances.STATUS,
        CalendarContract.Instances.SELF_ATTENDEE_STATUS,
        CalendarContract.Instances.DURATION,
        CalendarContract.Instances.EVENT_TIMEZONE,
        CalendarContract.Instances.EVENT_END_TIMEZONE,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.ACCESS_LEVEL,
        CalendarContract.Instances.AVAILABILITY,
        CalendarContract.Instances.HAS_ALARM,
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ORGANIZER
    )
    val PROJECTION_CALENDAR_ID_INDEX = 0
    val PROJECTION_TITLE_INDEX = 1
    val PROJECTION_DESCRIPTION_INDEX = 2
    val PROJECTION_EVENT_LOCATION_INDEX = 3
    val PROJECTION_EVENT_COLOR_INDEX = 4
    val PROJECTION_STATUS_INDEX = 5
    val PROJECTION_SELF_ATTENDEE_STATUS_INDEX = 6
    val PROJECTION_DURATION_INDEX = 7
    val PROJECTION_EVENT_TIMEZONE_INDEX = 8
    val PROJECTION_EVENT_END_TIMEZONE_INDEX = 9
    val PROJECTION_ALL_DAY_INDEX = 10
    val PROJECTION_ACCESS_LEVEL_INDEX = 11
    val PROJECTION_AVAILABILITY_INDEX = 12
    val PROJECTION_HAS_ALARM_INDEX = 13
    val PROJECTION_EVENT_ID_INDEX = 14
    val PROJECTION_BEGIN_INDEX = 15
    val PROJECTION_END_INDEX = 16
    val PROJECTION_ORGANIZER_INDEX = 17

    /**
     * Get available calendars from the Android Calendar Provider
     */
    fun getCalendars(contentResolver: ContentResolver): List<Calendar> {
        val calendars = mutableListOf<Calendar>()

        val CALENDAR_PROJECTION = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
        )
        val PROJECTION_ID_INDEX = 0
        val PROJECTION_DISPLAY_NAME_INDEX = 1
        val PROJECTION_NAME_INDEX = 2
        val PROJECTION_CALENDAR_COLOR_INDEX = 3
        val PROJECTION_VISIBLE_INDEX = 4
        val PROJECTION_SYNC_EVENTS_INDEX = 5
        val PROJECTION_ACCOUNT_NAME_INDEX = 6
        val PROJECTION_ACCOUNT_TYPE_INDEX = 7

        val uri = CalendarContract.Calendars.CONTENT_URI
        val selection = ""
        val selectionArgs = emptyArray<String>()
        val cur = contentResolver.query(
            uri,
            CALENDAR_PROJECTION,
            selection,
            selectionArgs,
            null
        )
        while (cur?.moveToNext() == true) {
            val calId = cur.getLong(PROJECTION_ID_INDEX)
            val displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX)
            val name = cur.getString(PROJECTION_NAME_INDEX)
            val color = cur.getLong(PROJECTION_CALENDAR_COLOR_INDEX)
            val visible = cur.getInt(PROJECTION_VISIBLE_INDEX)
            val syncEvents = cur.getInt(PROJECTION_SYNC_EVENTS_INDEX)
            val accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX)
            val accountType = cur.getString(PROJECTION_ACCOUNT_TYPE_INDEX)
            calendars.add(
                Calendar(
                    id = calId,
                    name = displayName ?: name,
                    color = color,
                    accountName = accountName ?: "",
                    accountType = accountType ?: "",
                    syncEvents = syncEvents == 1,
                    visible = visible == 1,
                    displayName = displayName ?: "No Display Name",
                )
            )
        }
        cur?.close()

        return calendars
    }

    /**
     * Get events for a specific calendar within a given time range.
     *
     * @param contentResolver The content resolver to access the calendar provider.
     * @param calendarId The ID of the calendar to fetch events from.
     * @param startMillis The start of the time range in milliseconds (default: start of the week).
     * @param endMillis The end of the time range in milliseconds (default: end of the week).
     * @return A list of events occurring in the specified calendar and time range.
     */
    fun getEventsForCalendar(
        contentResolver: ContentResolver,
        calendarId: Long,
        startMillis: Long = java.util.Calendar.getInstance().run {
            // Monday this week
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            timeInMillis
        },
        endMillis: Long = java.util.Calendar.getInstance().run {
            // Sunday this week end of day
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            timeInMillis
        }
    ): List<Event> {
        val events = mutableListOf<Event>()

        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val cur = contentResolver.query(
            builder.build(),
            INSTANCE_PROJECTION,
            "${CalendarContract.Instances.CALENDAR_ID} = ?",
            arrayOf(calendarId.toString()),
            "${CalendarContract.Instances.BEGIN} ASC"
        )
        while (cur?.moveToNext() == true) {
            val calId = cur.getLong(PROJECTION_CALENDAR_ID_INDEX)
            val title = cur.getString(PROJECTION_TITLE_INDEX) ?: "No Title"
            val description = cur.getString(PROJECTION_DESCRIPTION_INDEX)
            val location = cur.getString(PROJECTION_EVENT_LOCATION_INDEX)
            val event_color = cur.getLong(PROJECTION_EVENT_COLOR_INDEX)
            val status = cur.getInt(PROJECTION_STATUS_INDEX)
            val selfAttendeeStatus = cur.getInt(PROJECTION_SELF_ATTENDEE_STATUS_INDEX)
            val duration = cur.getString(PROJECTION_DURATION_INDEX)
            val eventTimezone = cur.getString(PROJECTION_EVENT_TIMEZONE_INDEX)
            val eventEndTimezone = cur.getString(PROJECTION_EVENT_END_TIMEZONE_INDEX)
            val allDay = cur.getInt(PROJECTION_ALL_DAY_INDEX) == 1
            val accessLevel = cur.getInt(PROJECTION_ACCESS_LEVEL_INDEX)
            val availability = cur.getInt(PROJECTION_AVAILABILITY_INDEX)
            val hasAlarm = cur.getInt(PROJECTION_HAS_ALARM_INDEX) == 1
            val eventId = cur.getLong(PROJECTION_EVENT_ID_INDEX)
            val startTime = cur.getLong(PROJECTION_BEGIN_INDEX)
            val endTime = cur.getLong(PROJECTION_END_INDEX)
            val organizer = cur.getString(PROJECTION_ORGANIZER_INDEX)

            events.add(
                Event(
                    eventId = eventId,
                    title = title,
                    description = description,
                    startTime = startTime,
                    endTime = endTime,
                    calendarId = calId,
                    location = location,
                    allDay = allDay,
                    organizer = organizer,
                    calendar = getCalendars(contentResolver).firstOrNull { it.id == calId }
                        ?: Calendar(
                            id = calId,
                            name = "Unknown Calendar",
                            color = 0xFF000000, // Default black color
                            accountName = "Unknown Account",
                            accountType = "Unknown Type",
                            syncEvents = false,
                            visible = false,
                            displayName = "Unknown Calendar" // Fallback if calendar not found
                        ),
                    eventColor = event_color,
                    status = status,
                    selfAttendeeStatus = selfAttendeeStatus,
                    duration = duration,
                    eventTimezone = eventTimezone,
                    eventEndTimezone = eventTimezone,
                    accessLevel = accessLevel,
                    availability = availability,
                    hasAlarm = hasAlarm,
                )
            )
        }
        cur?.close()

        return events
    }

    /**
     * Get events for multiple calendars within a given time range.
     *
     * @param contentResolver The content resolver to access the calendar provider.
     * @param calendarIds A list of calendar IDs to fetch events from.
     * @param startMillis The start of the time range in milliseconds (default: start of the week).
     * @param endMillis The end of the time range in milliseconds (default: end of the week).
     * @return A list of events occurring in the specified calendars and time range.
     */
    fun getEventsForCalendars(
        contentResolver: ContentResolver,
        calendarIds: List<Long>,
        startMillis: Long = java.util.Calendar.getInstance().run {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            timeInMillis
        },
        endMillis: Long = java.util.Calendar.getInstance().run {
            set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SUNDAY)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
            timeInMillis
        }
    ): List<Event> {
        if (calendarIds.isEmpty()) return emptyList()
        val calendars = getCalendars(contentResolver)
            .filter { it.id in calendarIds }
        if (calendars.isEmpty()) {
            Log.w("CalendarData", "No calendars found for IDs: ${calendarIds.joinToString()}")
            return emptyList()
        }
        Log.d(
            "CalendarData",
            "Fetching events for calendars: ${calendars.joinToString { it.name + " (${it.id})" }}"
        )
        val events = mutableListOf<Event>()
        // Fetch events for each calendar
        for (calendar in calendars) {
            val currentEvents = getEventsForCalendar(
                contentResolver,
                calendar.id,
                startMillis,
                endMillis
            )
            if (currentEvents.isNotEmpty()) {
                events.addAll(currentEvents).also {
                    // Log each calendar's events
                    Log.d(
                        "CalendarData",
                        "Fetched ${currentEvents.size} events for calendar: ${calendar.name} (${calendar.id})"
                    )
                }
            }
        }
        return events.sortedBy { it.startTime }.also {
            // Log the number of events fetched
            Log.d(
                "CalendarData",
                "Fetched ${it.size} events for calendars: ${calendarIds.joinToString()}"
            )
        }
    }

    /**
     * Get a specific event by its ID.
     *
     * @param contentResolver The content resolver to access the calendar provider.
     * @param eventId The ID of the event to fetch.
     * @return The event with the specified ID, or null if not found.
     */
    fun getEventById(contentResolver: ContentResolver, eventId: Long): Event? {
        val selection = "Instances.event_id = ?"
        val selectionArgs = arrayOf(eventId.toString())
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        val minMillis = 0L
        val maxMillis = Long.MAX_VALUE
        ContentUris.appendId(builder, minMillis)
        ContentUris.appendId(builder, maxMillis)
        val cur = contentResolver.query(
            builder.build(),
            INSTANCE_PROJECTION,
            selection,
            selectionArgs,
            null
        )
        val event = if (cur?.moveToFirst() == true) {
            Event(
                eventId = cur.getLong(PROJECTION_EVENT_ID_INDEX),
                title = cur.getString(PROJECTION_TITLE_INDEX) ?: "No Title",
                description = cur.getString(PROJECTION_DESCRIPTION_INDEX),
                startTime = cur.getLong(PROJECTION_BEGIN_INDEX),
                endTime = cur.getLong(PROJECTION_END_INDEX),
                calendarId = cur.getLong(PROJECTION_CALENDAR_ID_INDEX),
                location = cur.getString(PROJECTION_EVENT_LOCATION_INDEX),
                allDay = cur.getInt(PROJECTION_ALL_DAY_INDEX) == 1,
                organizer = cur.getString(PROJECTION_ORGANIZER_INDEX),
                calendar = getCalendars(contentResolver).firstOrNull {
                    it.id == cur.getLong(
                        PROJECTION_CALENDAR_ID_INDEX
                    )
                }
                    ?: Calendar(
                        id = cur.getLong(PROJECTION_CALENDAR_ID_INDEX),
                        name = "Unknown Calendar",
                        color = 0xFF000000, // Default black color
                        accountName = "Unknown Account",
                        accountType = "Unknown Type",
                        syncEvents = false,
                        visible = false,
                        displayName = "Unknown Calendar" // Fallback if calendar not found
                    ), // Fallback if calendar not found,
                eventColor = cur.getLong(PROJECTION_EVENT_COLOR_INDEX),
                status = cur.getInt(PROJECTION_STATUS_INDEX),
                selfAttendeeStatus = cur.getInt(PROJECTION_SELF_ATTENDEE_STATUS_INDEX),
                duration = cur.getString(PROJECTION_DURATION_INDEX),
                eventTimezone = cur.getString(PROJECTION_EVENT_TIMEZONE_INDEX),
                eventEndTimezone = cur.getString(PROJECTION_EVENT_END_TIMEZONE_INDEX),
                accessLevel = cur.getInt(PROJECTION_ACCESS_LEVEL_INDEX),
                availability = cur.getInt(PROJECTION_AVAILABILITY_INDEX),
                hasAlarm = cur.getInt(PROJECTION_HAS_ALARM_INDEX) == 1,
            )
        } else null
        cur?.close()
        return event
    }

    /**
     * Get all reminders (in minutes) for an event.
     *
     * @param contentResolver The content resolver to access the calendar provider.
     * @param eventId The ID of the event to fetch reminders for.
     * @return A list of reminders for the specified event.
     */
    fun getRemindersForEvent(contentResolver: ContentResolver, eventId: Long): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        val projection = arrayOf(
            CalendarContract.Reminders.MINUTES,
            CalendarContract.Reminders.METHOD
        )
        val selection = "${CalendarContract.Reminders.EVENT_ID} = ?"
        val selectionArgs = arrayOf(eventId.toString())
        val cursor = contentResolver.query(
            CalendarContract.Reminders.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )
        while (cursor?.moveToNext() == true) {
            val minutes = cursor.getInt(0)
            val method = cursor.getInt(1)
            reminders.add(Reminder(minutes, method))
        }
        cursor?.close()
        return reminders
    }
}