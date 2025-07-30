package space.midnightthoughts.nordiccalendar.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.components.DateRangeHeader
import space.midnightthoughts.nordiccalendar.viewmodels.CalendarViewModel

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
            } else if (selectedTab.value == 0) {
                MonthView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    navController,
                    calendarViewModel,
                )
            } else if (selectedTab.value == 1) {
                WeekView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    navController,
                    calendarViewModel,
                )
            } else if (selectedTab.value == 2) {
                DayView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    navController,
                    calendarViewModel,
                )
            }
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