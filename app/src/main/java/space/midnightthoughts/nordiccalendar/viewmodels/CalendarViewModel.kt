package space.midnightthoughts.nordiccalendar.viewmodels

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.Event
import java.time.LocalDate
import javax.inject.Inject

/**
 * CalendarViewModel coordinates between the three specialized view models (Month, Week, Day)
 * and manages tab selection. This ensures each view maintains its own independent time range
 * while providing a unified interface for the CalendarScreen.
 *
 * @property repository The CalendarRepository for accessing calendar and event data.
 * @property monthViewModel Specialized ViewModel for month view operations.
 * @property weekViewModel Specialized ViewModel for week view operations.
 * @property dayViewModel Specialized ViewModel for day view operations.
 * @property selectedTab StateFlow of the currently selected tab (0=month, 1=week, 2=day).
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * Specialized ViewModel for month view with month-specific time range management.
     */
    val monthViewModel: MonthViewModel by lazy {
        MonthViewModel(repository, savedStateHandle)
    }

    /**
     * Specialized ViewModel for week view with week-specific time range management.
     */
    val weekViewModel: WeekViewModel by lazy {
        WeekViewModel(repository, savedStateHandle)
    }

    /**
     * Specialized ViewModel for day view with day-specific time range management.
     */
    val dayViewModel: DayViewModel by lazy {
        DayViewModel(repository, savedStateHandle)
    }

    /**
     * Backing property for the currently selected tab (0=month, 1=week, 2=day).
     */
    private val _selectedTab = MutableStateFlow(0)

    /**
     * StateFlow of the currently selected tab (0=month, 1=week, 2=day).
     */
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    /**
     * Returns the currently active ViewModel based on the selected tab.
     */
    val currentViewModel: BaseCalendarViewModel
        get() = when (_selectedTab.value) {
            0 -> monthViewModel
            1 -> weekViewModel
            2 -> dayViewModel
            else -> monthViewModel
        }

    /**
     * Optimized events flow that only loads events for the currently active view.
     * This prevents unnecessary database queries when switching tabs.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val events: StateFlow<List<Event>> = combine(
        repository.calendarsFlow,
        _selectedTab
    ) { calendars, selectedTab ->
        val selectedIds = calendars.filter { it.selected }.map { it.id }
        selectedTab to selectedIds
    }.flatMapLatest { (selectedTab, selectedIds) ->
        // Only observe the time range for the currently active view
        val activeViewModel = when (selectedTab) {
            0 -> monthViewModel
            1 -> weekViewModel
            2 -> dayViewModel
            else -> monthViewModel
        }

        combine(
            activeViewModel.startMillis,
            activeViewModel.endMillis
        ) { start, end ->
            repository.getEventsForCalendarsAsync(selectedIds, start, end)
        }.flatMapLatest { it }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    /**
     * StateFlow indicating if a refresh is in progress.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        val tab = savedStateHandle.get<Int?>("tab")
        if (tab != null) {
            setTab(tab)
        }

        // Schedule reminders in background when events change
        viewModelScope.launch(Dispatchers.IO) {
            events.collect { eventList ->
                if (eventList.isNotEmpty()) {
                    try {
                        // Repository handles context internally now
                        repository.scheduleRemindersForEventsAsync(eventList)
                    } catch (e: Exception) {
                        Log.e(
                            "CalendarViewModel",
                            "Failed to schedule reminders for ${eventList.size} events",
                            e
                        )
                    }
                }
            }
        }
    }

    /**
     * Sets the currently selected tab and ensures the corresponding view model is active.
     *
     * @param tab The tab index to select (0=month, 1=week, 2=day).
     */
    fun setTab(tab: Int) {
        if (tab in 0..2) {
            _selectedTab.value = tab
        }
    }

    /**
     * Delegates navigation to the previous period to the current view model.
     */
    fun navigateToPrevious() {
        currentViewModel.navigateToPrevious()
    }

    /**
     * Delegates navigation to the next period to the current view model.
     */
    fun navigateToNext() {
        currentViewModel.navigateToNext()
    }

    /**
     * Delegates navigation to today to the current view model.
     */
    fun navigateToToday() {
        currentViewModel.navigateToToday()
    }

    /**
     * Delegates refresh to the current view model.
     */
    fun refreshEvents() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                currentViewModel.refreshEvents()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setTabAndDate(tabFromNav: Int, dateFromNav: String?) {
        setTab(tabFromNav)
        dateFromNav?.let { date ->
            when (tabFromNav) {
                0 -> {}
                1 -> {}
                2 -> {
                    val parsedLocalDate: LocalDate = try {
                        LocalDate.parse(date)
                    } catch (e: Exception) {
                        Log.e("CalendarViewModel", "Invalid date format: $date", e)
                        LocalDate.now()
                    }
                    dayViewModel.navigateToDate(parsedLocalDate)
                }
            }
        } ?: run {
            // If no date is provided, navigate to today
            currentViewModel.navigateToToday()
        }
    }
}
