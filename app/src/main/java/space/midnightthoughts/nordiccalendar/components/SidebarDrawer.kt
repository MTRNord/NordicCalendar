package space.midnightthoughts.nordiccalendar.components

import Destinations
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch
import space.midnightthoughts.nordiccalendar.R
import space.midnightthoughts.nordiccalendar.viewmodels.SidebarDrawerViewModel

/**
 * SidebarDrawer is a composable function that displays the app's navigation drawer.
 * It provides navigation items for main destinations (calendar, settings, about) and a list of selectable calendars.
 *
 * The user can navigate between destinations and select which calendars are active.
 *
 * @param navController The NavController for navigation actions.
 * @param selectedDestination The currently selected navigation destination.
 * @param drawerState The DrawerState controlling the drawer's open/close state.
 */
@Composable
fun SidebarDrawer(
    navController: NavController,
    selectedDestination: String,
    drawerState: DrawerState
) {
    val viewModel: SidebarDrawerViewModel = hiltViewModel()

    val calendars = remember(viewModel) {
        viewModel.calendars
    }.collectAsState(initial = emptyList())
    val selectedCalendars = remember(calendars) {
        derivedStateOf {
            calendars.value.filter { it.selected }
        }
    }
    Log.d(
        "SidebarDrawer",
        "Selected Calendars: ${selectedCalendars.value}, Calendars: ${calendars.value}"
    )

    val scope = rememberCoroutineScope()

    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 32.dp, start = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.calendar)) },
                selected = selectedDestination == "calendar",
                onClick = {
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(Destinations.Calendar.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = NavigationDrawerItemDefaults.colors()
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.settings)) },
                selected = selectedDestination == "settings",
                onClick = {
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(Destinations.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = NavigationDrawerItemDefaults.colors()
            )
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.about)) },
                selected = selectedDestination == "about",
                onClick = {
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(Destinations.About.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = NavigationDrawerItemDefaults.colors()
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                stringResource(R.string.calendar_selection),
                style = MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier.padding(start = 8.dp, bottom = 8.dp, end = 8.dp, top = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
            ) {

                calendars.value.forEach { calendar ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedCalendars.value.contains(calendar),
                            onCheckedChange = {
                                scope.launch {
                                    viewModel.toggleCalendar(calendar)
                                }
                            }
                        )
                        Text(calendar.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}
