package space.midnightthoughts.nordiccalendar.viewmodels

import android.app.Application
import android.content.ContentResolver
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.util.Calendar
import space.midnightthoughts.nordiccalendar.util.CalendarData
import space.midnightthoughts.nordiccalendar.util.Event

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    val isRefreshing = MutableStateFlow(false)
    private val calendarData = CalendarData()
    private val contentResolver: ContentResolver = app.contentResolver

    private val _calendars = MutableStateFlow<List<Calendar>>(emptyList())
    val calendars: StateFlow<List<Calendar>> = _calendars.asStateFlow()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    var startMillis: Long = getDefaultStartMillis(0)
        private set
    var endMillis: Long = getDefaultEndMillis(0)
        private set

    init {
        loadCalendars()
    }

    private fun loadCalendars() {
        isRefreshing.value = true
        viewModelScope.launch {
            val allCalendars = calendarData.getCalendars(contentResolver)
            _calendars.value = allCalendars
            updateEvents()
            isRefreshing.value = false
        }
    }

    fun toggleCalendar(calendar: Calendar) {
        // TODO: Store this somewhere so it is kept across app restarts
        Log.d("CalendarViewModel", "Toggling calendar: ${calendar.name}, ID: ${calendar.id}")

        val updatedCalendars = _calendars.value.map {
            if (it.id == calendar.id) {
                it.copy(selected = !it.selected)
            } else {
                it
            }
        }
        _calendars.value = updatedCalendars
        updateEvents()
    }

    fun setTab(tab: Int) {
        _selectedTab.value = tab
        startMillis = getDefaultStartMillis(tab)
        endMillis = getDefaultEndMillis(tab)
        updateEvents()
    }

    private fun getDefaultStartMillis(tab: Int): Long {
        val cal = java.util.Calendar.getInstance()
        when (tab) {
            0 -> { // Monat
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            1 -> { // Woche
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            2 -> { // Tag
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            else -> return cal.timeInMillis
        }
    }

    private fun getDefaultEndMillis(tab: Int): Long {
        val cal = java.util.Calendar.getInstance()
        when (tab) {
            0 -> { // Monat
                cal.set(
                    java.util.Calendar.DAY_OF_MONTH,
                    cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                )
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            1 -> { // Woche
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek + 6)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            2 -> { // Tag
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            else -> return cal.timeInMillis
        }
    }

    fun updateEvents() {
        isRefreshing.value = true
        // Zeitintervall immer neu berechnen, damit Pull-to-Refresh in jedem Tab funktioniert
        val tab = _selectedTab.value
        startMillis = getDefaultStartMillis(tab)
        endMillis = getDefaultEndMillis(tab)
        Log.d(
            "CalendarViewModel",
            "Updating events for selected calendars: ${
                _calendars.value.filter { it.selected }.map { it.name }
            }"
        )
        viewModelScope.launch {
            val selectedIds = _calendars.value.filter { it.selected }.map { it.id }
            Log.d("CalendarViewModel", "Selected calendar IDs: $selectedIds")
            _events.value = calendarData.getEventsForCalendars(
                contentResolver,
                selectedIds,
                startMillis,
                endMillis
            )
            isRefreshing.value = false
        }
    }

    fun nextDay() {
        if (_selectedTab.value != 2) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        startMillis = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        endMillis = cal.timeInMillis
        updateEventsCustom()
    }

    fun prevDay() {
        if (_selectedTab.value != 2) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        startMillis = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        endMillis = cal.timeInMillis
        updateEventsCustom()
    }

    fun nextWeek() {
        if (_selectedTab.value != 1) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
        startMillis = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        endMillis = cal.timeInMillis
        updateEventsCustom()
    }

    fun prevWeek() {
        if (_selectedTab.value != 1) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        cal.add(java.util.Calendar.WEEK_OF_YEAR, -1)
        startMillis = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        endMillis = cal.timeInMillis
        updateEventsCustom()
    }

    fun nextMonth() {
        if (_selectedTab.value != 0) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        cal.add(java.util.Calendar.MONTH, 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        startMillis = cal.timeInMillis
        cal.set(
            java.util.Calendar.DAY_OF_MONTH,
            cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        )
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        endMillis = cal.timeInMillis
        updateEventsCustom()
    }

    fun prevMonth() {
        if (_selectedTab.value != 0) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
        cal.add(java.util.Calendar.MONTH, -1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        startMillis = cal.timeInMillis
        cal.set(
            java.util.Calendar.DAY_OF_MONTH,
            cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        )
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        endMillis = cal.timeInMillis
        updateEventsCustom()
    }

    private fun updateEventsCustom() {
        isRefreshing.value = true
        Log.d(
            "CalendarViewModel",
            "Updating events for selected calendars: ${
                _calendars.value.filter { it.selected }.map { it.name }
            }"
        )
        viewModelScope.launch {
            val selectedIds = _calendars.value.filter { it.selected }.map { it.id }
            Log.d("CalendarViewModel", "Selected calendar IDs: $selectedIds")
            _events.value = calendarData.getEventsForCalendars(
                contentResolver,
                selectedIds,
                startMillis,
                endMillis
            )
            isRefreshing.value = false
        }
    }

    fun setTodayWeek() {
        if (_selectedTab.value != 1) return
        val cal = java.util.Calendar.getInstance()
        startMillis = cal.apply {
            set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        endMillis = cal.apply {
            add(java.util.Calendar.DAY_OF_WEEK, 6)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis
        updateEventsCustom()
    }

    fun setTodayMonth() {
        if (_selectedTab.value != 0) return
        val cal = java.util.Calendar.getInstance()
        startMillis = cal.apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        endMillis = cal.apply {
            set(
                java.util.Calendar.DAY_OF_MONTH,
                getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            )
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis
        updateEventsCustom()
    }

    fun setTodayDay() {
        if (_selectedTab.value != 2) return
        val cal = java.util.Calendar.getInstance()
        startMillis = cal.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        endMillis = cal.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis
        updateEventsCustom()
    }
}
