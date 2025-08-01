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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import space.midnightthoughts.nordiccalendar.getCurrentAppLocale
import space.midnightthoughts.nordiccalendar.util.Event
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.PriorityQueue

/**
 * Assigns columns to events so that overlapping events are displayed side by side.
 * Each event is assigned a column index and the total number of columns for its overlap group.
 *
 * @param events List of events to assign columns to.
 * @return List of Triple<Event, columnIndex, maxColumns> for layout.
 */
private fun assignColumns(events: List<Event>): List<Triple<Event, Int, Int>> {
    data class ActiveEvent(val endTime: Long, val col: Int)

    val sorted = events.sortedBy { it.startTime }
    val active = PriorityQueue(compareBy<ActiveEvent> { it.endTime })
    val freeColumns = PriorityQueue<Int>()
    val result = mutableListOf<Triple<Event, Int, Int>>()
    var maxColumns = 0

    for (event in sorted) {
        // Remove expired events and free their columns
        while (active.isNotEmpty() && active.peek()?.endTime!! <= event.startTime) {
            freeColumns.add(active.poll()?.col)
        }
        val col = if (freeColumns.isNotEmpty()) freeColumns.poll() else active.size
        active.add(ActiveEvent(event.endTime, col))
        maxColumns = maxOf(maxColumns, active.size)
        result.add(Triple(event, col, maxColumns))
    }
    return result
}

/**
 * Displays the hour grid for a day, with time labels and horizontal dividers for each hour.
 *
 * @param dayStart Start time of the day in milliseconds.
 * @param hourHeightDp Height of each hour row in dp.
 * @param timeColumnWidth Width of the time label column in dp.
 * @param hourFormat DateTimeFormatter for the hour labels.
 */
@Composable
private fun HourLines(
    dayStart: Long,
    hourHeightDp: Dp,
    timeColumnWidth: Dp,
    hourFormat: DateTimeFormatter
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        for (hour in 0..24) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(hourHeightDp)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    Date(dayStart + hour * 60 * 60 * 1000).toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime().format(hourFormat),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(timeColumnWidth)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Displays a red bar indicating the current time ("now") on the day view.
 * The bar is only shown if the current time is within the visible range.
 *
 * @param nowOffsetY Vertical offset in pixels for the bar position.
 * @param hourHeightPx Height of one hour in pixels.
 * @param now Current time in milliseconds.
 * @param hourFormat Formatter for displaying the time label.
 */
@Composable
private fun NowBar(
    nowOffsetY: Float,
    hourHeightPx: Float,
    now: Long,
    hourFormat: DateTimeFormatter
) {
    val lineColor = MaterialTheme.colorScheme.error
    if (nowOffsetY >= 0f && nowOffsetY <= hourHeightPx * 24.5f) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, nowOffsetY.toInt()) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .background(
                        color = lineColor,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    Date(now).toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime()
                        .format(hourFormat),
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(end = 16.dp)
            ) {
                drawLine(
                    color = lineColor,
                    start = Offset(
                        0f,
                        size.height / 2
                    ),
                    end = Offset(
                        size.width,
                        size.height / 2
                    ),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Main composable for displaying the day view of the calendar.
 * Shows a scrollable list of hours, events, and a bar for the current time.
 *
 * @param modifier Modifier for styling and layout.
 * @param navController NavController for navigation actions.
 * @param dayViewModel Specialized ViewModel for day view operations.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DayView(
    modifier: Modifier = Modifier,
    navController: NavController,
    dayViewModel: space.midnightthoughts.nordiccalendar.viewmodels.DayViewModel,
    events: List<space.midnightthoughts.nordiccalendar.util.Event> = emptyList()
) {
    val hourHeightDp = 64.dp
    val timeColumnWidth = 64.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeightDp.toPx() }

    val dayStart = remember(dayViewModel) { dayViewModel.startMillis }.collectAsState()
    val dayEnd = remember(dayViewModel) { dayViewModel.endMillis }.collectAsState()
    val appLocale = getCurrentAppLocale(LocalContext.current)
    val hourFormat = DateTimeFormatter.ofPattern("HH:mm", appLocale)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(now) {
        now = System.currentTimeMillis()
        delay(1000)
    }
    val nowMinutes = ((now - dayStart.value) / 60000f)
    val nowOffsetY = (nowMinutes / 60f) * hourHeightPx

    BoxWithConstraints(
        modifier = modifier
    ) {
        val visibleHeightPx = with(density) { maxHeight.toPx() }
        val scrollTo = (nowOffsetY - visibleHeightPx / 2).toInt().coerceAtLeast(0)
        val scrollState = rememberScrollState(initial = scrollTo)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .semantics {
                    collectionInfo = CollectionInfo(
                        rowCount = events.size,
                        columnCount = 1,
                    )
                }
        ) {
            val maxWidthPx = with(density) { maxWidth.toPx() }

            // Hour grid
            HourLines(
                dayStart = dayStart.value,
                hourHeightDp = hourHeightDp,
                timeColumnWidth = timeColumnWidth,
                hourFormat = hourFormat
            )

            // Events (optimized as before)
            val eventColumns = remember(events, maxWidthPx) { assignColumns(events) }
            events.forEach { event ->
                val triple =
                    eventColumns.find { it.first.eventId == event.eventId && it.first.calendar.id == event.calendar.id }
                if (triple != null) {
                    val (event, col, maxColumns) = triple
                    val shownStart = maxOf(event.startTime, dayStart.value)
                    val shownEnd = minOf(event.endTime, dayEnd.value)
                    val startMinutes = ((shownStart - dayStart.value) / 60000f)
                    val endMinutes = ((shownEnd - dayStart.value) / 60000f)
                    val offsetY = (startMinutes / 60f) * hourHeightPx + (hourHeightPx / 2f)
                    val eventHeightPx = ((endMinutes - startMinutes) / 60f) * hourHeightPx
                    val columnWidthPx =
                        (maxWidthPx - with(density) { timeColumnWidth.toPx() }) / maxColumns
                    val offsetX =
                        (col * columnWidthPx).toInt() + with(density) { timeColumnWidth.toPx() }.toInt()
                    val minCardHeightDp = 60.dp
                    val isCompact = with(density) { eventHeightPx.toDp() } < minCardHeightDp

                    // Whether the event starts before the visible day (no top corners)
                    val noTopCorners = event.startTime < dayStart.value
                    // Whether the event ends after the visible day (no bottom corners)
                    val noBottomCorners = event.endTime > dayEnd.value

                    // Show start time as midnight if event starts at the beginning of the day
                    val showStartTimeAsMidnight = shownStart == dayStart.value
                    // Show end time as midnight if event ends at the end of the day
                    val showEndTimeAsMidnight = shownEnd == dayEnd.value

                    key(event.eventId, event.calendar.id) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(offsetX, offsetY.toInt()) }
                                .width(with(density) { columnWidthPx.toDp() })
                                .height(with(density) { eventHeightPx.toDp() })
                        ) {
                            EventCard(
                                event = event,
                                isCompact = isCompact,
                                onClick = {
                                    navController.navigate(
                                        "eventDetails/${event.eventId}?tab=2"
                                    ) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                noTopCorners = noTopCorners,
                                noBottomCorners = noBottomCorners,
                                showStartTimeAsMidnight = showStartTimeAsMidnight,
                                showEndTimeAsMidnight = showEndTimeAsMidnight,
                                eventStartOverride = shownStart,
                                eventEndOverride = shownEnd
                            )
                        }
                    }
                }
            }

            // Now bar (optimized as its own composable)
            NowBar(
                nowOffsetY = nowOffsetY,
                hourHeightPx = hourHeightPx,
                now = now,
                hourFormat = hourFormat
            )
        }
    }
}

