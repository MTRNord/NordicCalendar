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

@HiltViewModel
class SidebarDrawerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CalendarRepository
) : ViewModel() {
    val calendars = repository.calendarsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

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
