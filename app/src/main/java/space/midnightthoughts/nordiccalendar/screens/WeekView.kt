package space.midnightthoughts.nordiccalendar.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import space.midnightthoughts.nordiccalendar.viewmodels.WeekViewModel

/**
 * WeekView displays a list of events for the current week.
 * Each event is shown as a clickable text item that navigates to the event details view.
 *
 * @param modifier Modifier for styling and layout.
 * @param navController NavController for navigation actions.
 * @param weekViewModel Specialized ViewModel for week view operations.
 */
@Composable
fun WeekView(
    modifier: Modifier = Modifier,
    navController: NavController,
    weekViewModel: WeekViewModel,
) {
    // TODO: Implement WeekView
    val events = remember(weekViewModel) {
        weekViewModel.events
    }.collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(events.value) { event ->
            Text(
                event.title, modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        navController.navigate("eventDetails/${event.eventId}?tab=1") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    })
        }
    }

}