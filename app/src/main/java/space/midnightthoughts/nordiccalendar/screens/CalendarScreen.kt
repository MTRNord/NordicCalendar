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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import space.midnightthoughts.nordiccalendar.util.Event
import space.midnightthoughts.nordiccalendar.viewmodels.CalendarViewModel
import space.midnightthoughts.nordiccalendar.viewmodels.DayViewModel
import space.midnightthoughts.nordiccalendar.viewmodels.MonthViewModel
import space.midnightthoughts.nordiccalendar.viewmodels.WeekViewModel

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

    // Get tab and date parameters from navigation and set them in the ViewModel
    val backStackEntry = navController.currentBackStackEntry
    val tabFromNav = backStackEntry?.arguments?.getInt("tab") ?: 0
    val dateFromNav = backStackEntry?.arguments?.getString("date")

    // Use LaunchedEffect to handle navigation parameters only once
    LaunchedEffect(backStackEntry?.destination?.route, tabFromNav, dateFromNav) {
        calendarViewModel.setTabAndDate(tabFromNav, dateFromNav)
    }

    /**
     * State holding the currently selected tab (0=month, 1=week, 2=day).
     */
    val selectedTab by calendarViewModel.selectedTab.collectAsState()

    /**
     * Get the appropriate specialized view model based on selected tab.
     */
    val activeViewModel = remember(selectedTab) {
        when (selectedTab) {
            0 -> calendarViewModel.monthViewModel
            1 -> calendarViewModel.weekViewModel
            2 -> calendarViewModel.dayViewModel
            else -> calendarViewModel.monthViewModel
        }
    }

    /**
     * Collect events state from the main CalendarViewModel
     */
    val eventsList by calendarViewModel.events.collectAsState()

    /**
     * State indicating whether a refresh is in progress.
     */
    val isRefreshing by calendarViewModel.isRefreshing.collectAsState()

    /**
     * State for pull-to-refresh gesture.
     */
    val pullToRefreshState = rememberPullToRefreshState()

    // Memoized callbacks to prevent recomposition
    val onRefresh = remember(calendarViewModel) {
        { calendarViewModel.refreshEvents() }
    }

    val onTabSelected = remember(calendarViewModel) {
        { tab: Int -> calendarViewModel.setTab(tab) }
    }

    val onPrevious = remember(activeViewModel) {
        { activeViewModel.navigateToPrevious() }
    }

    val onNext = remember(activeViewModel) {
        { activeViewModel.navigateToNext() }
    }

    val onToday = remember(activeViewModel) {
        { activeViewModel.navigateToToday() }
    }

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
            CalendarTabBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected
            )
            DateRangeHeader(
                selectedTab = selectedTab,
                calendarViewModel = activeViewModel,
                onPrev = onPrevious,
                onNext = onNext,
                onToday = onToday
            )

            CalendarContent(
                selectedTab = selectedTab,
                eventsList = eventsList,
                navController = navController,
                monthViewModel = calendarViewModel.monthViewModel,
                weekViewModel = calendarViewModel.weekViewModel,
                dayViewModel = calendarViewModel.dayViewModel
            )
        }
    }
}

@Composable
private fun CalendarContent(
    selectedTab: Int,
    eventsList: List<Event>,
    navController: NavController,
    monthViewModel: MonthViewModel,
    weekViewModel: WeekViewModel,
    dayViewModel: DayViewModel
) {
    if (eventsList.isEmpty()) {
        Text(
            text = stringResource(R.string.no_events_found),
            modifier = Modifier.padding(16.dp)
        )
    } else {
        when (selectedTab) {
            0 -> MonthView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                navController = navController,
                monthViewModel = monthViewModel,
                events = eventsList
            )

            1 -> WeekView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                navController = navController,
                weekViewModel = weekViewModel,
                events = eventsList
            )

            2 -> DayView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                navController = navController,
                dayViewModel = dayViewModel,
                events = eventsList
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