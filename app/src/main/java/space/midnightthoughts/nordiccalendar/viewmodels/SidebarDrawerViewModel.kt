package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.Calendar
import javax.inject.Inject

/**
 * SidebarDrawerViewModel is the ViewModel responsible for managing the state of the sidebar drawer,
 * including the list of calendars and their selection state.
 *
 * @property context The application context injected by Hilt.
 * @property repository The CalendarRepository for accessing calendar data.
 */
@HiltViewModel
class SidebarDrawerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: CalendarRepository
) : ViewModel() {
    /**
     * Flow that emits the list of calendars, sharing the state with a timeout of 5000 milliseconds.
     * This allows the UI to observe changes in the calendar list and update accordingly.
     */
    val calendars = repository.calendarsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    /**
     * Toggles the selection state of a calendar.
     *
     * @param calendar The calendar to toggle.
     */
    fun toggleCalendar(calendar: Calendar) {
        viewModelScope.launch {
            repository.setCalendarSelected(
                context,
                calendar.id,
                !calendar.selected
            )
        }
    }
}
