package space.midnightthoughts.nordiccalendar.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.Calendar

class SidebarDrawerViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = CalendarRepository.getInstance()

    val calendars = repository.calendarsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun toggleCalendar(calendar: Calendar) {
        viewModelScope.launch {
            repository.setCalendarSelected(
                getApplication<Application>(),
                calendar.id,
                !calendar.selected
            )
        }
    }
}
