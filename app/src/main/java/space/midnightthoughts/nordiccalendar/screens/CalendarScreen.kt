package space.midnightthoughts.nordiccalendar.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.util.Event
import space.midnightthoughts.nordiccalendar.viewmodels.CalendarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    events: List<Event>,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean,
    onRefresh: (() -> Unit),
    navController: NavController,
    viewModel: CalendarViewModel, // <-- ViewModel als Parameter
    startMillis: Long,
    endMillis: Long
) {
    Log.d(
        "CalendarScreen",
        "Rendering with selectedTab: $selectedTab, events count: ${events.size}, isRefreshing: $isRefreshing"
    )
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullToRefreshState,
        modifier = modifier,
        onRefresh = onRefresh,
        isRefreshing = isRefreshing,
        indicator = {
            Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
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
                startMillis = startMillis,
                endMillis = endMillis,
                onPrev = {
                    when (selectedTab) {
                        0 -> viewModel.prevMonth()
                        1 -> viewModel.prevWeek()
                        2 -> viewModel.prevDay()
                    }
                },
                onNext = {
                    when (selectedTab) {
                        0 -> viewModel.nextMonth()
                        1 -> viewModel.nextWeek()
                        2 -> viewModel.nextDay()
                    }
                },
                onToday = {
                    when (selectedTab) {
                        0 -> viewModel.setTodayMonth()
                        1 -> viewModel.setTodayWeek()
                        2 -> viewModel.setTodayDay()
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            if (events.isEmpty()) {
                Text("Keine Events gefunden.", modifier = Modifier.padding(16.dp))
            } else if (selectedTab == 2) {
                val hourHeightDp = 64.dp
                val timeColumnWidth = 64.dp
                val density = LocalDensity.current
                val hourHeightPx = with(density) { hourHeightDp.toPx() }
                val dayStart = events.minOfOrNull { it.startTime }?.let {
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
                val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val scrollState = rememberScrollState()
                val coroutineScope = rememberCoroutineScope()
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                // Aktualisiere die "Jetzt"-Linie jede Sekunde
                LaunchedEffect(selectedTab) {
                    while (selectedTab == 2) {
                        now = System.currentTimeMillis()
                        delay(1000)
                    }
                }
                val nowMinutes = ((now - dayStart) / 60000f)
                val nowOffsetY = ((nowMinutes + 30f) / 60f) * hourHeightPx
                LaunchedEffect(selectedTab) {
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
                    Column(modifier = Modifier.fillMaxWidth()) {
                        for (hour in 0..23) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(hourHeightDp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    hourFormat.format(Date(dayStart + hour * 60 * 60 * 1000)),
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

                    val eventColumns = assignColumns(events)
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
                            hourFormat = hourFormat,
                            onClick = { navController.navigate("eventDetails/${event.id}") },
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
                            Text(
                                "Jetzt",
                                color = lineColor,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(start = 16.dp, end = 24.dp)
                            )
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(lineThickness)
                                    .padding(end = 16.dp)
                            ) {
                                drawLine(
                                    color = lineColor,
                                    start = androidx.compose.ui.geometry.Offset(
                                        0f,
                                        size.height / 2
                                    ),
                                    end = androidx.compose.ui.geometry.Offset(
                                        size.width,
                                        size.height / 2
                                    ),
                                    strokeWidth = size.height,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            } else {
                EventList(events = events)
            }
        }
    }
}

@Composable
fun CalendarTabBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val tabTitles = listOf("Monat", "Woche", "Tag")
    TabRow(selectedTabIndex = selectedTab) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = { Text(title) }
            )
        }
    }
}

@Composable
fun DateRangeHeader(
    selectedTab: Int,
    startMillis: Long,
    endMillis: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val dateFormat = when (selectedTab) {
        0, 1 -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        2 -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    }
    val rangeText = when (selectedTab) {
        0, 1 -> dateFormat.format(Date(startMillis)) + " – " + dateFormat.format(Date(endMillis))
        2 -> dateFormat.format(Date(startMillis))
        else -> ""
    }
    val isToday = when (selectedTab) {
        0 -> {
            val cal = Calendar.getInstance()
            val todayMonth = cal.get(Calendar.MONTH)
            val todayYear = cal.get(Calendar.YEAR)
            val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
            val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }
            startCal.get(Calendar.MONTH) == todayMonth &&
                    startCal.get(Calendar.YEAR) == todayYear &&
                    endCal.get(Calendar.MONTH) == todayMonth &&
                    endCal.get(Calendar.YEAR) == todayYear
        }

        1 -> {
            val cal = Calendar.getInstance()
            val today = cal.timeInMillis
            today >= startMillis && today <= endMillis
        }

        2 -> {
            val cal = Calendar.getInstance()
            val today = cal.timeInMillis
            val startDay = Calendar.getInstance().apply { timeInMillis = startMillis }
            val endDay = Calendar.getInstance().apply { timeInMillis = endMillis }
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
        IconButton(onClick = onPrev) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Vorheriger Zeitraum"
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(rangeText, style = MaterialTheme.typography.titleMedium)
            if (!isToday) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onToday) {
                    Text("Heute")
                }
            }
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Nächster Zeitraum"
            )
        }
    }
}

@Composable
fun EventCard(
    event: Event,
    isCompact: Boolean,
    hourFormat: SimpleDateFormat,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .defaultMinSize(minHeight = 24.dp)
            .padding(end = 8.dp)
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
                style = if (isCompact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge
            )
            if (!isCompact) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    hourFormat.format(Date(event.startTime)) + " - " + hourFormat.format(Date(event.endTime)),
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
        title = "Beispiel Event",
        description = "Dies ist eine Beschreibung des Beispiel-Events.",
        startTime = System.currentTimeMillis(),
        endTime = System.currentTimeMillis() + 3600000, // 1 Stunde später
        calendarId = 1L,
        location = "Beispielort",
        allDay = false,
        organizer = "Beispielorganisator"
    )
    EventCard(
        event = sampleEvent,
        isCompact = false,
        hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    )
}

@Composable
fun EventList(events: List<Event>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events) { event ->
            Text(event.title, modifier = Modifier.padding(8.dp))
        }
    }
}
