package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.Event
import javax.inject.Inject

/**
 * CalendarViewModel coordinates between the three specialized view models (Month, Week, Day)
 * and manages tab selection. This ensures each view maintains its own independent time range
 * while providing a unified interface for the CalendarScreen.
 *
 * @property context The application context injected by Hilt.
 * @property repository The CalendarRepository for accessing calendar and event data.
 * @property monthViewModel Specialized ViewModel for month view operations.
 * @property weekViewModel Specialized ViewModel for week view operations.
 * @property dayViewModel Specialized ViewModel for day view operations.
 * @property selectedTab StateFlow of the currently selected tab (0=month, 1=week, 2=day).
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
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
     * Centralized events flow that handles all event loading and reminder scheduling.
     */
    val events: StateFlow<List<Event>> = combine(
        repository.calendarsFlow,
        currentViewModel.startMillis,
        currentViewModel.endMillis
    ) { calendars, start, end ->
        val selectedIds = calendars.filter { it.selected }.map { it.id }
        repository.getEventsForCalendars(context, selectedIds, start, end)
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
                        repository.scheduleRemindersForEvents(context, eventList)
                    } catch (e: Exception) {
                        // Handle exception
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
        _isRefreshing.value = true
        currentViewModel.refreshEvents()
        _isRefreshing.value = false
    }
}
