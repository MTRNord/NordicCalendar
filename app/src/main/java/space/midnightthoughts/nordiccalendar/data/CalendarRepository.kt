package space.midnightthoughts.nordiccalendar.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.util.Calendar
import space.midnightthoughts.nordiccalendar.util.CalendarData
import space.midnightthoughts.nordiccalendar.util.Event
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
}
