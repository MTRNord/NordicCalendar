package space.midnightthoughts.nordiccalendar.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import space.midnightthoughts.nordiccalendar.data.CalendarRepository

/**
 * Base class for calendar view models providing common functionality for event management,
 * refreshing, and reminder scheduling.
 *
 * @property repository The CalendarRepository for accessing calendar and event data.
 */
abstract class BaseCalendarViewModel(
    protected val repository: CalendarRepository
) : ViewModel() {

    /**
     * Backing property for the current start time in milliseconds.
     */
    protected val _startMillis = MutableStateFlow(System.currentTimeMillis())

    /**
     * StateFlow of the current start time in milliseconds.
     */
    val startMillis: StateFlow<Long> = _startMillis.asStateFlow()

    /**
     * Backing property for the current end time in milliseconds.
     */
    protected val _endMillis = MutableStateFlow(System.currentTimeMillis())

    /**
     * StateFlow of the current end time in milliseconds.
     */
    val endMillis: StateFlow<Long> = _endMillis.asStateFlow()

    /**
     * StateFlow indicating if a refresh is in progress.
     */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /**
     * Sets the time range for this view and validates it.
     * Subclasses should override this to enforce their specific time range requirements.
     *
     * @param startMillis The start time in milliseconds.
     * @param endMillis The end time in milliseconds.
     */
    protected open fun setTimeRange(startMillis: Long, endMillis: Long) {
        if (endMillis <= startMillis) {
            Log.w(
                "BaseCalendarViewModel",
                "Invalid time range: end ($endMillis) <= start ($startMillis)"
            )
            return
        }
        _startMillis.value = startMillis
        _endMillis.value = endMillis
    }

    /**
     * Refreshes the events by triggering a recomposition of the events flow.
     */
    fun refreshEvents() {
        _isRefreshing.value = true
        // Force recomposition by slightly adjusting the time range
        val currentStart = _startMillis.value
        val currentEnd = _endMillis.value
        _startMillis.value = currentStart + 1
        _startMillis.value = currentStart
        _isRefreshing.value = false
    }

    /**
     * Navigates to the current time period (today) for this view type.
     * Subclasses must implement this to define what "today" means for their view.
     */
    abstract fun navigateToToday()

    /**
     * Navigates to the next time period for this view type.
     * Subclasses must implement this to define navigation behavior.
     */
    abstract fun navigateToNext()

    /**
     * Navigates to the previous time period for this view type.
     * Subclasses must implement this to define navigation behavior.
     */
    abstract fun navigateToPrevious()

    /**
     * Initializes the view with the default time range for this view type.
     * Subclasses must implement this to set their appropriate default range.
     */
    abstract fun initializeDefaultTimeRange()
}
