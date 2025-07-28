package space.midnightthoughts.nordiccalendar.data

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import space.midnightthoughts.nordiccalendar.util.Calendar
import space.midnightthoughts.nordiccalendar.util.CalendarData
import space.midnightthoughts.nordiccalendar.util.Event

class CalendarRepository(private val context: Context) {
    private val calendarData = CalendarData()
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("calendar_prefs", Context.MODE_PRIVATE)
    }
    private val contentResolver: ContentResolver get() = context.contentResolver

    fun getCalendars(): List<Calendar> {
        val calendars = calendarData.getCalendars(contentResolver)
        val selectedIds = prefs.getStringSet("selected_calendar_ids", null)
        return if (selectedIds != null) {
            calendars.map { it.copy(selected = selectedIds.contains(it.id.toString())) }
        } else {
            calendars
        }
    }

    fun setCalendarSelected(calendarId: Long, selected: Boolean) {
        val selectedIds =
            prefs.getStringSet("selected_calendar_ids", null)?.toMutableSet() ?: mutableSetOf()
        if (selected) {
            selectedIds.add(calendarId.toString())
        } else {
            selectedIds.remove(calendarId.toString())
        }
        prefs.edit { putStringSet("selected_calendar_ids", selectedIds) }
    }

    fun getEventsForCalendars(
        calendarIds: List<Long>,
        startMillis: Long,
        endMillis: Long
    ): List<Event> =
        calendarData.getEventsForCalendars(contentResolver, calendarIds, startMillis, endMillis)

    fun getEventById(eventId: Long): Event? {
        return calendarData.getEventById(contentResolver, eventId)
    }
}
