package space.midnightthoughts.nordiccalendar.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import space.midnightthoughts.nordiccalendar.data.CalendarRepository

class CalendarViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = CalendarRepository.getInstance()

    val events = repository.eventsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val startMillis = repository.startMillis
    val endMillis = repository.endMillis

    val isRefreshing = MutableStateFlow(false)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    init {
        // Repository initialisieren (Context benötigt)
        repository.initialize(app)
        setTab(0)
    }

    fun setTab(tab: Int) {
        _selectedTab.value = tab
        val start = getDefaultStartMillis(tab)
        val end = getDefaultEndMillis(tab)
        repository.setTimeRange(start, end)
    }

    private fun getDefaultStartMillis(tab: Int): Long {
        val cal = java.util.Calendar.getInstance()
        when (tab) {
            0 -> { // Monat
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            1 -> { // Woche
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            2 -> { // Tag
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                return cal.timeInMillis
            }

            else -> return cal.timeInMillis
        }
    }

    private fun getDefaultEndMillis(tab: Int): Long {
        val cal = java.util.Calendar.getInstance()
        when (tab) {
            0 -> { // Monat
                cal.set(
                    java.util.Calendar.DAY_OF_MONTH,
                    cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                )
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            1 -> { // Woche
                cal.set(java.util.Calendar.DAY_OF_WEEK, cal.firstDayOfWeek + 6)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            2 -> { // Tag
                cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
                cal.set(java.util.Calendar.MINUTE, 59)
                cal.set(java.util.Calendar.SECOND, 59)
                return cal.timeInMillis
            }

            else -> return cal.timeInMillis
        }
    }

    // Die folgenden Methoden ändern nur noch den Zeitraum im Repository
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
}
