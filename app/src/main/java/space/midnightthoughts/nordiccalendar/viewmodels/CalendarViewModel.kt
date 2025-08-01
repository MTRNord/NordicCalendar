package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * CalendarViewModel is the main ViewModel for managing calendar state, event data, and navigation logic.
 * It handles tab selection, date range management, event refreshing, and reminder scheduling.
 *
 * @property context The application context injected by Hilt.
 * @property repository The CalendarRepository for accessing calendar and event data.
 * @property events StateFlow of the current list of events.
 * @property startMillis StateFlow of the current start time in milliseconds.
 * @property endMillis StateFlow of the current end time in milliseconds.
 * @property isRefreshing StateFlow indicating if a refresh is in progress.
 * @property selectedTab StateFlow of the currently selected tab (0=month, 1=week, 2=day).
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    /**
     * StateFlow of the current list of events.
     */
    val events = repository.eventsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    /**
     * StateFlow of the current start time in milliseconds.
     */
    val startMillis = repository.startMillis

    /**
     * StateFlow of the current end time in milliseconds.
     */
    val endMillis = repository.endMillis

    /**
     * StateFlow indicating if a refresh is in progress.
     */
    val isRefreshing = MutableStateFlow(false)

    /**
     * Backing property for the currently selected tab (0=month, 1=week, 2=day).
     */
    private val _selectedTab = MutableStateFlow(0)

    /**
     * StateFlow of the currently selected tab (0=month, 1=week, 2=day).
     */
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    /**
     * Backing property for the date argument (if provided via navigation).
     */
    private val _dateArg = MutableStateFlow<String?>(null)

    /**
     * Initializes the ViewModel, sets up tab and date from navigation arguments, and starts reminder scheduling.
     */
    init {
        val tab = savedStateHandle.get<Int?>("tab")
        if (tab != null) {
            setTab(tab)
        }
        val dateArg = savedStateHandle.get<String?>("date")
        if (dateArg != null) {
            setDayFromString(dateArg)
            _dateArg.value = dateArg
        }
        // Always keep reminder notifications up to date (also on init)
        viewModelScope.launch {
            repository.eventsFlow.collect { events ->
                Log.d("CalendarViewModel", "Scheduling reminders for ${events.size} events")
                repository.scheduleRemindersForEvents(context, events)
            }
        }
    }

    /**
     * Sets the currently selected tab (0=month, 1=week, 2=day) and updates the time range accordingly.
     * If a date argument is present, it is used instead of the default range.
     *
     * @param tab The tab index to select.
     */
    fun setTab(tab: Int) {
        _selectedTab.value = tab
        if (_dateArg.value == null) {
            val start = getDefaultStartMillis(tab)
            val end = getDefaultEndMillis(tab)
            repository.setTimeRange(start, end)
        }
    }

    /**
     * Returns the default start time in milliseconds for the given tab (month, week, or day).
     *
     * @param tab The tab index (0=month, 1=week, 2=day).
     * @return The start time in milliseconds.
     */
    private fun getDefaultStartMillis(tab: Int): Long {
        val cal = java.util.Calendar.getInstance()
        when (tab) {
            0 -> { // Month
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            1 -> { // Week
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            2 -> { // Day
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            else -> return cal.timeInMillis
        }
    }

    /**
     * Returns the default end time in milliseconds for the given tab (month, week, or day).
     *
     * @param tab The tab index (0=month, 1=week, 2=day).
     * @return The end time in milliseconds.
     */
    private fun getDefaultEndMillis(tab: Int): Long {
        val cal = java.util.Calendar.getInstance()
        when (tab) {
            0 -> { // Month
                cal.set(
                    java.util.Calendar.DAY_OF_MONTH,
                    cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                )
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            1 -> { // Week
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek + 6)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            2 -> { // Day
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            else -> return cal.timeInMillis
        }
    }

    // The following methods only change the time period in the repository
    /**
     * Advances the day view to the next day and updates the time range.
     */
    fun nextDay() {
        if (_selectedTab.value != 2) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis.value }
        cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Moves the day view to the previous day and updates the time range.
     */
    fun prevDay() {
        if (_selectedTab.value != 2) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis.value }
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        val start = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Advances the week view to the next week and updates the time range.
     */
    fun nextWeek() {
        if (_selectedTab.value != 1) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis.value }
        cal.add(java.util.Calendar.WEEK_OF_YEAR, 1)
        val start = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Moves the week view to the previous week and updates the time range.
     */
    fun prevWeek() {
        if (_selectedTab.value != 1) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis.value }
        cal.add(java.util.Calendar.WEEK_OF_YEAR, -1)
        val start = cal.timeInMillis
        cal.add(java.util.Calendar.DAY_OF_WEEK, 6)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Advances the month view to the next month and updates the time range.
     */
    fun nextMonth() {
        if (_selectedTab.value != 0) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis.value }
        cal.add(java.util.Calendar.MONTH, 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        cal.set(
            java.util.Calendar.DAY_OF_MONTH,
            cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        )
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Moves the month view to the previous month and updates the time range.
     */
    fun prevMonth() {
        if (_selectedTab.value != 0) return
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis.value }
        cal.add(java.util.Calendar.MONTH, -1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val start = cal.timeInMillis
        cal.set(
            java.util.Calendar.DAY_OF_MONTH,
            cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        )
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Sets the week view to the current week and updates the time range.
     */
    fun setTodayWeek() {
        if (_selectedTab.value != 1) return
        val cal = java.util.Calendar.getInstance()
        val start = cal.apply {
            set(java.util.Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        val end = cal.apply {
            add(java.util.Calendar.DAY_OF_WEEK, 6)
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Sets the month view to the current month and updates the time range.
     */
    fun setTodayMonth() {
        if (_selectedTab.value != 0) return
        val cal = java.util.Calendar.getInstance()
        val start = cal.apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        val end = cal.apply {
            set(
                java.util.Calendar.DAY_OF_MONTH,
                getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            )
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Sets the day view to the current day and updates the time range.
     */
    fun setTodayDay() {
        if (_selectedTab.value != 2) return
        val cal = java.util.Calendar.getInstance()
        val start = cal.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        val end = cal.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Refreshes the events from the repository and updates reminders.
     * Sets the isRefreshing state to true during the operation.
     */
    fun refreshEvents() {
        isRefreshing.value = true
        repository.refreshEvents(context)
        // After updating the events, set reminders
        val events = repository.eventsFlow.value
        repository.scheduleRemindersForEvents(context, events)
        isRefreshing.value = false
    }

    /**
     * Sets the current day for the day view tab (tab 2) and updates the time range accordingly.
     *
     * @param date The LocalDate to set as the current day.
     */
    fun setDay(date: LocalDate) {
        if (_selectedTab.value != 2) {
            setTab(2)
        }
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.YEAR, date.year)
        cal.set(java.util.Calendar.MONTH, date.monthValue - 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, date.dayOfMonth)
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        val start = cal.timeInMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        cal.set(java.util.Calendar.MINUTE, 59)
        cal.set(java.util.Calendar.SECOND, 59)
        val end = cal.timeInMillis
        repository.setTimeRange(start, end)
    }

    /**
     * Parses a date string and sets the current day for the day view tab.
     * If the string is invalid, the error is ignored.
     *
     * @param dateString The date string to parse (in ISO format).
     */
    fun setDayFromString(dateString: String?) {
        if (dateString == null) return
        try {
            val date = LocalDate.parse(dateString)
            setDay(date)
        } catch (_: Exception) {
            // Ignore invalid date
        }
    }
}
