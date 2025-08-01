package space.midnightthoughts.nordiccalendar.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import space.midnightthoughts.nordiccalendar.getCurrentAppLocale
import space.midnightthoughts.nordiccalendar.util.ColorUtils
import space.midnightthoughts.nordiccalendar.util.Event
import space.midnightthoughts.nordiccalendar.viewmodels.WeekViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale
import java.util.PriorityQueue

/**
 * Assigns columns to events within each day so that overlapping events are displayed side by side.
 * Each event is assigned a column index and the total number of columns for its overlap group within that day.
 *
 * @param events List of events for a specific day.
 * @return List of Triple<Event, columnIndex, maxColumns> for layout.
 */
private fun assignDayColumns(events: List<Event>): List<Triple<Event, Int, Int>> {
    data class ActiveEvent(val endTime: Long, val col: Int)

    val sorted = events.sortedBy { it.startTime }
    val active = PriorityQueue(compareBy<ActiveEvent> { it.endTime })
    val freeColumns = PriorityQueue<Int>()
    val result = mutableListOf<Triple<Event, Int, Int>>()

    for (event in sorted) {
        // Remove expired events and free their columns
        while (active.isNotEmpty() && active.peek()?.endTime!! <= event.startTime) {
            freeColumns.add(active.poll()?.col)
        }
        val col = if (freeColumns.isNotEmpty()) freeColumns.poll() else active.size
        active.add(ActiveEvent(event.endTime, col))

        // For each event, maxColumns is the number of currently active events (including this one)
        val maxColumns = active.size
        result.add(Triple(event, col, maxColumns))
    }
    return result
}

/**
 * Displays the week header with day names and dates.
 *
 * @param weekStart The start of the week (Monday).
 * @param dayColumnWidth Width of each day column.
 * @param timeColumnWidth Width of the time label column.
 */
