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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.getCurrentAppLocale
import space.midnightthoughts.nordiccalendar.util.Event
import space.midnightthoughts.nordiccalendar.viewmodels.CalendarViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.PriorityQueue

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val events = remember(calendarViewModel) {
        calendarViewModel.events
    }.collectAsState(initial = emptyList())
    val isRefreshing = remember(calendarViewModel) {
        calendarViewModel.isRefreshing
    }.collectAsState(initial = false)
    val selectedTab = remember(calendarViewModel) {
        calendarViewModel.selectedTab
    }.collectAsState(initial = 0)

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullToRefreshState,
        modifier = modifier,
        onRefresh = {
            calendarViewModel.refreshEvents()
        },
        isRefreshing = isRefreshing.value,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing.value,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                state = pullToRefreshState
            )
        },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CalendarTabBar(
                selectedTab = selectedTab.value,
                onTabSelected = { calendarViewModel.setTab(it) })
            DateRangeHeader(
                selectedTab = selectedTab.value,
                calendarViewModel = calendarViewModel,
                onPrev = {
                    when (selectedTab.value) {
                        0 -> calendarViewModel.prevMonth()
                        1 -> calendarViewModel.prevWeek()
                        2 -> calendarViewModel.prevDay()
                    }
                },
                onNext = {
                    when (selectedTab.value) {
                        0 -> calendarViewModel.nextMonth()
                        1 -> calendarViewModel.nextWeek()
                        2 -> calendarViewModel.nextDay()
                    }
                },
                onToday = {
                    when (selectedTab.value) {
                        0 -> calendarViewModel.setTodayMonth()
                        1 -> calendarViewModel.setTodayWeek()
                        2 -> calendarViewModel.setTodayDay()
                    }
                }
            )
            if (events.value.isEmpty()) {
                Text(stringResource(R.string.no_events_found), modifier = Modifier.padding(16.dp))
            } else if (selectedTab.value == 2) {
                DayView(
                    navController = navController,
                    calendarViewModel = calendarViewModel,
                )
            } else {
                EventList(
                    navController = navController,
                    calendarViewModel = calendarViewModel,
                    selectedTab = selectedTab.value
                )
            }
        }
    }
}

private fun assignColumns(events: List<Event>): List<Triple<Event, Int, Int>> {
    data class ActiveEvent(val endTime: Long, val col: Int)

    val sorted = events.sortedBy { it.startTime }
    val active = PriorityQueue(compareBy<ActiveEvent> { it.endTime })
    val freeColumns = PriorityQueue<Int>()
    val result = mutableListOf<Triple<Event, Int, Int>>()
    var maxColumns = 0

    for (event in sorted) {
        // Entferne abgelaufene Events und gib deren Spalten frei
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun DayView(
    navController: NavController,
    calendarViewModel: CalendarViewModel,
) {
    val hourHeightDp = 64.dp
    val timeColumnWidth = 64.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeightDp.toPx() }
    val dayStart = remember(calendarViewModel) { calendarViewModel.startMillis }.collectAsState()
    val dayEnd = remember(calendarViewModel) { calendarViewModel.endMillis }.collectAsState()
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
        modifier = Modifier
            .fillMaxSize()
    ) {
        val visibleHeightPx = with(density) { maxHeight.toPx() }
        val scrollTo = (nowOffsetY - visibleHeightPx / 2).toInt().coerceAtLeast(0)
        val scrollState = rememberScrollState(initial = scrollTo)

        val events = remember(calendarViewModel) { calendarViewModel.events }.collectAsState()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .semantics {
                    collectionInfo = CollectionInfo(
                        rowCount = events.value.size,
                        columnCount = 1,
                    )
                }
        ) {
            val maxWidthPx = with(density) { maxWidth.toPx() }

            // Stundenraster
            HourLines(
                dayStart = dayStart.value,
                hourHeightDp = hourHeightDp,
                timeColumnWidth = timeColumnWidth,
                hourFormat = hourFormat
            )

            // Events (wie gehabt, optimiert)
            val eventColumns = remember(events.value, maxWidthPx) { assignColumns(events.value) }
            events.value.forEach { event ->
                val triple = eventColumns.find { it.first.id == event.id }
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

                    val noTopCorners = event.startTime < dayStart.value
                    val noBottomCorners = event.endTime > dayEnd.value

                    val showStartTimeAsMidnight = shownStart == dayStart.value
                    val showEndTimeAsMidnight = shownEnd == dayEnd.value

                    key(event.id) {
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
                                    navController.navigate("eventDetails/${event.eventId}?tab=2") {
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

            // Jetzt-Linie (optimiert als eigene Composable)
            NowBar(
                nowOffsetY = nowOffsetY,
                hourHeightPx = hourHeightPx,
                now = now,
                hourFormat = hourFormat
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabTitles = listOf(
        stringResource(R.string.tab_month),
        stringResource(R.string.tab_week),
        stringResource(R.string.tab_day)
    )
    PrimaryTabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.semantics {
            collectionInfo = CollectionInfo(
                rowCount = 1,
                columnCount = tabTitles.size,
            )
        }
    ) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) },
                modifier = Modifier
                    .semantics {
                        collectionItemInfo = CollectionItemInfo(
                            0, 0, index, 0,
                        )
                    }
                    .testTag("tab_$index")
            )
        }
    }
}

