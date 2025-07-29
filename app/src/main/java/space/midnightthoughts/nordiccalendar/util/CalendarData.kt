package space.midnightthoughts.nordiccalendar.util

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log

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

data class Event(
    val id: Long, // Instanz-ID (Instances._ID)
    val eventId: Long, // Event-ID (Instances.EVENT_ID)
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val calendarId: Long,
    val location: String?,
    val allDay: Boolean,
    val organizer: String?,
    val attendees: List<String> = emptyList(),
    // Reference to the calendar this event belongs to
    val calendar: Calendar
)

// Helper for the android calendar provider/CalendarContract
class CalendarData {
    // Get available calendars from the Android Calendar Provider
    fun getCalendars(contentResolver: ContentResolver): List<Calendar> {
        val calendars = mutableListOf<Calendar>()


        val EVENT_PROJECTION = arrayOf(
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
            EVENT_PROJECTION,
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

        val INSTANCE_PROJECTION = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.ORGANIZER
        )
        val PROJECTION_ID_INDEX = 0
        val PROJECTION_EVENT_ID_INDEX = 1
        val PROJECTION_TITLE_INDEX = 2
        val PROJECTION_DESCRIPTION_INDEX = 3
        val PROJECTION_BEGIN_INDEX = 4
        val PROJECTION_END_INDEX = 5
        val PROJECTION_CALENDAR_ID_INDEX = 6
        val PROJECTION_EVENT_LOCATION_INDEX = 7
        val PROJECTION_ALL_DAY_INDEX = 8
        val PROJECTION_ORGANIZER_INDEX = 9

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
            val id = cur.getLong(PROJECTION_ID_INDEX)
            val eventId = cur.getLong(PROJECTION_EVENT_ID_INDEX)
            val title = cur.getString(PROJECTION_TITLE_INDEX) ?: "No Title"
            val description = cur.getString(PROJECTION_DESCRIPTION_INDEX)
            val startTime = cur.getLong(PROJECTION_BEGIN_INDEX)
            val endTime = cur.getLong(PROJECTION_END_INDEX)
            val calId = cur.getLong(PROJECTION_CALENDAR_ID_INDEX)
            val location = cur.getString(PROJECTION_EVENT_LOCATION_INDEX)
            val allDay = cur.getInt(PROJECTION_ALL_DAY_INDEX) == 1
            val organizer = cur.getString(PROJECTION_ORGANIZER_INDEX)

            events.add(
                Event(
                    id = id,
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
                        ) // Fallback if calendar not found
                )
            )
        }
        cur?.close()

        return events
    }

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

    fun getEventById(contentResolver: ContentResolver, eventId: Long): Event? {
        val INSTANCE_PROJECTION = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.ORGANIZER
        )
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
                id = cur.getLong(0),
                eventId = cur.getLong(1),
                title = cur.getString(2) ?: "No Title",
                description = cur.getString(3),
                startTime = cur.getLong(4),
                endTime = cur.getLong(5),
                calendarId = cur.getLong(6),
                location = cur.getString(7),
                allDay = cur.getInt(8) == 1,
                organizer = cur.getString(9),
                calendar = getCalendars(contentResolver).firstOrNull { it.id == cur.getLong(6) }
                    ?: Calendar(
                        id = cur.getLong(6),
                        name = "Unknown Calendar",
                        color = 0xFF000000, // Default black color
                        accountName = "Unknown Account",
                        accountType = "Unknown Type",
                        syncEvents = false,
                        visible = false,
                        displayName = "Unknown Calendar" // Fallback if calendar not found
                    ) // Fallback if calendar not found
            )
        } else null
        cur?.close()
        return event
    }
}