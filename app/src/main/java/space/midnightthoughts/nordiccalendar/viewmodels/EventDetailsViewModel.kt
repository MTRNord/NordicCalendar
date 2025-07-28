package space.midnightthoughts.nordiccalendar.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.Event

class EventDetailsViewModel(
    app: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {
    private val repository = CalendarRepository(app)
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    init {
        val eventId = savedStateHandle.get<String>("eventId")?.toLongOrNull()
        if (eventId != null) {
            loadEvent(eventId)
        }
    }

    private fun loadEvent(eventId: Long) {
        viewModelScope.launch {
            val event = repository.getEventById(eventId)
            _event.value = event
        }
    }
}
