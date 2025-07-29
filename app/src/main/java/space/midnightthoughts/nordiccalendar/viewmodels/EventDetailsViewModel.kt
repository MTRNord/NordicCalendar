package space.midnightthoughts.nordiccalendar.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.data.CalendarRepository
import space.midnightthoughts.nordiccalendar.util.Event
import javax.inject.Inject

@HiltViewModel
class EventDetailsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: CalendarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
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
            val event = repository.getEventById(context, eventId)
            _event.value = event
        }
    }
}