/**
 * Card composable for displaying a single event in the day view.
 * Handles compact and expanded layouts, corner rounding, and click actions.
 *
 * @param event The event to display.
 * @param isCompact Whether to use a compact layout (for short events).
 * @param modifier Modifier for styling and layout.
 * @param onClick Optional callback for click actions.
 * @param index Index of the event (for accessibility semantics).
 * @param noBottomCorners If true, bottom corners are not rounded.
 * @param noTopCorners If true, top corners are not rounded.
 * @param showStartTimeAsMidnight If true, show start time as "00:00".
 * @param showEndTimeAsMidnight If true, show end time as "24:00".
 * @param eventStartOverride Optional override for the event start time.
 * @param eventEndOverride Optional override for the event end time.
 */
@Composable
private fun EventCard(
    event: Event,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    index: Int = 0,
    noBottomCorners: Boolean = false,
    noTopCorners: Boolean = false,
    showStartTimeAsMidnight: Boolean = false,
    showEndTimeAsMidnight: Boolean = false,
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
    val shape = if (noTopCorners && noBottomCorners) {
        MaterialTheme.shapes.medium.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize,
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize
        )
    } else if (noBottomCorners) {
        MaterialTheme.shapes.medium.copy(
            bottomStart = ZeroCornerSize,
            bottomEnd = ZeroCornerSize
        )
    } else if (noTopCorners) {
        MaterialTheme.shapes.medium.copy(
            topStart = ZeroCornerSize,
            topEnd = ZeroCornerSize
        )
    } else {
        MaterialTheme.shapes.medium
    }
    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 24.dp)
            .padding(end = 8.dp)
            .semantics {
                collectionItemInfo = CollectionItemInfo(
                    index, 0, 0, 0,
                )
            }
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(
            1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 12.dp,
                    vertical = if (isCompact) 4.dp else 12.dp,
                )
                .fillMaxSize(),
            verticalArrangement = if (isCompact) Arrangement.Center else Arrangement.Top,
        ) {
            Text(
                event.title,
                overflow = TextOverflow.Ellipsis,
                style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
                modifier = Modifier.semantics { heading() }
            )
            if (!isCompact) {
                Spacer(modifier = Modifier.height(4.dp))
                val startTimeText = when {
                    showStartTimeAsMidnight -> "00:00"
                    else -> startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        .format(hourFormat)
                }
                val endTimeText = when {
                    showEndTimeAsMidnight -> "24:00"
                    else -> endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                        .format(hourFormat)
                }
                Text(
                    "$startTimeText - $endTimeText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!event.description.isNullOrBlank()) {
                    Text(
                        event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}