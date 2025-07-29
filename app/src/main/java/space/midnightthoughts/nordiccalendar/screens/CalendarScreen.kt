package space.midnightthoughts.nordiccalendar.screens

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.getCurrentAppLocale
import space.midnightthoughts.nordiccalendar.util.Event
import space.midnightthoughts.nordiccalendar.viewmodels.CalendarViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date


@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    calendarViewModel: CalendarViewModel = viewModel(),
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    navController: NavController,
) {
    val events = remember(calendarViewModel) {
        calendarViewModel.events
    }.collectAsState(initial = emptyList())
    val isRefreshing = remember(calendarViewModel) {
        calendarViewModel.isRefreshing
    }.collectAsState(initial = false)

    Log.d(
        "CalendarScreen",
        "Rendering with selectedTab: $selectedTab, events count: ${events.value.size}, isRefreshing: $isRefreshing"
    )
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullToRefreshState,
        modifier = modifier,
        onRefresh = {
            // updateEvents gibt es nicht mehr im ViewModel, stattdessen Tab neu setzen, um Zeitraum zu triggern
            calendarViewModel.setTab(selectedTab)
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
            CalendarTabBar(selectedTab = selectedTab, onTabSelected = onTabSelected)
            // DateRangeHeader sollte direkt unter den Tabs stehen, IMMER sichtbar
            DateRangeHeader(
                selectedTab = selectedTab,
                calendarViewModel = calendarViewModel,
                onPrev = {
                    when (selectedTab) {
                        0 -> calendarViewModel.prevMonth()
                        1 -> calendarViewModel.prevWeek()
                        2 -> calendarViewModel.prevDay()
                    }
                },
                onNext = {
                    when (selectedTab) {
                        0 -> calendarViewModel.nextMonth()
                        1 -> calendarViewModel.nextWeek()
                        2 -> calendarViewModel.nextDay()
                    }
                },
                onToday = {
                    when (selectedTab) {
                        0 -> calendarViewModel.setTodayMonth()
                        1 -> calendarViewModel.setTodayWeek()
                        2 -> calendarViewModel.setTodayDay()
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            if (events.value.isEmpty()) {
                Text(stringResource(R.string.no_events_found), modifier = Modifier.padding(16.dp))
            } else if (selectedTab == 2) {
                DayView(
                    navController = navController,
                    calendarViewModel = calendarViewModel
                )
            } else {
                EventList(
                    navController = navController,
                    calendarViewModel = calendarViewModel
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DayView(
    navController: NavController,
    calendarViewModel: CalendarViewModel,
) {
    val events = remember(calendarViewModel) {
        calendarViewModel.events
    }.collectAsState(initial = emptyList())

    val hourHeightDp = 64.dp
    val timeColumnWidth = 64.dp
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeightDp.toPx() }
    val dayStart = events.value.minOfOrNull { it.startTime }?.let {
        val cal = Calendar.getInstance()
        cal.timeInMillis = it
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    } ?: Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val appLocale = getCurrentAppLocale(LocalContext.current)
    val hourFormat = DateTimeFormatter.ofPattern("HH:mm", appLocale)
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Aktualisiere die "Jetzt"-Linie jede Sekunde
    LaunchedEffect(now) {
        now = System.currentTimeMillis()
        delay(1000)
    }
    val nowMinutes = ((now - dayStart) / 60000f)
    val nowOffsetY = ((nowMinutes + 30f) / 60f) * hourHeightPx
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val visibleHeightPx = with(density) { 600.dp.toPx() }
            val scrollTo = (nowOffsetY - visibleHeightPx / 2).toInt().coerceAtLeast(0)
            scrollState.scrollTo(scrollTo)
        }
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val lineThickness = 4.dp
        val lineColor = MaterialTheme.colorScheme.error
        // Stunden-Divider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    collectionInfo = CollectionInfo(
                        rowCount = events.value.size,
                        columnCount = 1,
                    )
                }
        ) {
            for (hour in 0..23) {
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
        fun assignColumns(events: List<Event>): List<Triple<Event, Int, Int>> {
            val sorted = events.sortedBy { it.startTime }
            val active = mutableListOf<Pair<Event, Int>>() // Event, column
            val result =
                mutableListOf<Triple<Event, Int, Int>>() // Event, column, maxColumns
            for (event in sorted) {
                // Entferne abgelaufene Events
                active.removeAll { it.first.endTime <= event.startTime }
                // Finde freie Spalte
                val usedColumns = active.map { it.second }.toSet()
                val col =
                    (0..active.size).firstOrNull { it !in usedColumns } ?: active.size
                active.add(event to col)
                // maxColumns = aktuelle Anzahl aktiver Events
                val maxColumns = active.size
                result.add(Triple(event, col, maxColumns))
            }
            return result
        }

        val eventColumns = assignColumns(events.value)
        eventColumns.forEach { (event, col, maxColumns) ->
            val startMinutes = ((event.startTime - dayStart) / 60000f)
            val endMinutes = ((event.endTime - dayStart) / 60000f)
            val offsetY = ((startMinutes + 30f) / 60f) * hourHeightPx
            val eventHeightPx = ((endMinutes - startMinutes) / 60f) * hourHeightPx
            val columnWidthPx =
                (maxWidthPx - with(density) { timeColumnWidth.toPx() }) / maxColumns
            val offsetX =
                (col * columnWidthPx).toInt() + with(density) { timeColumnWidth.toPx() }.toInt()
            val minCardHeightDp = 60.dp
            val isCompact = with(density) { eventHeightPx.toDp() } < minCardHeightDp
            EventCard(
                event = event,
                isCompact = isCompact,
                onClick = { navController.navigate("eventDetails/${event.eventId}") },
                modifier = Modifier
                    .width(with(density) { columnWidthPx.toDp() })
                    .defaultMinSize(minHeight = 24.dp)
                    .padding(end = 8.dp)
                    .offset { IntOffset(offsetX, offsetY.toInt()) }
                    .height(with(density) { eventHeightPx.toDp() })
            )
        }
        // "Jetzt"-Linie und Label, nur wenn im Tagesbereich
        if (nowOffsetY >= 0f && nowOffsetY <= hourHeightPx * 24) {
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
                            .atZone(ZoneId.systemDefault()).toLocalDateTime().format(hourFormat),
                        color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lineThickness)
                        .padding(end = 16.dp)
                ) {
                    drawLine(
                        color = lineColor,
                        start = Offset(
                            0f,
                            size.height / 2
                        ),
                        end = androidx.compose.ui.geometry.Offset(
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
}

@Composable
fun CalendarTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabTitles = listOf(
        stringResource(R.string.tab_month),
        stringResource(R.string.tab_week),
        stringResource(R.string.tab_day)
    )
    TabRow(
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
    index: Int = 0
) {
    val appLocale = getCurrentAppLocale(LocalContext.current)
    val hourFormat = DateTimeFormatter.ofPattern("HH:mm", appLocale)
    val startDate = remember(event.startTime) {
        Date(event.startTime)
    }
    val endDate = remember(event.endTime) {
        Date(event.endTime)
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
        )
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
                Text(
                    "${
                        startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            .format(hourFormat)
                    } - ${
                        endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            .format(hourFormat)
                    }",
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
    calendarViewModel: CalendarViewModel
) {
    val events = remember(calendarViewModel) {
        calendarViewModel.events
    }.collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events.value) { event ->
            Text(
                event.title, modifier = Modifier
                    .padding(8.dp)
                    .clickable { navController.navigate("eventDetails/${event.eventId}") })
        }
    }
}