@Composable
private fun WeekHeader(
    weekStart: LocalDate,
    dayColumnWidth: Dp,
    timeColumnWidth: Dp
) {
    val locale = Locale.getDefault()
    val today = LocalDate.now()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Empty space for time column
        Spacer(modifier = Modifier.width(timeColumnWidth))

        // Day headers
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val isToday = date == today

            Box(
                modifier = Modifier.width(dayColumnWidth),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .then(
                                if (isToday) Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.shapes.small
                                ) else Modifier
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays the hour grid for the week, with time labels and horizontal dividers for each hour.
 *
 * @param weekStart Start of the week.
 * @param hourHeightDp Height of each hour row in dp.
 * @param timeColumnWidth Width of the time label column in dp.
 * @param dayColumnWidth Width of each day column in dp.
 * @param hourFormat DateTimeFormatter for the hour labels.
 */
@Composable
private fun WeekHourGrid(
    weekStart: LocalDate,
    hourHeightDp: Dp,
    timeColumnWidth: Dp,
    dayColumnWidth: Dp,
    hourFormat: DateTimeFormatter
) {
    val zoneId = ZoneId.systemDefault()
    val dayStartMillis = weekStart.atStartOfDay(zoneId).toInstant().toEpochMilli()

    Column(modifier = Modifier.fillMaxWidth()) {
        for (hour in 0..23) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(hourHeightDp)
                    .fillMaxWidth()
            ) {
                // Time label
                Box(
                    modifier = Modifier.width(timeColumnWidth),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Text(
                        text = Date(dayStartMillis + hour * 60 * 60 * 1000).toInstant()
                            .atZone(zoneId).toLocalDateTime().format(hourFormat),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                    )
                }

                // Day columns with dividers
                for (dayIndex in 0..6) {
                    Box(
                        modifier = Modifier.width(dayColumnWidth)
                    ) {
                        // Horizontal divider
                        if (hour > 0) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Vertical divider between days (except after last day)
                    if (dayIndex < 6) {
                        VerticalDivider(
                            modifier = Modifier.height(hourHeightDp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays a red bar indicating the current time ("now") across all days in the week view.
 * The bar is only shown if the current time is within the visible range and on a day within the week.
 *
 * @param weekStart Start of the week.
 * @param nowOffsetY Vertical offset in pixels for the bar position.
 * @param hourHeightPx Height of one hour in pixels.
 * @param timeColumnWidth Width of the time column.
 * @param dayColumnWidth Width of each day column.
 * @param now Current time in milliseconds.
 * @param hourFormat Formatter for displaying the time label.
 */
@Composable
private fun WeekNowBar(
    weekStart: LocalDate,
    nowOffsetY: Float,
    hourHeightPx: Float,
    timeColumnWidth: Dp,
    dayColumnWidth: Dp,
    now: Long,
    hourFormat: DateTimeFormatter
) {
    val lineColor = MaterialTheme.colorScheme.error
    val today = LocalDate.now()
    val todayIndex = today.toEpochDay() - weekStart.toEpochDay()

    // Only show if today is within the current week and within the hour range
    if (todayIndex in 0..6 && nowOffsetY >= 0f && nowOffsetY <= hourHeightPx * 24f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, nowOffsetY.toInt()) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time label
            Box(
                modifier = Modifier
                    .width(timeColumnWidth)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = lineColor,
                            shape = MaterialTheme.shapes.extraSmall
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = Date(now).toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                            .format(hourFormat),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Now line across all days
            for (dayIndex in 0..6) {
                Box(modifier = Modifier.width(dayColumnWidth)) {
                    if (dayIndex == todayIndex.toInt()) {
                        // Red line for today
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                        ) {
                            drawLine(
                                color = lineColor,
                                start = Offset(0f, size.height / 2),
                                end = Offset(size.width, size.height / 2),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Space for vertical dividers
                if (dayIndex < 6) {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }
        }
    }
}

/**
 * WeekView displays the calendar in a weekly format, showing events for 7 days side by side.
 * Similar to DayView but with multiple day columns arranged horizontally.
 *
 * @param modifier Modifier for styling and layout.
 * @param navController NavController for navigation actions.
 * @param weekViewModel Specialized ViewModel for week view operations.
 * @param events List of events to display.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun WeekView(
    modifier: Modifier = Modifier,
    navController: NavController,
    weekViewModel: WeekViewModel,
    events: List<Event> = emptyList()
) {
    val hourHeightDp = 64.dp
    val timeColumnWidth = 64.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeightDp.toPx() }

    val weekStartMillis by weekViewModel.startMillis.collectAsState()
    val weekEndMillis by weekViewModel.endMillis.collectAsState()
    val appLocale = getCurrentAppLocale(LocalContext.current)
    val hourFormat = DateTimeFormatter.ofPattern("HH:mm", appLocale)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(now) {
        now = System.currentTimeMillis()
        delay(60000) // Update every minute for week view
    }

    val zoneId = ZoneId.systemDefault()
    val weekStart = Instant.ofEpochMilli(weekStartMillis).atZone(zoneId).toLocalDate()
    val today = LocalDate.now()

    // Fix: Calculate now offset more precisely to align with grid
    val todayStartMillis = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val nowMinutes = ((now - todayStartMillis) / 60000f)

    // Align with grid by using exact same calculation as hour grid positions
    val nowOffsetY = nowMinutes * (hourHeightPx / 60f)


    BoxWithConstraints(modifier = modifier) {
        val totalWidth = maxWidth - timeColumnWidth
        val dayColumnWidth = totalWidth / 7

        val visibleHeightPx = with(density) { maxHeight.toPx() }
        val scrollTo = (nowOffsetY - visibleHeightPx / 2).toInt().coerceAtLeast(0)
        val scrollState =
            rememberScrollState(initial = if (today >= weekStart && today <= weekStart.plusDays(6)) scrollTo else 0)

        Column(modifier = Modifier.fillMaxSize()) {
            // Week header with day names and dates
            WeekHeader(
                weekStart = weekStart,
                dayColumnWidth = dayColumnWidth,
                timeColumnWidth = timeColumnWidth
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .semantics {
                        collectionInfo = CollectionInfo(
                            rowCount = events.size,
                            columnCount = 7,
                        )
                    }
            ) {
                // Hour grid background
                WeekHourGrid(
                    weekStart = weekStart,
                    hourHeightDp = hourHeightDp,
                    timeColumnWidth = timeColumnWidth,
                    dayColumnWidth = dayColumnWidth,
                    hourFormat = hourFormat
                )

                // Group events by day
                val eventsByDay = events.groupBy { event ->
                    Instant.ofEpochMilli(event.startTime).atZone(zoneId).toLocalDate()
                }

                // Display events for each day
                for (dayIndex in 0..6) {
                    val currentDay = weekStart.plusDays(dayIndex.toLong())
                    val dayEvents = eventsByDay[currentDay] ?: emptyList()
                    val dayStartMillis = currentDay.atStartOfDay(zoneId).toInstant().toEpochMilli()
                    val dayEndMillis =
                        currentDay.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

                    if (dayEvents.isNotEmpty()) {
                        val eventColumns = remember(dayEvents) { assignDayColumns(dayEvents) }

                        dayEvents.forEach { event ->
                            val triple =
                                eventColumns.find { it.first.eventId == event.eventId && it.first.calendar.id == event.calendar.id }
                            if (triple != null) {
                                val (_, col, maxColumns) = triple
                                val shownStart = maxOf(event.startTime, dayStartMillis)
                                val shownEnd = minOf(event.endTime, dayEndMillis)
                                val startMinutes = ((shownStart - dayStartMillis) / 60000f)
                                val endMinutes = ((shownEnd - dayStartMillis) / 60000f)
                                val offsetY = (startMinutes / 60f) * hourHeightPx
                                val eventHeightPx =
                                    ((endMinutes - startMinutes) / 60f) * hourHeightPx
                                val columnWidthPx =
                                    with(density) { dayColumnWidth.toPx() } / maxColumns
                                val offsetX =
                                    (timeColumnWidth + dayColumnWidth * dayIndex).value.let { baseX ->
                                        with(density) { (baseX.dp + (col * columnWidthPx / density.density).dp).toPx() }
                                    }.toInt()

                                val minCardHeightDp = 40.dp
                                val isCompact =
                                    with(density) { eventHeightPx.toDp() } < minCardHeightDp

                                // Whether the event starts before the visible day
                                val noTopCorners = event.startTime < dayStartMillis
                                // Whether the event ends after the visible day
                                val noBottomCorners = event.endTime > dayEndMillis

                                key(event.eventId, event.calendar.id, dayIndex) {
                                    Box(
                                        modifier = Modifier
                                            .offset { IntOffset(offsetX, offsetY.toInt()) }
                                            .width(with(density) { (columnWidthPx / density.density).dp })
                                            .height(with(density) { eventHeightPx.toDp() })
                                    ) {
                                        WeekEventCard(
                                            event = event,
                                            isCompact = isCompact,
                                            onClick = {
                                                navController.navigate(
                                                    "eventDetails/${event.eventId}?tab=1"
                                                ) {
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            noTopCorners = noTopCorners,
                                            noBottomCorners = noBottomCorners,
                                            eventStartOverride = shownStart,
                                            eventEndOverride = shownEnd
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Now bar
                WeekNowBar(
                    weekStart = weekStart,
                    nowOffsetY = nowOffsetY,
                    hourHeightPx = hourHeightPx,
                    timeColumnWidth = timeColumnWidth,
                    dayColumnWidth = dayColumnWidth,
                    now = now,
                    hourFormat = hourFormat
                )
            }
        }
    }
}

/**
 * Card composable for displaying a single event in the week view.
 * Optimized for smaller width compared to day view.
 *
 * @param event The event to display.
 * @param isCompact Whether to use a compact layout (for short events).
 * @param modifier Modifier for styling and layout.
 * @param onClick Optional callback for click actions.
 * @param noBottomCorners If true, bottom corners are not rounded.
 * @param noTopCorners If true, top corners are not rounded.
 * @param eventStartOverride Optional override for the event start time.
 * @param eventEndOverride Optional override for the event end time.
 */
@Composable
private fun WeekEventCard(
    event: Event,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    noBottomCorners: Boolean = false,
    noTopCorners: Boolean = false,
    eventStartOverride: Long = 0L,
    eventEndOverride: Long = 0L,
) {
    val appLocale = getCurrentAppLocale(LocalContext.current)
    val hourFormat = DateTimeFormatter.ofPattern("HH:mm", appLocale)
    val startDate = remember(eventStartOverride.takeIf { it > 0L } ?: event.startTime) {
        Date(eventStartOverride.takeIf { it > 0L } ?: event.startTime)
    }
    val endDate = remember(eventEndOverride.takeIf { it > 0L } ?: event.endTime) {
        Date(eventEndOverride.takeIf { it > 0L } ?: event.endTime)
    }

    // Use calendar color for background and calculate contrasting text color
    val backgroundColor =
        ColorUtils.longToColor(event.calendar.color)
    val textColor = ColorUtils.getContrastingTextColor(
        backgroundColor
    )

    val shape = if (noTopCorners && noBottomCorners) {
        MaterialTheme.shapes.small.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize,
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize
        )
    } else if (noBottomCorners) {
        MaterialTheme.shapes.small.copy(
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize
        )
    } else if (noTopCorners) {
        MaterialTheme.shapes.small.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize
        )
    } else {
        MaterialTheme.shapes.small
    }

    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 20.dp)
            .padding(end = 2.dp, bottom = 1.dp)
            .semantics {
                collectionItemInfo = CollectionItemInfo(0, 0, 0, 0)
            }
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(
            1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 4.dp,
                    vertical = if (isCompact) 2.dp else 6.dp,
                )
                .fillMaxSize(),
            verticalArrangement = if (isCompact) Arrangement.Center else Arrangement.Top,
        ) {
            Text(
                text = event.title,
                overflow = TextOverflow.Ellipsis,
                maxLines = if (isCompact) 1 else 2,
                style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                color = textColor,
                modifier = Modifier.semantics { heading() }
            )

            if (!isCompact) {
                val startTimeText = startDate.toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDateTime().format(hourFormat)
                val endTimeText = endDate.toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDateTime().format(hourFormat)

                Text(
                    text = "$startTimeText-$endTimeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}