package space.midnightthoughts.nordiccalendar.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * MonthView displays the calendar in a monthly format, showing all days of the month
 * with events and provides navigation to view event details.
 *
 * @param modifier Modifier for styling and layout.
 * @param navController NavController for navigation actions.
 * @param monthViewModel Specialized ViewModel for month view operations.
 * @param events List of events to display.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthView(
    modifier: Modifier = Modifier,
    navController: NavController,
    monthViewModel: space.midnightthoughts.nordiccalendar.viewmodels.MonthViewModel,
    events: List<space.midnightthoughts.nordiccalendar.util.Event> = emptyList()
) {
    val startMillis by monthViewModel.startMillis.collectAsState()
    val today = LocalDate.now()
    val zoneId = ZoneId.systemDefault()
    val firstDay = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
    val daysInMonth = firstDay.lengthOfMonth()
    // Week starts on Monday (1=Monday, 7=Sunday)
    val firstDayOfWeek = 1
    val firstOfMonth = firstDay.withDayOfMonth(1)
    val firstOfMonthDayOfWeek = (firstOfMonth.dayOfWeek.value - firstDayOfWeek + 7) % 7
    val totalCells = ((daysInMonth + firstOfMonthDayOfWeek + 6) / 7) * 7
    val days = (0 until totalCells).map { cell ->
        val dayOfMonth = cell - firstOfMonthDayOfWeek + 1
        if (dayOfMonth in 1..daysInMonth) firstDay.withDayOfMonth(dayOfMonth) else null
    }
    val eventsByDay = events.groupBy { event ->
        Instant.ofEpochMilli(event.startTime).atZone(zoneId).toLocalDate()
    }
    Column(modifier) {
        // Weekday names as grid row
        Row(Modifier.fillMaxWidth()) {
            val locale = Locale.getDefault()
            for (i in 0..6) {
                val day = DayOfWeek.of(((i + firstDayOfWeek - 1) % 7) + 1)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        day.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        // Month grid with lines (only inner lines)
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = true
        ) {
            itemsIndexed(days) { idx, date ->
                val isToday = date == today
                val isCurrentMonth = date != null && date.month == firstDay.month
                val dayEvents = if (date != null) eventsByDay[date].orEmpty() else emptyList()
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        Modifier
                            .padding(4.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // Day number
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .aspectRatio(1f)
                                .then(
                                    if (isToday) Modifier.background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(50)
                                    ) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date?.dayOfMonth?.toString() ?: "",
                                color = when {
                                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                },
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                lineHeight = 8.sp,
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                        Spacer(Modifier.size(2.dp))
                        // Events as compact chips
                        val maxEvents = 3
                        dayEvents.take(maxEvents).forEach { event ->
                            CompactChip(
                                text = event.title,
                                backgroundColor = Color(event.calendar.color.toInt() or 0xFF000000.toInt()),
                                textColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(bottom = 1.dp)
                                    .clickable {
                                        navController.navigate("eventDetails/${event.eventId}") {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                            )
                        }
                        if (dayEvents.size > maxEvents) {
                            CompactChip(
                                text = "+${dayEvents.size - maxEvents} more",
                                backgroundColor = Color.Transparent,
                                textColor = MaterialTheme.colorScheme.primary,
                                center = true,
                                modifier = Modifier
                                    .padding(bottom = 1.dp)
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CompactChip displays a small, rounded chip for an event or a "+N more" indicator in the month view.
 *
 * @param text The text to display inside the chip.
 * @param backgroundColor The background color of the chip.
 * @param textColor The text color.
 * @param modifier Modifier for styling and layout.
 * @param borderColor Optional border color for the chip.
 * @param center If true, centers the text inside the chip.
 */
@Composable
fun CompactChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    center: Boolean? = false
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .height(20.dp)
            .fillMaxWidth()
            .then(
                if (borderColor != null) Modifier.border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp)
                ) else Modifier
            ),
        contentAlignment = if (center == true) Alignment.Center else Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = textColor,
            lineHeight = 12.sp,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 3.dp)
        )
    }
}