package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
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
        MonthViewModel(context, repository, savedStateHandle)
    }

    /**
     * Specialized ViewModel for week view with week-specific time range management.
     */
    val weekViewModel: WeekViewModel by lazy {
        WeekViewModel(context, repository, savedStateHandle)
    }

    /**
     * Specialized ViewModel for day view with day-specific time range management.
     */
    val dayViewModel: DayViewModel by lazy {
        DayViewModel(context, repository, savedStateHandle)
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
     * Convenience properties that delegate to the current view model.
     */
    val events
        get() = when (_selectedTab.value) {
            0 -> monthViewModel.events
            1 -> weekViewModel.events
            2 -> dayViewModel.events
            else -> monthViewModel.events
        }
    
    val startMillis get() = currentViewModel.startMillis
    val endMillis get() = currentViewModel.endMillis
    val isRefreshing get() = currentViewModel.isRefreshing

    init {
        val tab = savedStateHandle.get<Int?>("tab")
        if (tab != null) {
            setTab(tab)
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
        currentViewModel.refreshEvents()
    }
}
