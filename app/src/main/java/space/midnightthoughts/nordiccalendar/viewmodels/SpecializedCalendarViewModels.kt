package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.Event
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel specifically for the month view, ensuring month-appropriate time ranges.
 * Manages navigation between months and enforces month boundaries.
 */
@HiltViewModel
class MonthViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    repository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : BaseCalendarViewModel(repository) {

    /**
     * StateFlow of events for the current month.
     */
    val events: StateFlow<List<Event>> by lazy { getEventsFlow(context) }

    init {
        initializeDefaultTimeRange()
        // Schedule reminders when events change
        viewModelScope.launch {
            events.collect { eventList ->
                scheduleRemindersForEvents(context, eventList)
            }
        }
    }

    override fun initializeDefaultTimeRange() {
        navigateToToday()
    }

    override fun navigateToToday() {
        val cal = java.util.Calendar.getInstance()
        setMonthRange(cal)
    }

    override fun navigateToNext() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = startMillis.value
        }
        cal.add(java.util.Calendar.MONTH, 1)
        setMonthRange(cal)
    }

    override fun navigateToPrevious() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = startMillis.value
        }
        cal.add(java.util.Calendar.MONTH, -1)
        setMonthRange(cal)
    }

    /**
     * Sets the time range to encompass the entire month containing the given calendar.
     * Ensures the range always covers a complete month from day 1 to the last day.
     */
    private fun setMonthRange(cal: java.util.Calendar) {
        // Start: First day of month at 00:00:00
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        // End: Last day of month at 23:59:59.999
        cal.set(
            java.util.Calendar.DAY_OF_MONTH,
            cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        )
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        Log.d(
            "MonthViewModel",
            "Setting month range: ${java.util.Date(start)} to ${java.util.Date(end)}"
        )
        setTimeRange(start, end)
    }

    /**
     * Navigates to a specific month and year.
     */
    fun navigateToMonth(year: Int, month: Int) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, month - 1) // Calendar months are 0-based
        setMonthRange(cal)
    }
}

/**
 * ViewModel specifically for the week view, ensuring week-appropriate time ranges.
 * Manages navigation between weeks and enforces week boundaries.
 */
@HiltViewModel
class WeekViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    repository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : BaseCalendarViewModel(repository) {

    /**
     * StateFlow of events for the current week.
     */
    val events: StateFlow<List<Event>> by lazy { getEventsFlow(context) }

    init {
        initializeDefaultTimeRange()
        // Schedule reminders when events change
        viewModelScope.launch {
            events.collect { eventList ->
                scheduleRemindersForEvents(context, eventList)
            }
        }
    }

    override fun initializeDefaultTimeRange() {
        navigateToToday()
    }

    override fun navigateToToday() {
        val cal = java.util.Calendar.getInstance()
        setWeekRange(cal)
    }

    override fun navigateToNext() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = startMillis.value
        }
        cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
        setWeekRange(cal)
    }

    override fun navigateToPrevious() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = startMillis.value
        }
        cal.add(java.util.Calendar.WEEK_OF_YEAR, -1)
        setWeekRange(cal)
    }

    /**
     * Sets the time range to encompass the entire week containing the given calendar.
     * Week starts on Monday and ends on Sunday.
     */
    private fun setWeekRange(cal: java.util.Calendar) {
        // Start: Monday of the week at 00:00:00
        cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        // End: Sunday of the week at 23:59:59.999
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        Log.d(
            "WeekViewModel",
            "Setting week range: ${java.util.Date(start)} to ${java.util.Date(end)}"
        )
        setTimeRange(start, end)
    }

    /**
     * Navigates to the week containing a specific date.
     */
    fun navigateToWeekContaining(date: LocalDate) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, date.year)
        cal.set(java.util.Calendar.MONTH, date.monthValue - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, date.dayOfMonth)
        setWeekRange(cal)
    }
}

/**
 * ViewModel specifically for the day view, ensuring day-appropriate time ranges.
 * Manages navigation between days and enforces day boundaries.
 */
@HiltViewModel
class DayViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    repository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : BaseCalendarViewModel(repository) {

    /**
     * StateFlow of events for the current day.
     */
    val events: StateFlow<List<Event>> by lazy { getEventsFlow(context) }

    init {
        // Check for date argument from navigation
        val dateArg = savedStateHandle.get<String?>("date")
        if (dateArg != null) {
            navigateToDateFromString(dateArg)
        } else {
            initializeDefaultTimeRange()
        }
        // Schedule reminders when events change
        viewModelScope.launch {
            events.collect { eventList ->
                scheduleRemindersForEvents(context, eventList)
            }
        }
    }

    override fun initializeDefaultTimeRange() {
        navigateToToday()
    }

    override fun navigateToToday() {
        val cal = java.util.Calendar.getInstance()
        setDayRange(cal)
    }

    override fun navigateToNext() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = startMillis.value
        }
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        setDayRange(cal)
    }

    override fun navigateToPrevious() {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = startMillis.value
        }
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        setDayRange(cal)
    }

    /**
     * Sets the time range to encompass the entire day containing the given calendar.
     * Ensures the range covers from 00:00:00 to 23:59:59.999 of the same day.
     */
    private fun setDayRange(cal: java.util.Calendar) {
        // Start: Beginning of day at 00:00:00
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        // End: End of day at 23:59:59.999
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        cal.set(java.util.Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        Log.d(
            "DayViewModel",
            "Setting day range: ${java.util.Date(start)} to ${java.util.Date(end)}"
        )
        setTimeRange(start, end)
    }

    /**
     * Navigates to a specific date.
     */
    fun navigateToDate(date: LocalDate) {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, date.year)
        cal.set(java.util.Calendar.MONTH, date.monthValue - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, date.dayOfMonth)
        setDayRange(cal)
    }

    /**
     * Parses a date string and navigates to that date.
     * If the string is invalid, the error is ignored and today is used.
     */
    fun navigateToDateFromString(dateString: String?) {
        if (dateString == null) {
            navigateToToday()
            return
        }
        try {
            val date = LocalDate.parse(dateString)
            navigateToDate(date)
        } catch (e: Exception) {
            Log.w("DayViewModel", "Invalid date string: $dateString, using today instead")
            navigateToToday()
        }
    }
}
