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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.util.ColorUtils
import space.midnightthoughts.nordiccalendar.util.Event
import space.midnightthoughts.nordiccalendar.viewmodels.MonthViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Displays the weekday header for the month view.
 *
 * @param firstDayOfWeek First day of week (1=Monday, 7=Sunday).
 */
@Composable
private fun MonthWeekdayHeader(
    firstDayOfWeek: Int
) {
    val locale = Locale.getDefault()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        for (i in 0..6) {
            val day = DayOfWeek.of(((i + firstDayOfWeek - 1) % 7) + 1)
            Box(
                modifier = Modifier.weight(1f), // Use weight instead of fixed width
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Vertical divider between days (except after last day)
            if (i < 6) {
                VerticalDivider(
                    modifier = Modifier.height(32.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

/**
 * Displays the month grid with continuous lines and proper cell layout.
 * Optimized version to reduce recomposition and improve performance.
 *
 * @param days List of dates for each cell (null for empty cells).
 * @param eventsByDay Events grouped by day.
 * @param today Today's date for highlighting.
 * @param firstDay First day of the month for comparison.
 * @param navController Navigation controller for event clicks.
 */
@Composable
private fun MonthGrid(
    days: List<LocalDate?>,
    eventsByDay: Map<LocalDate, List<Event>>,
    today: LocalDate,
    firstDay: LocalDate,
    navController: NavController
) {
    val rows = days.size / 7

    // Pre-calculate all cell data to avoid recomposition
    val cellData = remember(days, eventsByDay, today, firstDay) {
        days.mapIndexed { index, date ->
            val isToday = date == today
            val isCurrentMonth = date != null && date.month == firstDay.month
            val dayEvents = if (date != null) eventsByDay[date].orEmpty() else emptyList()

            Triple(
                Triple(date, isToday, isCurrentMonth),
                dayEvents,
                index
            )
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp) // Increase from 100dp to 120dp for more space
            ) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val (dateInfo, dayEvents, _) = cellData[cellIndex]
                    val (date, isToday, isCurrentMonth) = dateInfo

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Horizontal divider at top of cell (except first row)
                        if (row > 0) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }

                        // Cell background and content
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isCurrentMonth) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .padding(4.dp)
                        ) {
                            MonthDayCell(
                                date = date,
                                isToday = isToday,
                                isCurrentMonth = isCurrentMonth,
                                dayEvents = dayEvents,
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Vertical divider between columns (except after last column)
                    if (col < 6) {
                        VerticalDivider(
                            modifier = Modifier.height(120.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the content of a single day cell.
 *
 * @param date The date for this cell (null if empty).
 * @param isToday Whether this is today's date.
 * @param isCurrentMonth Whether this date is in the current month.
 * @param dayEvents Events for this day.
 * @param navController Navigation controller for event clicks.
 * @param modifier Modifier for styling.
 */
@Composable
private fun MonthDayCell(
    date: LocalDate?,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    dayEvents: List<Event>,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp) // Use spacedBy instead of padding on individual chips
    ) {
        // Day number
        Box(
            modifier = Modifier
                .size(28.dp)
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
                fontSize = 12.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(2.dp)
            )
        }

        Spacer(Modifier.height(1.dp))

        // Events as compact chips with correct colors
        val maxEvents = 3
        dayEvents.take(maxEvents).forEach { event ->
            // Use ColorUtils for consistent colors
            val backgroundColor = ColorUtils.longToColor(event.calendar.color)
            val textColor = ColorUtils.getContrastingTextColor(backgroundColor)

            CompactChip(
                text = event.title,
                backgroundColor = backgroundColor,
                textColor = textColor,
                modifier = Modifier
                    .fillMaxWidth()
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
                text = LocalContext.current.getString(
                    R.string.events_more,
                    dayEvents.size - maxEvents
                ),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                textColor = MaterialTheme.colorScheme.primary,
                center = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        // Navigate to calendar with day view tab and specific date
                        date?.let {
                            navController.navigate("calendar?tab=2&date=${it}") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
            )
        }
    }
}

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
    monthViewModel: MonthViewModel,
    events: List<Event> = emptyList()
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Weekday names header
        MonthWeekdayHeader(
            firstDayOfWeek = firstDayOfWeek
        )

        // Month grid with continuous lines
        MonthGrid(
            days = days,
            eventsByDay = eventsByDay,
            today = today,
            firstDay = firstDay,
            navController = navController
        )
    }
}

/**
 * CompactChip displays a small, rounded chip for an event or a "+N more" indicator in the month view.
 *
 * @param text The text to display inside the chip.
 * @param backgroundColor The background color of the chip.
 * @param textColor The text color.
 * @param modifier Modifier for styling and layout.
 * @param center If true, centers the text inside the chip.
 */
@Composable
fun CompactChip(
    text: String,
    backgroundColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    center: Boolean = false
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .height(20.dp) // Back to a more readable height
            .fillMaxWidth(),
        contentAlignment = if (center) Alignment.Center else Alignment.CenterStart
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