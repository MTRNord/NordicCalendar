package space.midnightthoughts.nordiccalendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.getCurrentAppLocale
import space.midnightthoughts.nordiccalendar.viewmodels.BaseCalendarViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date

/**
 * DateRangeHeader is a composable function that displays a header with the current date range
 * (month, week, or day) and navigation controls for moving to the previous/next period or jumping to today.
 *
 * The displayed range and navigation logic depend on the selectedTab:
 *   0 = month view, 1 = week view, 2 = day view.
 *
 * @param selectedTab The currently selected tab (0=month, 1=week, 2=day).
 * @param calendarViewModel The BaseCalendarViewModel providing start and end millis for the range.
 * @param onPrev Callback for navigating to the previous period.
 * @param onNext Callback for navigating to the next period.
 * @param onToday Callback for jumping to today.
 */
@Composable
fun DateRangeHeader(
    selectedTab: Int,
    calendarViewModel: BaseCalendarViewModel,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val startMillis = remember(calendarViewModel) {
        calendarViewModel.startMillis
    }.collectAsState(initial = System.currentTimeMillis())
    val endMillis = remember(calendarViewModel) {
        calendarViewModel.endMillis
    }.collectAsState(initial = System.currentTimeMillis())
    val appLocale = getCurrentAppLocale(LocalContext.current)
    val dateFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy", appLocale)

    val startDate = remember(startMillis.value) {
        Date(startMillis.value)
    }
    val endDate = remember(endMillis.value) {
        Date(endMillis.value)
    }

    val rangeText = when (selectedTab) {
        0 -> {
            val localDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val monthFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", appLocale)
            localDate.format(monthFormatter).replaceFirstChar { it.uppercase(appLocale) }
        }

        1 -> "${
            startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                .format(dateFormat)
        } – ${
            endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(dateFormat)
        }"

        2 -> startDate.toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate()
            .format(dateFormat)

        else -> ""
    }
    val isToday = when (selectedTab) {
        0 -> {
            val cal = Calendar.getInstance()
            val todayMonth = cal.get(Calendar.MONTH)
            val todayYear = cal.get(Calendar.YEAR)
            val startCal = Calendar.getInstance().apply { timeInMillis = startMillis.value }
            val endCal = Calendar.getInstance().apply { timeInMillis = endMillis.value }
            startCal.get(Calendar.MONTH) == todayMonth &&
                    startCal.get(Calendar.YEAR) == todayYear &&
                    endCal.get(Calendar.MONTH) == todayMonth &&
                    endCal.get(Calendar.YEAR) == todayYear
        }

        1 -> {
            val today = Calendar.getInstance()
            val todayMillis = today.timeInMillis
            todayMillis >= startMillis.value && todayMillis <= endMillis.value
        }

        2 -> {
            val today = Calendar.getInstance()
            val startCal = Calendar.getInstance().apply { timeInMillis = startMillis.value }
            today.get(Calendar.YEAR) == startCal.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == startCal.get(Calendar.DAY_OF_YEAR)
        }

        else -> false
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrev,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.previous_period)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = rangeText,
                style = MaterialTheme.typography.titleMedium,
            )


            if (!isToday) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onToday) {
                    Text(stringResource(R.string.today))
                }
            }
        }

        IconButton(
            onClick = onNext,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.next_period)
            )
        }
    }
}