@Composable
fun DateRangeHeader(
    selectedTab: Int,
    calendarViewModel: CalendarViewModel,
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
        0, 1 -> "${
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
            val cal = Calendar.getInstance()
            val today = cal.timeInMillis
            today >= startMillis.value && today <= endMillis.value
        }

        2 -> {
            val cal = Calendar.getInstance()
            val startDay = Calendar.getInstance().apply { timeInMillis = startMillis.value }
            cal.get(Calendar.YEAR) == startDay.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == startDay.get(Calendar.DAY_OF_YEAR)
        }

        else -> false
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPrev,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.previous_period)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rangeText, style = MaterialTheme.typography.titleMedium)
            if (!isToday) {
                Spacer(Modifier.width(8.dp))
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
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.next_period)
            )
        }
    }
}

@Composable
fun EventCard(
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
            topStart = androidx.compose.foundation.shape.ZeroCornerSize,
            topEnd = androidx.compose.foundation.shape.ZeroCornerSize,
            bottomStart = androidx.compose.foundation.shape.ZeroCornerSize,
            bottomEnd = androidx.compose.foundation.shape.ZeroCornerSize
        )
    } else if (noBottomCorners) {
        MaterialTheme.shapes.medium.copy(
            bottomStart = androidx.compose.foundation.shape.ZeroCornerSize,
            bottomEnd = androidx.compose.foundation.shape.ZeroCornerSize
        )
    } else if (noTopCorners) {
        MaterialTheme.shapes.medium.copy(
            topStart = androidx.compose.foundation.shape.ZeroCornerSize,
            topEnd = androidx.compose.foundation.shape.ZeroCornerSize
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

@Preview
@Composable
fun EventCardPreview() {
    val sampleEvent = Event(
        id = 1L,
        eventId = 1L,
        title = "Beispiel Event",
        description = "Dies ist eine Beschreibung des Beispiel-Events.",
        startTime = System.currentTimeMillis(),
        endTime = System.currentTimeMillis() + 3600000, // 1 Stunde später
        calendarId = 1L,
        location = "Beispielort",
        allDay = false,
        organizer = "Beispielorganisator",
        calendar = space.midnightthoughts.nordiccalendar.util.Calendar(
            id = 1L,
            name = "Beispielkalender",
            color = 0xFF6200EE, // Beispiel-Farbe
            selected = true,
            accountName = "Beispielkonto",
            accountType = "com.example",
            syncEvents = true,
            visible = true,
            displayName = "Beispielkalender",
        )
    )
    EventCard(
        event = sampleEvent,
        isCompact = false,
    )
}

@Composable
fun EventList(
    navController: NavController,
    calendarViewModel: CalendarViewModel,
    selectedTab: Int // Tab-Index als neuen Parameter
) {
    val events = remember(calendarViewModel) {
        calendarViewModel.events
    }.collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events.value) { event ->
            Text(
                event.title, modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        navController.navigate("eventDetails/${event.eventId}?tab=$selectedTab") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
        }
    }
}
