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

/**
 * CalendarScreen is the main composable for displaying the calendar view.
 * It provides a tabbed interface for switching between month, week, and day views,
 * supports pull-to-refresh, and displays the current date range and events.
 *
 * @param modifier Modifier for styling and layout.
 * @param navController NavController for navigation actions.
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
) {
    /**
     * ViewModel coordinator for calendar data and state.
     */
    val calendarViewModel: CalendarViewModel = hiltViewModel()

    /**
     * State holding the currently selected tab (0=month, 1=week, 2=day).
     */
    val selectedTab = remember(calendarViewModel) {
        calendarViewModel.selectedTab
    }.collectAsState(initial = 0)

    /**
     * Get the appropriate specialized view model based on selected tab.
     */
    val activeViewModel = when (selectedTab.value) {
        0 -> calendarViewModel.monthViewModel
        1 -> calendarViewModel.weekViewModel
        2 -> calendarViewModel.dayViewModel
        else -> calendarViewModel.monthViewModel
    }

    /**
     * State holding the list of events for the current view.
     */
    val events = remember(selectedTab.value, calendarViewModel) {
        when (selectedTab.value) {
            0 -> calendarViewModel.monthViewModel.events
            1 -> calendarViewModel.weekViewModel.events
            2 -> calendarViewModel.dayViewModel.events
            else -> calendarViewModel.monthViewModel.events
        }
    }.collectAsState(initial = emptyList())

    /**
     * State indicating whether a refresh is in progress.
     */
    val isRefreshing = remember(activeViewModel) {
        activeViewModel.isRefreshing
    }.collectAsState(initial = false)

    /**
     * State for pull-to-refresh gesture.
     */
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        state = pullToRefreshState,
        modifier = modifier,
        onRefresh = {
            activeViewModel.refreshEvents()
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
                calendarViewModel = activeViewModel,
                onPrev = { activeViewModel.navigateToPrevious() },
                onNext = { activeViewModel.navigateToNext() },
                onToday = { activeViewModel.navigateToToday() }
            )
            if (events.value.isEmpty()) {
                Text(stringResource(R.string.no_events_found), modifier = Modifier.padding(16.dp))
            } else when (selectedTab.value) {
                0 -> MonthView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    navController,
                    calendarViewModel.monthViewModel,
                )

                1 -> WeekView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    navController,
                    calendarViewModel.weekViewModel,
                )

                2 -> DayView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    navController,
                    calendarViewModel.dayViewModel,
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